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

import io.cheshire.query.engine.calcite.pipeline.model.LogicalPlan;
import io.cheshire.query.engine.calcite.pipeline.model.OptimizedPlan;
import io.cheshire.query.engine.calcite.pipeline.model.ParsedQuery;
import io.cheshire.query.engine.calcite.pipeline.model.PlanningContext;
import io.cheshire.query.engine.calcite.pipeline.model.RawResult;
import io.cheshire.query.engine.calcite.pipeline.model.RuleBoundPlan;
import io.cheshire.query.engine.calcite.pipeline.model.SqlInput;
import io.cheshire.query.engine.calcite.pipeline.model.ValidatedQuery;
import io.cheshire.query.engine.calcite.pipeline.stage.BuildContextStage;
import io.cheshire.query.engine.calcite.pipeline.stage.ConvertStage;
import io.cheshire.query.engine.calcite.pipeline.stage.ExecuteStage;
import io.cheshire.query.engine.calcite.pipeline.stage.OptimizeStage;
import io.cheshire.query.engine.calcite.pipeline.stage.ParseStage;
import io.cheshire.query.engine.calcite.pipeline.stage.SelectRulesStage;
import io.cheshire.query.engine.calcite.pipeline.stage.TransformStage;
import io.cheshire.query.engine.calcite.pipeline.stage.ValidateStage;
import io.cheshire.spi.query.exception.QueryExecutionException;
import io.cheshire.spi.query.result.QueryEngineResult;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptUtil;

/**
 * Orchestrates typed Calcite pipeline stages with shared cutoffs for validate / explain / execute.
 */
@Slf4j
public final class QueryPipeline {

  private final ParseStage parseStage = new ParseStage();
  private final ValidateStage validateStage = new ValidateStage();
  private final ConvertStage convertStage = new ConvertStage();
  private final BuildContextStage buildContextStage = new BuildContextStage();
  private final SelectRulesStage selectRulesStage = new SelectRulesStage();
  private final OptimizeStage optimizeStage = new OptimizeStage();
  private final ExecuteStage executeStage = new ExecuteStage();
  private final TransformStage transformStage = new TransformStage();

  public void validate(final String sql, final StageContext context)
      throws QueryExecutionException {
    Objects.requireNonNull(sql, "sql");
    Objects.requireNonNull(context, "context");
    final ParsedQuery parsed = run(parseStage, new SqlInput(sql), context);
    run(validateStage, parsed, context);
  }

  public String explain(final String sql, final StageContext context)
      throws QueryExecutionException {
    Objects.requireNonNull(sql, "sql");
    Objects.requireNonNull(context, "context");
    final OptimizedPlan optimized = optimizeThrough(sql, context);
    return RelOptUtil.toString(optimized.node());
  }

  public QueryEngineResult execute(final String sql, final StageContext context)
      throws QueryExecutionException {
    Objects.requireNonNull(sql, "sql");
    Objects.requireNonNull(context, "context");
    final OptimizedPlan optimized = optimizeThrough(sql, context);
    try (RawResult raw = run(executeStage, optimized, context)) {
      return run(transformStage, raw, context);
    } catch (QueryExecutionException e) {
      throw e;
    } catch (Exception e) {
      throw new QueryExecutionException("Error during ExecuteStage resource cleanup", e);
    }
  }

  public OptimizedPlan optimizeThrough(final String sql, final StageContext context)
      throws QueryExecutionException {
    final ParsedQuery parsed = run(parseStage, new SqlInput(sql), context);
    final ValidatedQuery validated = run(validateStage, parsed, context);
    final LogicalPlan logical = run(convertStage, validated, context);
    final PlanningContext planning = run(buildContextStage, logical, context);
    final RuleBoundPlan ruleBound = run(selectRulesStage, planning, context);
    return run(optimizeStage, ruleBound, context);
  }

  private <I, O> O run(final PipelineStage<I, O> stage, final I input, final StageContext context)
      throws QueryExecutionException {
    final String stageName = stage.name();
    log.debug("Starting stage: {}", stageName);
    final long start = System.currentTimeMillis();
    try {
      final O output = stage.apply(input, context);
      log.info("Stage {} finished in {}ms", stageName, System.currentTimeMillis() - start);
      return output;
    } catch (QueryExecutionException e) {
      log.error("Stage {} failed: {}", stageName, e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("Stage {} failed: {}", stageName, e.getMessage());
      throw new QueryExecutionException("Error during " + stageName, e);
    }
  }
}
