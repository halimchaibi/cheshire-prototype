package io.cheshire.spi.pipeline.step;

import io.cheshire.spi.pipeline.CanonicalInput;
import io.cheshire.spi.pipeline.CanonicalOutput;
import io.cheshire.spi.pipeline.exception.PipelineException;

import java.util.Map;

/**
 * Factory for creating Executor instances.
 * Implement this interface and register via ServiceLoader.
 */
public non-sealed interface ExecutorFactory<I extends CanonicalInput<?>, O extends CanonicalOutput<?>> extends StepFactory<Executor<I, O>> {

    @Override
    Executor<I, O> create(Map<String, Object> config) throws PipelineException;

    @Override
    default StepType type() {
        return StepType.EXECUTOR;
    }
}
