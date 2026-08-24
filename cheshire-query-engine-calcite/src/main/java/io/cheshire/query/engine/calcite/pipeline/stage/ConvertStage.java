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
import io.cheshire.query.engine.calcite.pipeline.model.ValidatedQuery;
import io.cheshire.spi.query.exception.QueryExecutionException;

public final class ConvertStage implements PipelineStage<ValidatedQuery, LogicalPlan> {

  @Override
  public LogicalPlan apply(final ValidatedQuery input, final StageContext context)
      throws QueryExecutionException {
    try {
      return new LogicalPlan(context.planner().rel(input.node()).rel);
    } catch (Exception e) {
      throw new QueryExecutionException("Error during ConvertStage", e);
    }
  }
}
