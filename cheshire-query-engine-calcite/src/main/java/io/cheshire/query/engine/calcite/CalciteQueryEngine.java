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
import io.cheshire.query.engine.calcite.query.DslQuery;
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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

  public CalciteQueryEngine(final CalciteQueryEngineConfig config) {
    this.calciteConfig = Objects.requireNonNull(config, "Calcite query engine config is required");
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
      final CacheConfig cacheConfig = extractCacheConfig();
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
  public QueryEngineResult execute(
      final LogicalQuery logicalQuery, final QueryEngineContext context)
      throws QueryEngineException {
    ensureOpen();

    final QueryRuntimeContext runtimeContext =
        QueryRuntimeContext.fromQuery(logicalQuery, context).build();

    try {
      final PlannedQuery plannedQuery = plan(logicalQuery, runtimeContext, context);

      final RelNode optimizedPlan = stage(ExecutionStage.OPTIMIZE, plannedQuery::relNode);

      try (final ResultSet resultSet =
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
  private RuleSetManager buildQueryRuleSet(final QueryEngineContext context) {
    final List<String> sourceNames = extractSourceNames(context);

    return RuleSetBuilder.forSources(sourceNames).withSchemaManager(schemaManager).build();
  }

  private List<String> extractSourceNames(final QueryEngineContext context) {
    final List<String> contextSourceNames =
        Optional.ofNullable(context).map(QueryEngineContext::sources).stream()
            .flatMap(Collection::stream)
            .map(this::sourceName)
            .flatMap(Optional::stream)
            .toList();

    return contextSourceNames.isEmpty()
        ? List.copyOf(calciteConfig.sources().keySet())
        : contextSourceNames;
  }

  private Optional<String> sourceName(final SourceProvider<?> provider) {
    try {
      return Optional.ofNullable(provider.name()).filter(name -> !name.isBlank());
    } catch (Exception e) {
      log.debug("Could not extract source name from provider", e);
      return Optional.empty();
    }
  }

  @Override
  public boolean validate(final LogicalQuery query) throws QueryEngineException {
    ensureOpen();
    try {
      final String sql = sqlFor(query);
      final Planner planner = new CalcitePlanner(frameworkConfig);
      try {
        final SqlNode parsed = planner.parse(sql);
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
  public String explain(final LogicalQuery query) throws QueryEngineException {
    try {
      ensureOpen();
      final QueryRuntimeContext runtimeContext =
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
      Optional.ofNullable(schemaManager).ifPresent(this::closeSchemaManager);
      Optional.ofNullable(planCache).ifPresent(QueryPlanCache::clear);
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

  private void closeSchemaManager(final SchemaManager schemaManager) {
    try {
      schemaManager.close();
    } catch (QueryEngineInitializationException e) {
      throw new IllegalStateException("Failed to close schema manager", e);
    }
  }

  private <T> T stage(final ExecutionStage stage, final LambdaUtils.CheckedSupplier<T> supplier)
      throws QueryExecutionException {

    log.debug("Starting stage: {}", stage);
    final long start = System.currentTimeMillis();

    final var result = LambdaUtils.Try.of(supplier);

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

  private String conciseMessage(final Throwable e) {
    return Optional.ofNullable(e.getMessage())
        .filter(message -> !message.isBlank())
        .flatMap(message -> message.lines().findFirst())
        .orElse(e.getClass().getSimpleName());
  }

  private PlannedQuery plan(
      final LogicalQuery logicalQuery,
      final QueryRuntimeContext runtimeContext,
      final QueryEngineContext context)
      throws Exception {
    final String sql = sqlFor(logicalQuery);
    final QueryEngineContext executionContext =
        Optional.ofNullable(context).orElseGet(QueryEngineContext::empty);
    final RuleSetManager ruleSet = buildQueryRuleSet(executionContext);

    final FrameworkConfig queryConfig =
        FrameworkInitializer.builder()
            .withSchemaManager(schemaManager)
            .buildQueryConfig(this.frameworkConfig, runtimeContext, ruleSet);
    final Planner planner = new CalcitePlanner(queryConfig);

    try {
      final SqlNode parsed = stage(ExecutionStage.PARSE, () -> planner.parse(sql));
      final SqlNode validated = stage(ExecutionStage.VALIDATE, () -> planner.validate(parsed));
      final RelNode relNode = stage(ExecutionStage.CONVERT, () -> planner.rel(validated).rel);

      return new PlannedQuery(relNode);
    } finally {
      planner.close();
    }
  }

  private String sqlFor(final LogicalQuery logicalQuery) {
    return switch (logicalQuery) {
      case DslQuery dslQuery -> dslQuery.toSql();
      default -> ObjectUtils.requireObjectAs(logicalQuery.query(), String.class);
    };
  }

  private record PlannedQuery(RelNode relNode) {}
}
