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

import io.cheshire.spi.query.exception.QueryExecutionException;

/**
 * A single typed transformation in the Calcite query pipeline.
 *
 * @param <I> stage input carrier
 * @param <O> stage output carrier
 */
@FunctionalInterface
public interface PipelineStage<I, O> {

  O apply(I input, StageContext context) throws QueryExecutionException;

  default String name() {
    return getClass().getSimpleName();
  }
}
