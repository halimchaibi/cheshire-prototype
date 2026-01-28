/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.optimizer;

import io.cheshire.query.engine.calcite.schema.SchemaManager;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.rules.CoreRules;

/**
 * Builder for creating query-scoped rule sets based on source capabilities.
 *
 * <p>This builder analyzes the sources involved in a query and builds an appropriate rule set that
 * matches their pushdown capabilities. This allows for optimal query optimization per query rather
 * than using a one-size-fits-all approach.
 *
 * <p>The builder uses SchemaManager to get source configurations, which already contain the type
 * information needed to determine capabilities.
 */
@Slf4j
public class RuleSetBuilder {
  private final List<String> sourceNames;
  private SchemaManager schemaManager;
  private final RuleSetManager.Builder ruleSetBuilder;

  private RuleSetBuilder(List<String> sourceNames) {
    this.sourceNames = Objects.requireNonNull(sourceNames, "Source names cannot be null");
    this.ruleSetBuilder = RuleSetManager.builder();
  }

  /**
   * Creates a new RuleSetBuilder for the given source names.
   *
   * @param sourceNames the list of source names involved in the query
   * @return a new RuleSetBuilder instance
   */
  public static RuleSetBuilder forSources(List<String> sourceNames) {
    return new RuleSetBuilder(sourceNames);
  }

  /**
   * Provides SchemaManager for accessing source configurations.
   *
   * @param schemaManager the schema manager containing registered source configs
   * @return this builder for chaining
   */
  public RuleSetBuilder withSchemaManager(SchemaManager schemaManager) {
    this.schemaManager = schemaManager;
    return this;
  }

  /**
   * Builds a CustomRuleSet based on source capabilities and query requirements.
   *
   * <p>The builder analyzes the sources and adds rules that match their capabilities: - Filter
   * pushdown rules if sources support filter pushdown - Projection pushdown rules if sources
   * support projection pushdown - Join pushdown rules if sources support join pushdown - Always
   * includes safe, universal rules (filter merge, project merge, etc.)
   *
   * @return a CustomRuleSet optimized for the given sources
   */
  public RuleSetManager build() {
    log.debug("Building rule set for sources: {}", sourceNames);

    // Always include safe, universal rules
    addUniversalRules();

    // Analyze source capabilities and add appropriate rules
    if (schemaManager != null) {
      analyzeSourceCapabilities();
    } else {
      // If no schema manager provided, use conservative default rules
      log.warn("No schema manager provided, using conservative default rules");
      addConservativeRules();
    }

    RuleSetManager ruleSet = ruleSetBuilder.build();
    log.debug("Built rule set with {} rules", ruleSet.size());
    return ruleSet;
  }

  /** Adds universal rules that are safe for all sources. */
  private void addUniversalRules() {
    ruleSetBuilder
        // Filter optimization (safe, no pushdown required)
        .addRule(CoreRules.FILTER_MERGE)
        // Projection optimization (safe, no pushdown required)
        .addRule(CoreRules.PROJECT_MERGE)
        // Aggregate optimization (safe)
        .addRule(CoreRules.AGGREGATE_PROJECT_MERGE)
        // Expression reduction (safe)
        .addRule(CoreRules.CALC_REDUCE_EXPRESSIONS)
        .addRule(CoreRules.FILTER_REDUCE_EXPRESSIONS)
        .addRule(CoreRules.PROJECT_REDUCE_EXPRESSIONS);
  }

  /** Adds conservative rules when source capabilities are unknown. */
  private void addConservativeRules() {
    // Only add rules that don't require pushdown
    // These are safe for any source type
    ruleSetBuilder
        .addRule(CoreRules.FILTER_MERGE)
        .addRule(CoreRules.PROJECT_MERGE)
        .addRule(CoreRules.AGGREGATE_PROJECT_MERGE);
  }

