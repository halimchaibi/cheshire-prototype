package io.cheshire.spi.pipeline.step;

import io.cheshire.spi.pipeline.Context;
import io.cheshire.spi.pipeline.exception.PipelineException;

public sealed interface Step<I, O>
        permits PostProcessor, PreProcessor, Executor {

    O apply(I input, Context ctx)
            throws PipelineException;
}
