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

public final class QueryEngineInitializationException extends QueryEngineException {
  public QueryEngineInitializationException(String message) {
    super(message);
  }

  public QueryEngineInitializationException(String message, Throwable cause) {
    super(message, cause);
  }

  public QueryEngineInitializationException(
      String message, String queryEngineName, Throwable cause) {
    super(message, cause);
  }
}
