package io.cheshire.spi.pipeline.exception;

public final class TimeoutError implements PipelineError {
    public ErrorCode code() {
        return ErrorCode.TIMEOUT_ERROR;
    }

    public boolean retryable() {
        return true;
    }
}
