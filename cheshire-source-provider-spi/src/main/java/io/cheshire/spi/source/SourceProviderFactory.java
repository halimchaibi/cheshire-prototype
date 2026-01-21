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

/**
 * Factory interface for creating {@link SourceProvider} instances.
 *
 * <p>Implementations of this interface are discovered via Java's {@link java.util.ServiceLoader}
 * mechanism, enabling pluggable source provider registration. Each factory is responsible for:
 *
 * <ul>
 *   <li>Defining the configuration type
 *   <li>Providing a configuration adapter for type conversion
 *   <li>Validating configurations before instantiation
 *   <li>Creating configured source provider instances
 * </ul>
 *
 * <h2>Service Provider Registration</h2>
 *
 * <p>To make a factory discoverable, create a file at:
 *
 * <pre>
 * META-INF/services/io.cheshire.spi.source.SourceProviderFactory
 * </pre>
 *
 * <p>containing the fully qualified class name of your factory implementation:
 *
 * <pre>
 * com.example.MySourceProviderFactory
 * </pre>
 *
 * <h2>Example Implementation</h2>
 *
 * <pre>{@code
 * public class JdbcSourceProviderFactory implements SourceProviderFactory<JdbcSourceProviderConfig> {
 *
 *     @Override
 *     public Class<JdbcSourceProviderConfig> configType() {
 *         return JdbcSourceProviderConfig.class;
 *     }
 *
 *     @Override
 *     public void validate(JdbcSourceProviderConfig config) throws SourceProviderException {
 *         if (config.url() == null || config.url().isBlank()) {
 *             throw new SourceProviderConfigException("JDBC URL is required", "url");
 *         }
 *         if (config.driver() == null) {
 *             throw new SourceProviderConfigException("JDBC driver is required", "driver");
 *         }
 *     }
 *
 *     @Override
 *     public SourceProviderConfigAdapter<JdbcSourceProviderConfig> adapter() {
 *         return map -> new JdbcSourceProviderConfig(
 *             (String) map.get("url"),
 *             (String) map.get("driver"),
 *             (String) map.get("username"),
 *             (String) map.get("password")
 *         );
 *     }
 *
 *     @Override
 *     public SourceProvider<?> create(JdbcSourceProviderConfig config)
 *             throws SourceProviderException {
 *         validate(config);
 *         return new JdbcSourceProvider(config);
 *     }
 * }
 * }</pre>
 *
 * @param <C> the configuration type extending {@link SourceProviderConfig}
 * @see SourceProvider
 * @see SourceProviderConfig
 * @see SourceProviderConfigAdapter
 * @see java.util.ServiceLoader
 * @since 1.0
 */
public interface SourceProviderFactory<C extends SourceProviderConfig> {

  /**
   * Returns the configuration class type handled by this factory.
   *
   * <p>This method enables type-safe configuration handling and runtime type discovery.
   *
   * @return the configuration class, never {@code null}
   */
  Class<C> configType();

  /**
   * Validates the given configuration before creating a source provider.
   *
   * <p>This method should check all required properties, validate formats, and ensure the
   * configuration is complete and correct. Validation failures should throw descriptive exceptions.
   *
   * <h3>Validation Checklist</h3>
   *
   * <ul>
   *   <li>Required fields are present
   *   <li>URLs and connection strings are valid
   *   <li>Credentials are provided (if authentication required)
   *   <li>Numeric values are in valid ranges
   *   <li>Enums have valid values
   * </ul>
   *
   * @param config the configuration to validate, must not be {@code null}
   * @throws io.cheshire.spi.source.exception.SourceProviderConfigException if validation fails
   * @throws SourceProviderException if any other error occurs during validation
   */
  void validate(C config) throws SourceProviderException;

  /**
   * Returns a configuration adapter for converting raw maps to typed configurations.
   *
   * <p>The adapter is used to transform {@code Map<String, Object>} (typically from YAML/JSON) into
   * strongly-typed configuration objects.
   *
   * @return the configuration adapter, never {@code null}
   * @see SourceProviderConfigAdapter
   */
  SourceProviderConfigAdapter<C> adapter();

  /**
   * Creates a new source provider instance with the given configuration.
   *
   * <p>This method should:
   *
   * <ol>
   *   <li>Validate the configuration (typically by calling {@link #validate(SourceProviderConfig)})
   *   <li>Instantiate the source provider
   *   <li>Return the uninitialized provider (caller will call {@link SourceProvider#open()})
   * </ol>
   *
   * <p>The returned provider should be in a closed state. Connection establishment and resource
   * allocation should be deferred to {@link SourceProvider#open()}.
   *
   * @param config the source provider configuration, must not be {@code null}
   * @return a new source provider instance, never {@code null}
   * @throws io.cheshire.spi.source.exception.SourceProviderInitializationException if instantiation
   *     fails
   * @throws SourceProviderException if any other error occurs
   */
  SourceProvider<?> create(C config) throws SourceProviderException;
}
