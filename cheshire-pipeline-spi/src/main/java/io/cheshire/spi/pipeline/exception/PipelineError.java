package io.cheshire.spi.pipeline.exception;

public sealed interface PipelineError
        permits ValidationError, ExecutionError, TimeoutError, CancellationError {

    ErrorCode code();

    boolean retryable();
}

