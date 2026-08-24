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

import java.util.Collections;
import java.util.List;

public final class QueryValidationException extends QueryEngineException {

  private final List<ValidationError> validationErrors;

  public QueryValidationException(String message) {
    super(message);
    this.validationErrors = Collections.emptyList();
  }

  public QueryValidationException(String message, List<ValidationError> validationErrors) {
    super(message);
    this.validationErrors =
        validationErrors != null ? List.copyOf(validationErrors) : Collections.emptyList();
  }

  public List<ValidationError> getValidationErrors() {
    return validationErrors;
  }

  @Override
  public String getErrorCode() {
    return "VALIDATION_FAILED";
  }
}
