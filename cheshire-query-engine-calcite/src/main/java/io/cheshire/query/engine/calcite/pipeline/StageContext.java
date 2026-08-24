/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.pipeline;

import io.cheshire.query.engine.calcite.executor.QueryExecutor;
import io.cheshire.query.engine.calcite.optimizer.QueryOptimizer;
import io.cheshire.query.engine.calcite.optimizer.QueryRuntimeContext;
import io.cheshire.query.engine.calcite.optimizer.RuleSetManager;
import io.cheshire.query.engine.calcite.schema.SchemaManager;
import io.cheshire.query.engine.calcite.transformer.ResultTransformer;
import io.cheshire.spi.query.request.QueryEngineContext;
import java.util.List;
import java.util.Objects;
import org.apache.calcite.tools.Planner;

/** Query-scoped dependencies shared across pipeline stages. */
public final class StageContext implements AutoCloseable {

  private final Planner planner;
  private final SchemaManager schemaManager;
  private final QueryRuntimeContext runtime;
  private final QueryEngineContext engineContext;
  private final RuleSetManager ruleSet;
  private final List<String> sourceNames;
  private final QueryExecutor executor;
  private final ResultTransformer resultTransformer;
  private final QueryOptimizer optimizer;

  public StageContext(
      final Planner planner,
      final SchemaManager schemaManager,
      final QueryRuntimeContext runtime,
      final QueryEngineContext engineContext,
      final RuleSetManager ruleSet,
      final List<String> sourceNames,
      final QueryExecutor executor,
      final ResultTransformer resultTransformer,
      final QueryOptimizer optimizer) {
    this.planner = Objects.requireNonNull(planner, "planner");
    this.schemaManager = Objects.requireNonNull(schemaManager, "schemaManager");
    this.runtime = Objects.requireNonNull(runtime, "runtime");
    this.engineContext = Objects.requireNonNull(engineContext, "engineContext");
    this.ruleSet = Objects.requireNonNull(ruleSet, "ruleSet");
    this.sourceNames = List.copyOf(Objects.requireNonNull(sourceNames, "sourceNames"));
    this.executor = Objects.requireNonNull(executor, "executor");
    this.resultTransformer = Objects.requireNonNull(resultTransformer, "resultTransformer");
    this.optimizer = Objects.requireNonNull(optimizer, "optimizer");
  }

  public Planner planner() {
    return planner;
  }

  public SchemaManager schemaManager() {
    return schemaManager;
  }

  public QueryRuntimeContext runtime() {
    return runtime;
  }

  public QueryEngineContext engineContext() {
    return engineContext;
  }

  public RuleSetManager ruleSet() {
    return ruleSet;
  }

  public List<String> sourceNames() {
    return sourceNames;
  }

  public QueryExecutor executor() {
    return executor;
  }

  public ResultTransformer resultTransformer() {
    return resultTransformer;
  }

  public QueryOptimizer optimizer() {
    return optimizer;
  }

  @Override
  public void close() {
    planner.close();
  }
}
