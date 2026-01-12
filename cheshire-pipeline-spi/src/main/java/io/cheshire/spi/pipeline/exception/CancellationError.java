package io.cheshire.spi.pipeline.exception;

public final class CancellationError implements PipelineError {
    public ErrorCode code() {
        return ErrorCode.CANCELLATION_ERROR;
    }

    public boolean retryable() {
        return false;
    }
}
