/*-
 * #%L
 * Cheshire :: Source Provider :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.source;

import io.cheshire.spi.source.exception.SourceProviderConfigException;
import io.cheshire.spi.source.exception.SourceProviderException;
import java.util.Map;

/**
 * Configuration interface for source providers.
 *
 * <p>Implementations provide type-safe access to configuration properties used by {@link
 * SourceProvider} instances. Configurations should be immutable after construction.
 *
 * <h2>Design Principles</h2>
 *
 * <ul>
 *   <li><b>Immutability</b>: Configurations should not change after creation
 *   <li><b>Type Safety</b>: Use specific types (records, classes) rather than raw maps
 *   <li><b>Validation</b>: Validate in constructors or factory methods
 *   <li><b>Required vs Optional</b>: Use {@link #require(String)} for mandatory properties
 * </ul>
 *
 * <h2>Example Implementation</h2>
 *
 * <pre>{@code
 * public record JdbcSourceProviderConfig(
 *     String url,
 *     String username,
 *     String password,
 *     Map<String, Object> poolConfig
 * ) implements SourceProviderConfig {
 *
 *     @Override
 *     public String get(String key) {
 *         return switch(key) {
 *             case "url" -> url;
 *             case "username" -> username;
 *             case "password" -> password;
 *             default -> (String) poolConfig.get(key);
 *         };
 *     }
 *
 *     @Override
 *     public Map<String, Object> asMap() {
 *         return Map.of("url", url, "username", username, "poolConfig", poolConfig);
 *     }
 * }
 * }</pre>
 *
 * @see SourceProvider
 * @see SourceProviderFactory
 * @see SourceProviderConfigAdapter
 * @since 1.0
 */
public interface SourceProviderConfig {

  /**
   * Returns an immutable map representation of this configuration.
   *
   * <p>This method is useful for serialization, logging, and debugging. The returned map should
   * contain all configuration properties, though sensitive values (passwords) may be masked.
   *
   * @return an immutable map of configuration properties, never {@code null}
   */
  Map<String, Object> asMap();

  /**
   * Retrieves a configuration value by key.
   *
   * <p>Returns {@code null} if the key is not found. For required properties, use {@link
   * #require(String)} instead.
   *
   * @param key the configuration key, must not be {@code null}
   * @return the configuration value as a string, or {@code null} if not found
   * @see #require(String)
   */
  String get(String key);

  /**
   * Retrieves a required configuration value by key.
   *
   * <p>This method throws an exception if the key is missing or the value is blank, ensuring that
   * required configuration is present at runtime.
   *
   * <h3>Example Usage</h3>
   *
   * <pre>{@code
   * String url = config.require("url");  // Throws if missing
   * String optional = config.get("description");  // Returns null if missing
   * }</pre>
   *
   * @param key the configuration key, must not be {@code null}
   * @return the configuration value, never {@code null} or blank
   * @throws SourceProviderConfigException if the key is missing or the value is blank
   * @throws SourceProviderException if any other error occurs
   */
  default String require(String key) throws SourceProviderException {
    String value = get(key);
    if (value == null || value.isBlank()) {
      throw new SourceProviderConfigException("Required configuration key missing: " + key, key);
    }
    return value;
  }
}