  /**
   * Analyzes source capabilities and adds appropriate pushdown rules. Uses SchemaManager's source
   * configurations to determine source types.
   */
  private void analyzeSourceCapabilities() {
    boolean supportsFilterPushdown = false;
    boolean supportsProjectionPushdown = false;
    boolean supportsJoinPushdown = false;
    boolean supportsAggregationPushdown = false;

    // Get source configurations from SchemaManager
    // SchemaManager stores sources as Map<String, Object> where Object is the source config
    // We need to access the internal sources map - for now we'll use reflection or add a getter

    // Check capabilities of all sources
    for (String sourceName : sourceNames) {
      String sourceType = inferSourceTypeFromSchemaManager(sourceName);

      if (sourceType == null || "UNKNOWN".equals(sourceType)) {
        log.debug("Could not determine type for source: {}, using conservative rules", sourceName);
        continue;
      }

      // JDBC sources typically support all pushdowns
      if ("JDBC".equalsIgnoreCase(sourceType)) {
        supportsFilterPushdown = true;
        supportsProjectionPushdown = true;
        supportsJoinPushdown = true;
        supportsAggregationPushdown = true;
      }
      // CSV sources typically support filter and projection only
      else if ("CSV".equalsIgnoreCase(sourceType)) {
        supportsFilterPushdown = true;
        supportsProjectionPushdown = true;
      }
      // REST API sources typically support filter only (via query params)
      else if ("REST".equalsIgnoreCase(sourceType) || "API".equalsIgnoreCase(sourceType)) {
        supportsFilterPushdown = true;
      }
      // ElasticSearch supports filter and projection
      else if ("ELASTICSEARCH".equalsIgnoreCase(sourceType) || "ES".equalsIgnoreCase(sourceType)) {
        supportsFilterPushdown = true;
        supportsProjectionPushdown = true;
      }
      // Spark and Trino support all pushdowns
      else if ("SPARK".equalsIgnoreCase(sourceType) || "TRINO".equalsIgnoreCase(sourceType)) {
        supportsFilterPushdown = true;
        supportsProjectionPushdown = true;
        supportsJoinPushdown = true;
        supportsAggregationPushdown = true;
      }
    }

    // Add rules based on capabilities
    if (supportsFilterPushdown) {
      addFilterPushdownRules();
    }

    if (supportsProjectionPushdown) {
      addProjectionPushdownRules();
    }

    if (supportsJoinPushdown) {
      addJoinPushdownRules();
    }

    if (supportsAggregationPushdown) {
      addAggregationPushdownRules();
    }
  }

  /**
   * Infers source type from SchemaManager's source configurations.
   *
   * @param sourceName the name of the source
   * @return the source type (JDBC, CSV, REST, etc.) or "UNKNOWN" if not found
   */
  private String inferSourceTypeFromSchemaManager(String sourceName) {
    try {
      // First, try to get type from source config
      Map<String, Object> sourceConfig = schemaManager.getSourceConfig(sourceName);
      if (sourceConfig != null) {
        // The config might be nested, check for "type" field
        Object type = sourceConfig.get("type");
        if (type != null) {
          return type.toString().toUpperCase();
        }

        // If type is not at top level, check nested config
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedConfig = (Map<String, Object>) sourceConfig.get("config");
        if (nestedConfig != null) {
          Object nestedType = nestedConfig.get("type");
          if (nestedType != null) {
            return nestedType.toString().toUpperCase();
          }
        }
      }

      // Fallback: Try to infer from schema class name in rootSchema
      if (schemaManager.rootSchema() != null) {
        var schema = schemaManager.rootSchema().getSubSchema(sourceName);
        if (schema != null) {
          String schemaClassName = schema.getClass().getSimpleName().toUpperCase();
          if (schemaClassName.contains("JDBC")) {
            return "JDBC";
          } else if (schemaClassName.contains("CSV")) {
            return "CSV";
          } else if (schemaClassName.contains("REST") || schemaClassName.contains("API")) {
            return "REST";
          } else if (schemaClassName.contains("ELASTIC") || schemaClassName.contains("ES")) {
            return "ELASTICSEARCH";
          } else if (schemaClassName.contains("SPARK")) {
            return "SPARK";
          } else if (schemaClassName.contains("TRINO")) {
            return "TRINO";
          }
        }
      }

    } catch (Exception e) {
      log.debug("Could not infer source type from schema manager for source: {}", sourceName, e);
    }

    return "UNKNOWN";
  }

  /** Adds filter pushdown rules. */
  private void addFilterPushdownRules() {
    log.debug("Adding filter pushdown rules");
    ruleSetBuilder
        // Push filters before aggregates
        .addRule(CoreRules.FILTER_AGGREGATE_TRANSPOSE)
        // Push filters before projections
        .addRule(CoreRules.FILTER_PROJECT_TRANSPOSE);
    // Note: FILTER_INTO_JOIN is excluded to avoid infinite loops
    // when combined with other join rules
  }

  /** Adds projection pushdown rules. */
  private void addProjectionPushdownRules() {
    log.debug("Adding projection pushdown rules");
    ruleSetBuilder
        // Remove unnecessary projections
        .addRule(CoreRules.PROJECT_REMOVE);
    // Note: PROJECT_JOIN_TRANSPOSE and PROJECT_FILTER_TRANSPOSE are excluded
    // to avoid conflicts with filter rules
  }

  /** Adds join pushdown rules. */
  private void addJoinPushdownRules() {
    log.debug("Adding join pushdown rules");
    ruleSetBuilder
        // Push join conditions down
        .addRule(CoreRules.JOIN_CONDITION_PUSH)
        // Extract filters from joins
        .addRule(CoreRules.JOIN_EXTRACT_FILTER);
    // Note: JOIN_COMMUTE and JOIN_ASSOCIATE are excluded to avoid infinite loops
  }

  /** Adds aggregation pushdown rules. */
  private void addAggregationPushdownRules() {
    log.debug("Adding aggregation pushdown rules");
    ruleSetBuilder
        // Remove unnecessary aggregates
        .addRule(CoreRules.AGGREGATE_REMOVE);
  }
}
