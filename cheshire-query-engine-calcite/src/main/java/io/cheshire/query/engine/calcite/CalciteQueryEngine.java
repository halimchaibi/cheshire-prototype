/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite;

import io.cheshire.common.utils.LambdaUtils;
import io.cheshire.common.utils.ObjectUtils;
import io.cheshire.query.engine.calcite.config.CacheConfig;
import io.cheshire.query.engine.calcite.config.CalciteQueryEngineConfig;
import io.cheshire.query.engine.calcite.converter.Converter;
import io.cheshire.query.engine.calcite.executor.QueryExecutor;
import io.cheshire.query.engine.calcite.optimizer.QueryOptimizer;
import io.cheshire.query.engine.calcite.optimizer.RuleSetManager;
import io.cheshire.query.engine.calcite.optimizer.claude.ClaudeRuleSetBuilder;
import io.cheshire.query.engine.calcite.parser.QueryParser;
import io.cheshire.query.engine.calcite.query.QueryPlanCache;
import io.cheshire.query.engine.calcite.schema.SchemaManager;
import io.cheshire.query.engine.calcite.transformer.ResultTransformer;
import io.cheshire.query.engine.calcite.validator.QueryValidator;
import io.cheshire.spi.query.engine.QueryEngine;
import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.exception.QueryEngineInitializationException;
import io.cheshire.spi.query.exception.QueryExecutionException;
import io.cheshire.spi.query.request.LogicalQuery;
import io.cheshire.spi.query.request.QueryEngineContext;
import io.cheshire.spi.query.result.QueryEngineResult;
import io.cheshire.spi.source.SourceProvider;
import java.sql.ResultSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.type.SqlTypeFactoryImpl;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;

@Slf4j
public class CalciteQueryEngine implements QueryEngine<LogicalQuery> {

  private final CalciteQueryEngineConfig calciteConfig;
  private SchemaManager schemaManager;
  private QueryParser parser;
  private QueryValidator validator;
  private Converter converter;
  private QueryOptimizer optimizer;
  private QueryExecutor executor;
  private ResultTransformer resultTransformer;
  private QueryPlanCache planCache;
  private FrameworkConfig frameworkConfig;

  // TODO: This should be enough to start with, advanced use cases might require a custom
  // RelDataTypeSystem
  private SqlTypeFactoryImpl typeFactory = new SqlTypeFactoryImpl(RelDataTypeSystem.DEFAULT);

  private boolean isOpen = false;

  public CalciteQueryEngine(CalciteQueryEngineConfig config) {
    this.calciteConfig = config;
  }

  @Override
  public void open() throws QueryEngineException {
    try {
      if (isOpen) {
        log.warn("Query engine in open state, already initialized and opened");
        return;
      }
      initialize();

      this.parser = new QueryParser(frameworkConfig.getParserConfig());
      this.validator = new QueryValidator(frameworkConfig.getDefaultSchema(), typeFactory);
      this.converter = new Converter(frameworkConfig);
      // Initialize optimizer with default rules (no FrameworkConfig parameter)
      this.optimizer = new QueryOptimizer(frameworkConfig);
      this.executor = new QueryExecutor(frameworkConfig);
      this.resultTransformer = new ResultTransformer();
      CacheConfig cacheConfig = extractCacheConfig();
      this.planCache = new QueryPlanCache(cacheConfig);

      this.isOpen = true;
      log.info("Calcite query engine opened successfully");

    } catch (Exception e) {
      throw new QueryEngineInitializationException("Failed to open Calcite query engine", e);
    }
  }

  private void initialize() throws QueryEngineInitializationException {

    this.schemaManager =
        SchemaManager.builder()
            .withRootSchema(Frameworks.createRootSchema(true))
            .addSources(calciteConfig.sources())
            .build();

    this.frameworkConfig = FrameworkInitializer.builder().withSchemaManager(schemaManager).build();
  }

  /** Extracts cache configuration from engine config, using defaults if not specified. */
  private CacheConfig extractCacheConfig() {
    // TODO: Extract from calciteConfig once cache config is added
    return CacheConfig.defaults();
  }

