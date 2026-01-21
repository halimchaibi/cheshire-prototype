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

import io.cheshire.spi.source.exception.SourceProviderException;
import java.util.Map;

/**
 * Functional interface for adapting raw configuration maps to typed configuration objects.
 *
 * <p>Adapters transform {@code Map<String, Object>} (typically loaded from YAML, JSON, or
 * properties files) into strongly-typed {@link SourceProviderConfig} implementations. This provides
 * type safety and enables configuration validation at construction time.
 *
 * <h2>Design Pattern</h2>
 *
 * <p>The adapter pattern separates configuration loading (generic maps) from configuration usage
 * (typed objects), allowing flexible serialization formats while maintaining type safety.
 *
 * <h2>Example Implementation</h2>
 *
 * <pre>{@code
 * SourceProviderConfigAdapter<JdbcSourceProviderConfig> adapter = map -> {
 *     // Extract and validate required fields
 *     String url = (String) map.get("url");
 *     if (url == null) {
 *         throw new SourceProviderConfigException("URL is required", "url");
 *     }
 *
 *     // Extract optional fields with defaults
 *     String driver = (String) map.getOrDefault("driver", "org.postgresql.Driver");
 *     Integer poolSize = (Integer) map.getOrDefault("poolSize", 10);
 *
 *     // Construct typed config (validation happens in constructor)
 *     return new JdbcSourceProviderConfig(url, driver, poolSize);
 * };
 * }</pre>
 *
 * <h2>Error Handling</h2>
 *
 * <p>Adapters should throw {@link io.cheshire.spi.source.exception.SourceProviderConfigException}
 * for configuration errors (missing keys, invalid formats, etc.) and preserve type-safety by
 * validating casts and conversions.
 *
 * <h2>Usage with ServiceLoader</h2>
 *
 * <p>Adapters are typically provided by {@link SourceProviderFactory#adapter()}:
 *
 * <pre>{@code
 * ServiceLoader<SourceProviderFactory> loader =
 *     ServiceLoader.load(SourceProviderFactory.class);
 *
 * for (SourceProviderFactory<?> factory : loader) {
 *     SourceProviderConfigAdapter<?> adapter = factory.adapter();
 *     SourceProviderConfig config = adapter.adapt(configMap);
 *     SourceProvider<?> provider = factory.create(config);
 * }
 * }</pre>
 *
 * @param <C> the configuration type extending {@link SourceProviderConfig}
 * @see SourceProviderConfig
 * @see SourceProviderFactory
 * @since 1.0
 */
@FunctionalInterface
public interface SourceProviderConfigAdapter<C extends SourceProviderConfig> {

  /**
   * Adapts a raw configuration map to a typed configuration object.
   *
   * <p>This method should:
   *
   * <ul>
   *   <li>Extract required and optional properties from the map
   *   <li>Perform type conversions and validations
   *   <li>Construct the typed configuration object
   *   <li>Throw descriptive exceptions for invalid configurations
   * </ul>
   *
   * <h3>Type Conversion Examples</h3>
   *
   * <pre>{@code
   * // String values
   * String url = (String) map.get("url");
   *
   * // Numeric values with defaults
   * int port = (Integer) map.getOrDefault("port", 5432);
   *
   * // Nested maps
   * Map<String, Object> poolConfig = (Map<String, Object>) map.get("pool");
   *
   * // Lists
   * List<String> hosts = (List<String>) map.get("hosts");
   * }</pre>
   *
   * @param config the raw configuration map, must not be {@code null}
   * @return a typed configuration object, never {@code null}
   * @throws io.cheshire.spi.source.exception.SourceProviderConfigException if configuration is
   *     invalid
   * @throws SourceProviderException if any other error occurs during adaptation
   * @throws ClassCastException if type conversion fails (should be caught and wrapped)
   */
  C adapt(Map<String, Object> config) throws SourceProviderException;
}
