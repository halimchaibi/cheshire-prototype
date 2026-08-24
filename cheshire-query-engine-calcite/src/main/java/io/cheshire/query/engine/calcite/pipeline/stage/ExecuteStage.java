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
import io.cheshire.query.engine.calcite.pipeline.model.RawResult;
import io.cheshire.spi.query.exception.QueryExecutionException;

public final class ExecuteStage implements PipelineStage<OptimizedPlan, RawResult> {

  @Override
  public RawResult apply(final OptimizedPlan input, final StageContext context)
      throws QueryExecutionException {
    try {
      return new RawResult(context.executor().execute(input.node()));
    } catch (Exception e) {
      throw new QueryExecutionException("Error during ExecuteStage", e);
    }
  }
}