  @Override
  public QueryEngineResult execute(LogicalQuery logicalQuery, QueryEngineContext context)
      throws QueryEngineException {
    ensureOpen();
    try {
      Planner planner = Frameworks.getPlanner(frameworkConfig);

      String sql = ObjectUtils.requireObjectAs(logicalQuery.query(), String.class);
      // TODO: Using the parser provided by the FrameworkConfig
      // SqlNode parsed = stage(ExecutionStage.PARSE, () -> parser.parse(logicalQuery));
      SqlNode parsed = stage(ExecutionStage.PARSE, () -> planner.parse(sql));

      // TODO: Using the validator provided by the FrameworkConfig
      // SqlNode validated = stage(ExecutionStage.VALIDATE, () -> validator.validate(parsed));
      SqlNode validated = stage(ExecutionStage.VALIDATE, () -> planner.validate(parsed));

      RelNode logicalPlan = stage(ExecutionStage.CONVERT, () -> converter.convert(validated));

      // TODO: Requires refactoring to set boundaries
      // Build logicalQuery-scoped rule set based on sources
      // CustomRuleSet queryRules = stage(ExecutionStage.OPTIMIZE, () ->
      // buildQueryRuleSet(context));

      RelNode optimizedPlan =
          stage(ExecutionStage.OPTIMIZE, () -> optimizer.optimizeWithVolcano(logicalPlan));

      ResultSet resultSet = stage(ExecutionStage.EXECUTE, () -> executor.execute(optimizedPlan));

      return stage(ExecutionStage.TRANSFORM, () -> resultTransformer.transform(resultSet));

    } catch (QueryExecutionException e) {
      throw e;
    } catch (Exception e) {
      throw new QueryExecutionException("Unexpected engine failure", e);
    }
  }

  /**
   * Builds a query-scoped rule set based on the sources involved in the query. Uses SchemaManager
   * which already has all source configurations registered.
   *
   * @param context the query execution context
   * @return a CustomRuleSet optimized for the query's sources
   */
  private RuleSetManager buildQueryRuleSet(QueryEngineContext context) {

    List<String> sourceNames = extractSourceNames(context);

    ClaudeRuleSetBuilder builder =
        ClaudeRuleSetBuilder.forSources(sourceNames).withSchemaManager(schemaManager);

    return builder.build();
  }

  /**
   * Extracts source names from the query context.
   *
   * @param context the query execution context
   * @return list of source names
   */
  private List<String> extractSourceNames(QueryEngineContext context) {
    List<String> sourceNames = new java.util.ArrayList<>();

    if (context.sources() != null) {
      for (SourceProvider<?> provider : context.sources()) {
        try {
          String name = provider.name();
          if (name != null && !name.isBlank()) {
            sourceNames.add(name);
          }
        } catch (Exception e) {
          log.debug("Could not extract source name from provider", e);
        }
      }
    }

    if (sourceNames.isEmpty() && calciteConfig.sources() != null) {
      sourceNames.addAll(calciteConfig.sources().keySet());
    }

    return sourceNames;
  }

  @Override
  public boolean validate(LogicalQuery query) throws QueryEngineException {

    ensureOpen();

    try {
      SqlNode sqlNode = parser.parse(query);
      validator.validate(sqlNode);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public String explain(LogicalQuery query) throws QueryEngineException {

    ensureOpen();

    try {
      SqlNode sqlNode = parser.parse(query);
      SqlNode validatedNode = validator.validate(sqlNode);
      RelNode logicalPlan = converter.convert(validatedNode);
      RelNode optimizedPlan = optimizer.optimize(logicalPlan);
      return optimizer.explain(optimizedPlan);

    } catch (Exception e) {
      throw new QueryExecutionException("Explain failed", e);
    }
  }

  @Override
  public boolean supportsStreaming() {
    return true;
  }

  @Override
  public void close() {
    if (!isOpen) {
      return;
    }

    try {
      if (schemaManager != null) {
        schemaManager.close();
      }
      if (planCache != null) {
        planCache.clear();
      }
      isOpen = false;
      log.info("Calcite query engine closed successfully");
    } catch (Exception e) {
      throw new IllegalStateException("Failed to close CalciteQueryEngine", e);
    }
  }

  @Override
  public boolean isOpen() {
    return isOpen;
  }

  @Override
  public String name() {
    return calciteConfig.name();
  }

  private void ensureOpen() throws QueryEngineException {
    if (!isOpen) {
      throw new QueryEngineInitializationException("Engine not open. Call open() first.");
    }
  }

  private <T> T stage(ExecutionStage stage, LambdaUtils.CheckedSupplier<T> supplier)
      throws QueryExecutionException {

    log.debug("Starting stage: {}", stage);
    long start = System.currentTimeMillis();

    var result = LambdaUtils.Try.of(supplier);

    return switch (result) {
      case LambdaUtils.Try.Success(var value) -> {
        log.info("Stage {} finished in {}ms", stage, System.currentTimeMillis() - start);
        yield value;
      }
      case LambdaUtils.Try.Failure(var e) -> {
        log.error("Stage {} failed: {}", stage, e.getMessage());
        throw new QueryExecutionException("Error during " + stage, e);
      }
    };
  }
}
