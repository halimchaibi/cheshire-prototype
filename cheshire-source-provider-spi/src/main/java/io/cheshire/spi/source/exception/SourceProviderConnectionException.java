/*-
 * #%L
 * Cheshire :: Source Provider :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.source.exception;

/**
 * Exception thrown when a connection to a data source cannot be established.
 *
 * <p>This exception indicates connection failures such as:
 *
 * <ul>
 *   <li>Network connectivity issues
 *   <li>Authentication failures
 *   <li>Host unreachable or not found
 *   <li>Connection timeout
 *   <li>Protocol errors
 * </ul>
 *
 * <h2>Error Code</h2>
 *
 * <p>{@code CONNECTION_FAILED} - This error is <b>retryable</b> as connection issues may be
 * transient.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * try {
 *     connection = DriverManager.getConnection(url, username, password);
 * } catch (SQLException e) {
 *     throw new SourceProviderConnectionException(
 *         "Failed to connect to database",
 *         sourceName,
 *         url,
 *         e);
 * }
 * }</pre>
 *
 * @since 1.0
 */
public final class SourceProviderConnectionException extends SourceProviderException {

  private final String connectionUrl;

  /**
   * Constructs a connection exception for the specified source.
   *
   * @param message the detail message
   * @param sourceName the name of the source provider
   */
  public SourceProviderConnectionException(String message, String sourceName) {
    super(message, sourceName);
    this.connectionUrl = null;
  }

  /**
   * Constructs a connection exception with the specified message, source, and cause.
   *
   * @param message the detail message
   * @param sourceName the name of the source provider
   * @param cause the underlying cause
   */
  public SourceProviderConnectionException(String message, String sourceName, Throwable cause) {
    super(message, sourceName, cause);
    this.connectionUrl = null;
  }

  /**
   * Constructs a connection exception with full context including the connection URL.
   *
   * @param message the detail message
   * @param sourceName the name of the source provider
   * @param connectionUrl the URL/endpoint that failed to connect (may contain sensitive info)
   * @param cause the underlying cause
   */
  public SourceProviderConnectionException(
      String message, String sourceName, String connectionUrl, Throwable cause) {
    super(message, sourceName, cause);
    this.connectionUrl = connectionUrl;
  }

  /**
   * Returns the connection URL that failed, if available.
   *
   * <p><b>Security Note</b>: This URL may contain sensitive information. Avoid logging or exposing
   * it to untrusted contexts.
   *
   * @return the connection URL, or {@code null} if not specified
   */
  public String getConnectionUrl() {
    return connectionUrl;
  }

  /**
   * Returns {@code "CONNECTION_FAILED"}.
   *
   * @return the error code
   */
  @Override
  public String getErrorCode() {
    return "CONNECTION_FAILED";
  }

  /**
   * Returns {@code true} as connection failures are often transient and retryable.
   *
   * @return {@code true}
   */
  @Override
  public boolean isRetryable() {
    return true;
  }
}
