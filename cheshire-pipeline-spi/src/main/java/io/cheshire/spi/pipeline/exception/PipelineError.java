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

public sealed interface PipelineError permits ValidationError, ExecutionError, TimeoutError, CancellationError {

    ErrorCode code();

    boolean retryable();
}
