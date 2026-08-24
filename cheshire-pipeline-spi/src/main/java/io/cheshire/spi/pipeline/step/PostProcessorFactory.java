/*-
 * #%L
 * Cheshire :: Pipeline :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.pipeline.step;

import io.cheshire.spi.pipeline.CanonicalOutput;
import io.cheshire.spi.pipeline.exception.PipelineException;
import java.util.Map;

/**
 * Factory for creating PostProcessor instances. Implement this interface and register via
 * ServiceLoader.
 */
public non-sealed interface PostProcessorFactory<O extends CanonicalOutput<?>>
    extends StepFactory<PostProcessor<O>> {

  @Override
  PostProcessor<O> create(Map<String, Object> config) throws PipelineException;

  @Override
  default StepType type() {
    return StepType.POST_PROCESSOR;
  }
}
