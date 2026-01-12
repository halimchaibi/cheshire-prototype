package io.cheshire.spi.source;

/**
 * Factory interface for creating {@link SourceProvider} instances.
 * <p>
 * <strong>SPI Pattern:</strong>
 * <p>
 * This abstraction allows the Cheshire framework to instantiate source providers
 * dynamically based on configuration without knowing the concrete implementation
 * (JDBC, REST, CSV, in-memory, etc.).
 * <p>
 * <strong>Type Parameters:</strong>
 * <ul>
 *   <li><strong>C:</strong> Configuration type (extends {@link SourceConfig})</li>
 *   <li><strong>S:</strong> Provider type (extends {@link SourceProvider})</li>
 *   <li><strong>A:</strong> Adapter input type (raw YAML config map)</li>
 * </ul>
 * <p>
 * <strong>Responsibilities:</strong>
 * <ul>
 *   <li>Validate the provided {@link SourceConfig}</li>
 *   <li>Build and return a fully initialized SourceProvider instance</li>
 *   <li>Provide configuration adapter for YAML-to-typed-config conversion</li>
 *   <li>Throw {@link SourceProviderException} if creation fails</li>
 * </ul>
 * <p>
 * <strong>Example Implementation:</strong>
 * <pre>{@code
 * public class JdbcDataSourceProviderFactory
 *     implements SourceProviderFactory<JdbcSourceConfig, JdbcDataSourceProvider, Map<String, Object>> {
 *
 *     @Override
 *     public JdbcDataSourceProvider create(JdbcSourceConfig config) {
 *         return new JdbcDataSourceProvider(config);
 *     }
 *
 *     @Override
 *     public ConfigAdapter<Map<String, Object>> adapter() {
 *         return (name, rawConfig) -> JdbcSourceConfig.from(name, rawConfig);
 *     }
 *
 *     @Override
 *     public Class<JdbcSourceConfig> configClass() {
 *         return JdbcSourceConfig.class;
 *     }
 *
 *     @Override
 *     public Class<JdbcDataSourceProvider> providerClass() {
 *         return JdbcDataSourceProvider.class;
 *     }
 * }
 * }</pre>
 *
 * @param <C> configuration type
 * @param <S> source provider type
 * @param <A> adapter input type (typically {@code Map<String, Object>})
 * @see SourceProvider
 * @see SourceConfig
 * @see ConfigAdapter
 * @since 1.0.0
 */
public interface SourceProviderFactory<C extends SourceConfig, S extends SourceProvider<?, ?>, A> {

    /**
     * Creates a new {@link SourceProvider} based on the provided configuration.
     * <p>
     * The factory is responsible for:
     * <ul>
     *   <li>Instantiating the provider</li>
     *   <li>Applying configuration settings</li>
     *   <li>Validating configuration (via {@link #validate(SourceConfig)})</li>
     *   <li>Returning a ready-to-open provider</li>
     * </ul>
     * <p>
     * The returned provider is in the NEW state and requires {@link SourceProvider#open()}
     * to be called before use.
     *
     * @param config typed configuration for the provider
     * @return initialized source provider instance (not yet opened)
     * @throws SourceProviderException if creation fails (invalid config, resource issues)
     */
    S create(C config) throws SourceProviderException;

    /**
     * Returns the configuration adapter for converting raw YAML to typed config.
     * <p>
     * The adapter is used by {@link io.cheshire.core.manager.SourceProviderManager}
     * to transform raw configuration maps into typed configuration objects.
     *
     * @return configuration adapter instance
     */
    ConfigAdapter<A> adapter();

    /**
     * Returns the configuration class this factory expects.
     * <p>
     * Used for type checking and reflection-based operations.
     *
     * @return configuration class
     */
    Class<C> configClass();

    /**
     * Returns the source provider class this factory creates.
     * <p>
     * Used for type checking and reflection-based operations.
     *
     * @return source provider class
     */
    Class<S> providerClass();

    /**
     * Validates configuration before provider creation.
     * <p>
     * Default implementation performs no validation. Implementations should
     * override to check:
     * <ul>
     *   <li>Required fields are present (URL, credentials, etc.)</li>
     *   <li>Values are within acceptable ranges</li>
     *   <li>Dependencies are available (drivers, libraries)</li>
     * </ul>
     *
     * @param config configuration to validate
     * @throws SourceProviderException if validation fails
     */
    default void validate(C config) throws SourceProviderException {
        // Default: no validation
    }
}