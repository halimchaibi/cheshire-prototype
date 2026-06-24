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
import io.cheshire.query.engine.calcite.executor.QueryExecutor;
import io.cheshire.query.engine.calcite.optimizer.QueryRuntimeContext;
import io.cheshire.query.engine.calcite.optimizer.RuleSetBuilder;
import io.cheshire.query.engine.calcite.optimizer.RuleSetManager;
import io.cheshire.query.engine.calcite.query.QueryPlanCache;
import io.cheshire.query.engine.calcite.schema.SchemaManager;
import io.cheshire.query.engine.calcite.transformer.ResultTransformer;
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
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;

@Slf4j
public class CalciteQueryEngine implements QueryEngine<LogicalQuery> {

  private final CalciteQueryEngineConfig calciteConfig;
  private SchemaManager schemaManager;
  private QueryExecutor executor;
  private ResultTransformer resultTransformer;
  private QueryPlanCache planCache;
  private FrameworkConfig frameworkConfig;

  private boolean opened = false;

  public CalciteQueryEngine(CalciteQueryEngineConfig config) {
    this.calciteConfig = config;
  }

  @Override
  public void open() throws QueryEngineException {
    try {
      if (opened) {
        log.warn("Query engine in open state, already initialized and opened");
        return;
      }
      initialize();
      this.executor = new QueryExecutor(frameworkConfig, schemaManager.schemas());
      this.resultTransformer = new ResultTransformer();
      CacheConfig cacheConfig = extractCacheConfig();
      this.planCache = new QueryPlanCache(cacheConfig);

      this.opened = true;
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

    this.frameworkConfig =
        FrameworkInitializer.builder().withSchemaManager(schemaManager).buildBaseConfig();
  }

  /** Extracts cache configuration from engine config, using defaults for the current MVP. */
  private CacheConfig extractCacheConfig() {
    return CacheConfig.defaults();
  }

  @Override
  public QueryEngineResult execute(LogicalQuery logicalQuery, QueryEngineContext context)
      throws QueryEngineException {
    ensureOpen();

    QueryRuntimeContext runtimeContext =
        QueryRuntimeContext.fromQuery(logicalQuery, context).build();

    try {
      PlannedQuery plannedQuery = plan(logicalQuery, runtimeContext, context);

      RelNode optimizedPlan = stage(ExecutionStage.OPTIMIZE, plannedQuery::relNode);

      try (ResultSet resultSet =
          stage(ExecutionStage.EXECUTE, () -> executor.execute(optimizedPlan))) {
        return stage(ExecutionStage.TRANSFORM, () -> resultTransformer.transform(resultSet));
      }

    } catch (QueryExecutionException e) {
      throw e;
    } catch (Exception e) {
      throw new QueryExecutionException("Unexpected engine failure", e);
    }
  }

  /** Builds a query-scoped rule set based on the sources involved in the query. */
  private RuleSetManager buildQueryRuleSet(QueryEngineContext context) {
    List<String> sourceNames = extractSourceNames(context);

    return RuleSetBuilder.forSources(sourceNames).withSchemaManager(schemaManager).build();
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
      String sql = ObjectUtils.requireObjectAs(query.query(), String.class);
      Planner planner = new CalcitePlanner(frameworkConfig);
      try {
        SqlNode parsed = planner.parse(sql);
        planner.validate(parsed);
      } finally {
        planner.close();
      }
      return true;
    } catch (Exception e) {
      log.debug("Query validation failed: {}", conciseMessage(e));
      return false;
    }
  }

  @Override
  public String explain(LogicalQuery query) throws QueryEngineException {
    try {
      ensureOpen();
      QueryRuntimeContext runtimeContext =
          QueryRuntimeContext.fromQuery(query, QueryEngineContext.empty()).build();
      return RelOptUtil.toString(plan(query, runtimeContext, QueryEngineContext.empty()).relNode());
    } catch (Exception e) {
      throw new QueryExecutionException("Explain failed", e);
    }
  }

  @Override
  public boolean supportsStreaming() {
    return false;
  }

  @Override
  public void close() {
    if (!opened) {
      return;
    }

    try {
      if (schemaManager != null) {
        schemaManager.close();
      }
      if (planCache != null) {
        planCache.clear();
      }
      opened = false;
      log.info("Calcite query engine closed successfully");
    } catch (Exception e) {
      throw new IllegalStateException("Failed to close CalciteQueryEngine", e);
    }
  }

  @Override
  public boolean isOpen() {
    return opened;
  }

  @Override
  public String name() {
    return calciteConfig.name();
  }

  private void ensureOpen() throws QueryEngineException {
    if (!opened) {
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

  private String conciseMessage(Throwable e) {
    String message = e.getMessage();
    if (message == null || message.isBlank()) {
      return e.getClass().getSimpleName();
    }
    return message.lines().findFirst().orElse(message);
  }

  private PlannedQuery plan(
      LogicalQuery logicalQuery, QueryRuntimeContext runtimeContext, QueryEngineContext context)
      throws Exception {
    String sql = ObjectUtils.requireObjectAs(logicalQuery.query(), String.class);
    QueryEngineContext executionContext = context != null ? context : QueryEngineContext.empty();
    RuleSetManager ruleSet = buildQueryRuleSet(executionContext);

    FrameworkConfig queryConfig =
        FrameworkInitializer.builder()
            .withSchemaManager(schemaManager)
            .buildQueryConfig(this.frameworkConfig, runtimeContext, ruleSet);
    Planner planner = new CalcitePlanner(queryConfig);

    try {
      SqlNode parsed = stage(ExecutionStage.PARSE, () -> planner.parse(sql));
      SqlNode validated = stage(ExecutionStage.VALIDATE, () -> planner.validate(parsed));
      RelNode relNode = stage(ExecutionStage.CONVERT, () -> planner.rel(validated).rel);

      return new PlannedQuery(relNode);
    } finally {
      planner.close();
    }
  }

  private record PlannedQuery(RelNode relNode) {}
}
