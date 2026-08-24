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

public abstract sealed class QueryEngineException extends Exception
    permits QueryEngineConfigurationException,
        QueryEngineInitializationException,
        QueryExecutionException,
        QueryValidationException {

  public QueryEngineException(String message) {
    super(message);
  }

  public QueryEngineException(String message, Throwable cause) {
    super(message, cause);
  }

  public String getErrorCode() {
    return "UNKNOWN";
  }

  public boolean isRetryable() {
    return false;
  }
}
