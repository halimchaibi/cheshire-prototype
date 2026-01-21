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

public class PipelineException extends Exception {

  public PipelineException(String message) {
    super(message);
  }

  public PipelineException(String message, Throwable cause) {
    super(message, cause);
  }
}
