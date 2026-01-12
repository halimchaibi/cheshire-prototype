package io.cheshire.spi.pipeline.exception;

public final class ExecutionError implements PipelineError {
    public ErrorCode code() {
        return ErrorCode.EXECUTION_ERROR;
    }

    public boolean retryable() {
        return true;
    }
}
