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
import io.cheshire.query.engine.calcite.pipeline.model.ParsedQuery;
import io.cheshire.query.engine.calcite.pipeline.model.SqlInput;
import io.cheshire.spi.query.exception.QueryExecutionException;

public final class ParseStage implements PipelineStage<SqlInput, ParsedQuery> {

  @Override
  public ParsedQuery apply(final SqlInput input, final StageContext context)
      throws QueryExecutionException {
    try {
      return new ParsedQuery(context.planner().parse(input.sql()));
    } catch (Exception e) {
      throw new QueryExecutionException("Error during ParseStage", e);
    }
  }
}
