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

import io.cheshire.spi.pipeline.Canonical;
import io.cheshire.spi.pipeline.CanonicalInput;
import io.cheshire.spi.pipeline.exception.PipelineException;

import java.util.Map;

/**
 * Factory for creating PreProcessor instances. Implement this interface and register via ServiceLoader.
 */
public non-sealed interface PreProcessorFactory<I extends CanonicalInput<? extends Canonical<?>>>
        extends StepFactory<PreProcessor<I>> {

    @Override
    PreProcessor<I> create(Map<String, Object> config) throws PipelineException;

    @Override
    default StepType type() {
        return StepType.PRE_PROCESSOR;
    }
}
