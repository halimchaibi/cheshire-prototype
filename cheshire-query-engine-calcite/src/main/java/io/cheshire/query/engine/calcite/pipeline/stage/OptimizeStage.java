/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.pipeline.stage;

import io.cheshire.query.engine.calcite.pipeline.PipelineStage;
import io.cheshire.query.engine.calcite.pipeline.StageContext;
import io.cheshire.query.engine.calcite.pipeline.model.OptimizedPlan;
import io.cheshire.query.engine.calcite.pipeline.model.RuleBoundPlan;
import io.cheshire.spi.query.exception.QueryExecutionException;
import org.apache.calcite.rel.RelNode;

public final class OptimizeStage implements PipelineStage<RuleBoundPlan, OptimizedPlan> {

  @Override
  public OptimizedPlan apply(final RuleBoundPlan input, final StageContext context)
      throws QueryExecutionException {
    try {
      final RelNode optimized =
          context.optimizer().optimize(input.node(), input.ruleSet(), context.runtime());
      return new OptimizedPlan(optimized);
    } catch (Exception e) {
      throw new QueryExecutionException("Error during OptimizeStage", e);
    }
  }
}
