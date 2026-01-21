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

import java.time.Duration;
import java.time.Instant;

public final class QueryTimeoutException extends QueryExecutionException {

  private final Duration elapsedTime;
  private final Instant deadline;

  public QueryTimeoutException(String message, Duration elapsedTime, Instant deadline) {
    super(message);
    this.elapsedTime = elapsedTime;
    this.deadline = deadline;
  }

  public QueryTimeoutException(
      String message, Duration elapsedTime, Instant deadline, Throwable cause) {
    super(message, cause);
    this.elapsedTime = elapsedTime;
    this.deadline = deadline;
  }

  public Duration getElapsedTime() {
    return elapsedTime;
  }

  public Instant getDeadline() {
    return deadline;
  }

  @Override
  public String getErrorCode() {
    return "QUERY_TIMEOUT";
  }

  @Override
  public boolean isRetryable() {
    return true;
  }
}
