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
import io.cheshire.query.engine.calcite.pipeline.model.LogicalPlan;
import io.cheshire.query.engine.calcite.pipeline.model.PlanningContext;
import io.cheshire.spi.query.exception.QueryExecutionException;

public final class BuildContextStage implements PipelineStage<LogicalPlan, PlanningContext> {

  @Override
  public PlanningContext apply(final LogicalPlan input, final StageContext context)
      throws QueryExecutionException {
    try {
      return new PlanningContext(input.node(), context.runtime(), context.sourceNames());
    } catch (Exception e) {
      throw new QueryExecutionException("Error during BuildContextStage", e);
    }
  }
}
