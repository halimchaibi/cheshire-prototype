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
 * Base sealed exception for all source provider errors.
 *
 * <p>This sealed hierarchy (Java 17+) enables exhaustive error handling through pattern matching,
 * ensuring all possible exception types are explicitly handled at compile time.
 *
 * <h2>Exception Hierarchy</h2>
 *
 * <pre>
 * SourceProviderException (sealed, abstract)
 * ├── SourceProviderConfigException
 * ├── SourceProviderConnectionException
 * ├── SourceProviderInitializationException
 * └── SourceProviderExecutionException (sealed)
 *     └── SourceProviderTimeoutException
 * </pre>
 *
 * <h2>Pattern Matching (Java 17+)</h2>
 *
 * <p>The sealed hierarchy enables exhaustive switch expressions:
 *
 * <pre>{@code
 * try {
 *     result = provider.execute(query);
 * } catch (SourceProviderException e) {
 *     switch (e) {
 *         case SourceProviderConfigException ce ->
 *             log.error("Config error: {}", ce.getMissingKey());
 *         case SourceProviderConnectionException ce ->
 *             retry(provider);  // Retryable
 *         case SourceProviderTimeoutException te ->
 *             log.warn("Timeout after {}ms", te.getElapsedTime().toMillis());
 *         case SourceProviderExecutionException ee ->
 *             log.error("Execution failed");
 *         case SourceProviderInitializationException ie ->
 *             log.fatal("Init failed", ie);
 *     }
 * }
 * }</pre>
 *
 * <h2>Error Metadata</h2>
 *
 * <p>All exceptions provide:
 *
 * <ul>
 *   <li><b>Source name</b>: Identifies which source failed
 *   <li><b>Error code</b>: Machine-readable error identifier
 *   <li><b>Retryable flag</b>: Indicates if the operation can be retried
 * </ul>
 *
 * @see SourceProviderConfigException
 * @see SourceProviderConnectionException
 * @see SourceProviderExecutionException
 * @see SourceProviderInitializationException
 * @since 1.0
 */
public abstract sealed class SourceProviderException extends Exception
    permits SourceProviderConfigException,
        SourceProviderConnectionException,
        SourceProviderExecutionException,
        SourceProviderInitializationException {

  private final String sourceName;

  /**
   * Constructs a new exception with the specified message.
   *
   * @param message the detail message
   */
  public SourceProviderException(String message) {
    super(message);
    this.sourceName = null;
  }

  /**
   * Constructs a new exception with the specified message and source name.
   *
   * @param message the detail message
   * @param sourceName the name of the source provider that failed
   */
  public SourceProviderException(String message, String sourceName) {
    super(message);
    this.sourceName = sourceName;
  }

  /**
   * Constructs a new exception with the specified message and cause.
   *
   * @param message the detail message
   * @param cause the underlying cause
   */
  public SourceProviderException(String message, Throwable cause) {
    super(message, cause);
    this.sourceName = null;
  }

  /**
   * Constructs a new exception with the specified message, source name, and cause.
   *
   * @param message the detail message
   * @param sourceName the name of the source provider that failed
   * @param cause the underlying cause
   */
  public SourceProviderException(String message, String sourceName, Throwable cause) {
    super(message, cause);
    this.sourceName = sourceName;
  }

  /**
   * Returns the name of the source provider that encountered the error.
   *
   * @return the source name, or {@code null} if not specified
   */
  public String getSourceName() {
    return sourceName;
  }

  /**
   * Returns a machine-readable error code identifying the error type.
   *
   * <p>Error codes are stable identifiers that can be used for:
   *
   * <ul>
   *   <li>Error categorization and metrics
   *   <li>User-facing error messages (via lookup tables)
   *   <li>Automated error handling and routing
   * </ul>
   *
   * @return the error code, never {@code null}
   */
  public String getErrorCode() {
    return "UNKNOWN";
  }

  /**
   * Indicates whether this error is transient and the operation can be retried.
   *
   * <p>Retryable errors typically include:
   *
   * <ul>
   *   <li>Connection timeouts
   *   <li>Temporary network failures
   *   <li>Resource unavailable (try again later)
   * </ul>
   *
   * <p>Non-retryable errors typically include:
   *
   * <ul>
   *   <li>Configuration errors
   *   <li>Authentication failures
   *   <li>Query syntax errors
   * </ul>
   *
   * @return {@code true} if the operation can be retried, {@code false} otherwise
   */
  public boolean isRetryable() {
    return false;
  }
}
