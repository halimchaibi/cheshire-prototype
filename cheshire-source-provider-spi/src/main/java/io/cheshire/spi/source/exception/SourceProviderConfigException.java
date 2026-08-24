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

import io.cheshire.spi.source.SourceProviderConfig;

/**
 * Exception thrown when source provider configuration is invalid or incomplete.
 *
 * <p>This exception indicates configuration problems such as:
 *
 * <ul>
 *   <li>Missing required configuration keys
 *   <li>Invalid configuration values (format, range, type)
 *   <li>Incompatible configuration combinations
 * </ul>
 *
 * <h2>Error Code</h2>
 *
 * <p>{@code CONFIG_INVALID} - This error is <b>not retryable</b> as configuration must be fixed.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * if (config.get("url") == null) {
 *     throw new SourceProviderConfigException(
 *         "URL is required", "url");
 * }
 *
 * if (port < 1 || port > 65535) {
 *     throw new SourceProviderConfigException(
 *         "Port must be between 1 and 65535", config);
 * }
 * }</pre>
 *
 * @see SourceProviderConfig
 * @since 1.0
 */
public final class SourceProviderConfigException extends SourceProviderException {

  private final String missingKey;
  private final SourceProviderConfig invalidConfig;

  /**
   * Constructs a config exception with the specified message.
   *
   * @param message the detail message
   */
  public SourceProviderConfigException(String message) {
    super(message);
    this.missingKey = null;
    this.invalidConfig = null;
  }

  /**
   * Constructs a config exception with the specified message and cause.
   *
   * @param message the detail message
   * @param cause the underlying cause
   */
  public SourceProviderConfigException(String message, Throwable cause) {
    super(message, cause);
    this.missingKey = null;
    this.invalidConfig = null;
  }

  /**
   * Constructs a config exception for a missing required key.
   *
   * @param message the detail message
   * @param missingKey the configuration key that was missing
   */
  public SourceProviderConfigException(String message, String missingKey) {
    super(message);
    this.missingKey = missingKey;
    this.invalidConfig = null;
  }

  /**
   * Constructs a config exception with the invalid configuration.
   *
   * @param message the detail message
   * @param invalidConfig the configuration that failed validation
   */
  public SourceProviderConfigException(String message, SourceProviderConfig invalidConfig) {
    super(message);
    this.missingKey = null;
    this.invalidConfig = invalidConfig;
  }

  /**
   * Returns the missing configuration key, if applicable.
   *
   * @return the missing key, or {@code null} if not applicable
   */
  public String getMissingKey() {
    return missingKey;
  }

  /**
   * Returns the invalid configuration, if applicable.
   *
   * @return the invalid configuration, or {@code null} if not applicable
   */
  public SourceProviderConfig getInvalidConfig() {
    return invalidConfig;
  }

  /**
   * Returns {@code "CONFIG_INVALID"}.
   *
   * @return the error code
   */
  @Override
  public String getErrorCode() {
    return "CONFIG_INVALID";
  }
}
