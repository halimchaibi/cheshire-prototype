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
import io.cheshire.query.engine.calcite.pipeline.model.PlanningContext;
import io.cheshire.query.engine.calcite.pipeline.model.RuleBoundPlan;
import io.cheshire.spi.query.exception.QueryExecutionException;

public final class SelectRulesStage implements PipelineStage<PlanningContext, RuleBoundPlan> {

  @Override
  public RuleBoundPlan apply(final PlanningContext input, final StageContext context)
      throws QueryExecutionException {
    try {
      return new RuleBoundPlan(input.node(), context.ruleSet());
    } catch (Exception e) {
      throw new QueryExecutionException("Error during SelectRulesStage", e);
    }
  }
}
