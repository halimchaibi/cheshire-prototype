/*-
 * #%L
 * Cheshire :: Query Engine :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.query.exception;

public record ValidationError(String field, String message, String code) {

  public ValidationError {
    if (field == null) {
      throw new NullPointerException("Field cannot be null");
    }
    if (message == null) {
      throw new NullPointerException("Message cannot be null");
    }
    if (code == null) {
      throw new NullPointerException("Code cannot be null");
    }
  }
}
