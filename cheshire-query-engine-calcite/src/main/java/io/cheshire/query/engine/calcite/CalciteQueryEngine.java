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

import io.cheshire.common.utils.ObjectUtils;
import io.cheshire.query.engine.calcite.config.CacheConfig;
import io.cheshire.query.engine.calcite.config.CalciteQueryEngineConfig;
import io.cheshire.query.engine.calcite.executor.QueryExecutor;
import io.cheshire.query.engine.calcite.optimizer.QueryOptimizer;
import io.cheshire.query.engine.calcite.optimizer.QueryRuntimeContext;
import io.cheshire.query.engine.calcite.optimizer.RuleSetBuilder;
import io.cheshire.query.engine.calcite.optimizer.RuleSetManager;
import io.cheshire.query.engine.calcite.pipeline.QueryPipeline;
import io.cheshire.query.engine.calcite.pipeline.StageContext;
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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;

/**
 * Session façade over the typed {@link QueryPipeline}. Session-scoped schema/config live here;
 * per-query planning and execution are delegated to pipeline stages.
 */
@Slf4j
public class CalciteQueryEngine implements QueryEngine<LogicalQuery> {

  private final CalciteQueryEngineConfig calciteConfig;
  private final QueryPipeline queryPipeline = new QueryPipeline();

  private SchemaManager schemaManager;
  private QueryExecutor executor;
  private ResultTransformer resultTransformer;
  private QueryPlanCache planCache;
  private FrameworkConfig frameworkConfig;
  private QueryOptimizer optimizer;

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
      this.planCache = new QueryPlanCache(extractCacheConfig());
      this.optimizer = QueryOptimizer.builder().withFrameworkConfig(frameworkConfig).build();

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

  private CacheConfig extractCacheConfig() {
    return CacheConfig.defaults();
  }

  @Override
  public QueryEngineResult execute(
      final LogicalQuery logicalQuery, final QueryEngineContext context)
      throws QueryEngineException {
    ensureOpen();

    final QueryEngineContext executionContext =
        Optional.ofNullable(context).orElseGet(QueryEngineContext::empty);
    final QueryRuntimeContext runtimeContext =
        QueryRuntimeContext.fromQuery(logicalQuery, executionContext).build();
    final String sql = ObjectUtils.requireObjectAs(logicalQuery.query(), String.class);

    try (StageContext stageContext = createStageContext(runtimeContext, executionContext)) {
      return queryPipeline.execute(sql, stageContext);
    } catch (QueryExecutionException e) {
      throw e;
    } catch (Exception e) {
      throw new QueryExecutionException("Unexpected engine failure", e);
    }
  }

  @Override
  public boolean validate(final LogicalQuery query) throws QueryEngineException {
    ensureOpen();
    final QueryEngineContext executionContext = QueryEngineContext.empty();
    final QueryRuntimeContext runtimeContext =
        QueryRuntimeContext.fromQuery(query, executionContext).build();
    final String sql = ObjectUtils.requireObjectAs(query.query(), String.class);

    try (StageContext stageContext = createStageContext(runtimeContext, executionContext)) {
      queryPipeline.validate(sql, stageContext);
      return true;
    } catch (Exception e) {
      log.debug("Query validation failed: {}", conciseMessage(e));
      return false;
    }
  }

  @Override
  public String explain(final LogicalQuery query) throws QueryEngineException {
    ensureOpen();
    final QueryEngineContext executionContext = QueryEngineContext.empty();
    final QueryRuntimeContext runtimeContext =
        QueryRuntimeContext.fromQuery(query, executionContext).build();
    final String sql = ObjectUtils.requireObjectAs(query.query(), String.class);

    try (StageContext stageContext = createStageContext(runtimeContext, executionContext)) {
      return queryPipeline.explain(sql, stageContext);
    } catch (QueryExecutionException e) {
      throw e;
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

  private StageContext createStageContext(
      final QueryRuntimeContext runtimeContext, final QueryEngineContext executionContext) {
    final List<String> sourceNames = extractSourceNames(executionContext);
    final RuleSetManager ruleSet =
        RuleSetBuilder.forSources(sourceNames).withSchemaManager(schemaManager).build();

    final FrameworkConfig queryConfig =
        FrameworkInitializer.builder()
            .withSchemaManager(schemaManager)
            .buildQueryConfig(this.frameworkConfig, runtimeContext, ruleSet);

    final Planner planner = new CalcitePlanner(queryConfig);

    return new StageContext(
        planner,
        schemaManager,
        runtimeContext,
        executionContext,
        ruleSet,
        sourceNames,
        executor,
        resultTransformer,
        optimizer);
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

  private String conciseMessage(final Throwable e) {
    return Optional.ofNullable(e.getMessage())
        .filter(message -> !message.isBlank())
        .flatMap(message -> message.lines().findFirst())
        .orElse(e.getClass().getSimpleName());
  }
}
