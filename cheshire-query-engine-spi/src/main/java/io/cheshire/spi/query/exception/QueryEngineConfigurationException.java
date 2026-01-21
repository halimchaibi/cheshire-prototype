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

import io.cheshire.spi.query.engine.QueryEngineConfig;

public final class QueryEngineConfigurationException extends QueryEngineException {

  private final QueryEngineConfig invalidConfig;

  public QueryEngineConfigurationException(String message) {
    super(message);
    this.invalidConfig = null;
  }

  public QueryEngineConfigurationException(String message, Throwable cause) {
    super(message, cause);
    this.invalidConfig = null;
  }

  public QueryEngineConfigurationException(String message, QueryEngineConfig invalidConfig) {
    super(message);
    this.invalidConfig = invalidConfig;
  }

  public QueryEngineConfigurationException(
      String message, QueryEngineConfig invalidConfig, Throwable cause) {
    super(message, cause);
    this.invalidConfig = invalidConfig;
  }

  public QueryEngineConfig getInvalidConfig() {
    return invalidConfig;
  }

  @Override
  public String getErrorCode() {
    return "CONFIG_INVALID";
  }
}
