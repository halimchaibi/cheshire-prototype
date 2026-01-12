package io.cheshire.spi.pipeline.exception;

public final class ValidationError implements PipelineError {
    public ErrorCode code() {
        return ErrorCode.VALIDATION_ERROR;
    }

    public boolean retryable() {
        return false;
    }
}
