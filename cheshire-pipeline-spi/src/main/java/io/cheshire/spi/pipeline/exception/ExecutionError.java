/*-
 * #%L
 * Cheshire :: Pipeline :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.pipeline.exception;

public final class ExecutionError implements PipelineError {
    public ErrorCode code() {
        return ErrorCode.EXECUTION_ERROR;
    }

    public boolean retryable() {
        return true;
    }
}
