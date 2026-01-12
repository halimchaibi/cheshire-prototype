package io.cheshire.spi.query.engine;

import io.cheshire.spi.query.exception.QueryEngineException;

/**
 * Factory interface for creating {@link QueryEngine} instances.
 * <p>
 * <strong>SPI Pattern:</strong>
 * <p>
 * Implementations are discovered via Java's Service Provider Interface (SPI).
 * The factory class name is specified in configuration, and the framework
 * dynamically loads and instantiates it.
 * <p>
 * <strong>Type Parameters:</strong>
 * <ul>
 *   <li><strong>C:</strong> Configuration type (extends {@link QueryEngineConfig})</li>
 *   <li><strong>E:</strong> Engine type (extends {@link QueryEngine})</li>
 *   <li><strong>A:</strong> Adapter input type (raw YAML config map)</li>
 * </ul>
 * <p>
 * <strong>Configuration Adaptation:</strong>
 * <p>
 * The {@link #adapter()} method provides a {@link ConfigAdapter} that converts
 * raw YAML configuration maps into typed configuration objects. This allows
 * engine-specific configuration while maintaining a consistent loading mechanism.
 * <p>
 * <strong>Example Implementation:</strong>
 * <pre>{@code
 * public class JdbcQueryEngineFactory
 *     implements QueryEngineFactory<JdbcEngineConfig, JdbcQueryEngine, Map<String, Object>> {
 *
 *     @Override
 *     public JdbcQueryEngine create(JdbcEngineConfig config) {
 *         return new JdbcQueryEngine(config);
 *     }
 *
 *     @Override
 *     public ConfigAdapter<Map<String, Object>> adapter() {
 *         return (name, rawConfig) -> JdbcEngineConfig.from(rawConfig);
 *     }
 *
 *     @Override
 *     public Class<JdbcEngineConfig> configClass() {
 *         return JdbcEngineConfig.class;
 *     }
 *
 *     @Override
 *     public Class<JdbcQueryEngine> queryEngineClass() {
 *         return JdbcQueryEngine.class;
 *     }
 * }
 * }</pre>
 *
 * @param <C> configuration type
 * @param <E> query engine type
 * @param <A> adapter input type (typically {@code Map<String, Object>})
 * @see QueryEngine
 * @see QueryEngineConfig
 * @see ConfigAdapter
 * @since 1.0.0
 */
public interface QueryEngineFactory<C extends QueryEngineConfig, E extends QueryEngine<?, ?>, A> {

    /**
     * Creates a query engine instance from configuration.
     * <p>
     * The factory is responsible for:
     * <ul>
     *   <li>Instantiating the engine</li>
     *   <li>Applying configuration settings</li>
     *   <li>Validating configuration (via {@link #validate(QueryEngineConfig)})</li>
     *   <li>Returning a ready-to-open engine</li>
     * </ul>
     * <p>
     * The returned engine is in the NEW state and requires {@link QueryEngine#open()}
     * to be called before use.
     *
     * @param config typed configuration for the engine
     * @return initialized query engine instance (not yet opened)
     * @throws QueryEngineException if creation fails (invalid config, resource issues)
     */
    E create(C config) throws QueryEngineException;

    /**
     * Returns the configuration adapter for converting raw YAML to typed config.
     * <p>
     * The adapter is used by {@link io.cheshire.core.manager.QueryEngineManager}
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
     * Returns the query engine class this factory creates.
     * <p>
     * Used for type checking and reflection-based operations.
     *
     * @return query engine class
     */
    Class<E> queryEngineClass();

    /**
     * Validates configuration before engine creation.
     * <p>
     * Default implementation performs no validation. Implementations should
     * override to check:
     * <ul>
     *   <li>Required fields are present</li>
     *   <li>Values are within acceptable ranges</li>
     *   <li>Dependencies are available</li>
     * </ul>
     *
     * @param config configuration to validate
     * @throws QueryEngineException if validation fails
     */
    default void validate(C config) throws QueryEngineException {
        // Default: no validation
    }
}