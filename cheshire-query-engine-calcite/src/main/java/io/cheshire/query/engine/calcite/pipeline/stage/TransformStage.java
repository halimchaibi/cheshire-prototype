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
import io.cheshire.query.engine.calcite.pipeline.model.RawResult;
import io.cheshire.spi.query.exception.QueryExecutionException;
import io.cheshire.spi.query.result.QueryEngineResult;

public final class TransformStage implements PipelineStage<RawResult, QueryEngineResult> {

  @Override
  public QueryEngineResult apply(final RawResult input, final StageContext context)
      throws QueryExecutionException {
    try {
      return context.resultTransformer().transform(input.resultSet());
    } catch (Exception e) {
      throw new QueryExecutionException("Error during TransformStage", e);
    }
  }
}
