/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.config;

/**
 * Centralized constants for configuration map / YAML keys.
 *
 * <p>This enum is intended to replace the many hard-coded string keys currently used across the
 * prototype (e.g. "name", "sources", "config", "type", "connection", "url", ...).
 *
 * <p>Usage (future refactor):
 *
 * <pre>{@code
 * MapUtils.someValueFromMapAs(engineConfig, ConfigKeys.NAME.key(), String.class);
 * }</pre>
 *
 * <p>For now, this enum is only defined; existing usages still use string literals. See the
 * accompanying report for where these keys are currently hard-coded.
 */
public enum ConfigKeys {

  // ---------------------------------------------------------------------------
  // Top-level engine / source identifiers (from app-template.yaml)
  // ---------------------------------------------------------------------------

  /** Configuration key: {@code "name"} */
  NAME("name"),

  /** Configuration key: {@code "description"} */
  DESCRIPTION("description"),

  /** Configuration key: {@code "sources"} */
  SOURCES("sources"),

  /** Configuration key: {@code "config"} */
  CONFIG("config"),

  /** Configuration key: {@code "type"} */
  TYPE("type"),

  // ---------------------------------------------------------------------------
  // JDBC source configuration (connection section)
  // ---------------------------------------------------------------------------

  /** Nested config key: {@code "connection"} */
  CONNECTION("connection"),

  /** JDBC connection property: {@code "url"} */
  URL("url"),

  /** JDBC connection property: {@code "driver"} */
  DRIVER("driver"),

  /** JDBC connection property: {@code "username"} */
  USERNAME("username"),

  /** JDBC connection property: {@code "password"} */
  PASSWORD("password"),

  /** JDBC schema name (e.g. {@code "app"}, {@code "public"}) */
  SCHEMA("schema"),

  /** Auto-discovery flag: {@code "auto-discover-schema"} */
  AUTO_DISCOVER_SCHEMA("auto-discover-schema"),

  // ---------------------------------------------------------------------------
  // Query engine configuration keys (from app-template.yaml)
  // ---------------------------------------------------------------------------

  /** Engine default limit: {@code "defaultLimit"} */
  DEFAULT_LIMIT("defaultLimit"),

  /** Engine max limit: {@code "maxLimit"} */
  MAX_LIMIT("maxLimit"),

  /** Engine timeout in milliseconds: {@code "timeoutMs"} */
  TIMEOUT_MS("timeoutMs"),

  // ---------------------------------------------------------------------------
  // Higher-level Cheshire config structure (from CheshireConfig / template)
  // ---------------------------------------------------------------------------

  /** Top-level section: {@code "query-engines"} */
  QUERY_ENGINES("query-engines");

  private final String key;

  ConfigKeys(String key) {
    this.key = key;
  }

  /**
   * Returns the string value of this configuration key, to be used when accessing maps or building
   * YAML/JSON structures.
   */
  public String key() {
    return key;
  }

  @Override
  public String toString() {
    return key;
  }
}
