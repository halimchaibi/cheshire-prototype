package io.cheshire.source.jdbc;

import io.cheshire.core.config.CheshireConfig;
import io.cheshire.spi.source.ConfigAdapter;
import io.cheshire.spi.source.SourceProviderException;
import io.cheshire.spi.source.SourceProviderFactory;

/**
 * Factory for creating {@link JdbcDataSourceProvider} instances.
 * <p>
 * <strong>SPI Implementation:</strong>
 * <p>
 * Discovered via Java's Service Provider Interface (SPI), registered in
 * {@code META-INF/services/io.cheshire.spi.source.SourceProviderFactory}.
 * <p>
 * <strong>Configuration Adaptation:</strong>
 * <p>
 * Converts raw YAML configuration ({@link CheshireConfig.Source}) into
 * typed {@link JdbcDataSourceConfig} via adapter pattern.
 * <p>
 * <strong>Example Configuration:</strong>
 * <pre>{@code
 * sources:
 *   blog-db:
 *     factory: io.cheshire.source.jdbc.JdbcDataSourceProviderFactory
 *     type: jdbc
 *     description: "Blog database"
 *     config:
 *       connection:
 *         driver: "org.h2.Driver"
 *         url: "jdbc:h2:mem:blog"
 *         username: "sa"
 *         password: ""
 * }</pre>
 *
 * @see JdbcDataSourceProvider
 * @see JdbcDataSourceConfig
 * @see SourceProviderFactory
 * @since 1.0.0
 */
public class JdbcDataSourceProviderFactory implements SourceProviderFactory<JdbcDataSourceConfig, JdbcDataSourceProvider, CheshireConfig.Source> {

    /**
     * Public no-arg constructor required by ServiceLoader.
     */
    public JdbcDataSourceProviderFactory() {
        //
    }

    /**
     * Creates a new JDBC data source provider from configuration.
     *
     * @param config typed JDBC source configuration
     * @return initialized provider instance
     * @throws SourceProviderException if configuration is invalid or creation fails
     */
    @Override
    public JdbcDataSourceProvider create(JdbcDataSourceConfig config) throws SourceProviderException {
        try {
            return new JdbcDataSourceProvider(config);
        } catch (Exception e) {
            throw new SourceProviderException("Failed to initialize JDBC provider for: " + config.name(), e);
        }
    }

    @Override
    public ConfigAdapter<CheshireConfig.Source> adapter() {
        return (name, sourceDef) -> JdbcDataSourceConfig.from(name, sourceDef);
    }

    @Override
    public Class<JdbcDataSourceConfig> configClass() {
        return JdbcDataSourceConfig.class;
    }

    @Override
    public Class<JdbcDataSourceProvider> providerClass() {
        return JdbcDataSourceProvider.class;
    }

    @Override
    public void validate(JdbcDataSourceConfig config) throws SourceProviderException {
        SourceProviderFactory.super.validate(config);
    }

//
//    private final SourceProviderFactory<JdbcDataSourceConfig, SourceProvider> delegate;
//
//    public JdbcDataSourceProviderFactory(
//            SourceProviderFactory<JdbcDataSourceConfig, SourceProvider> delegate) {
//        this.delegate = delegate;
//    }
//
//    @Override
//    public JdbcDataSourceProvider create(JdbcDataSourceConfig config) throws SourceProviderException {
//        SourceProvider provider = delegate.create(config);
//
//        if (!(provider instanceof JdbcDataSourceProvider jdbcProvider)) {
//            throw new SourceProviderException("Factory returned unexpected provider type: "
//                    + provider.getClass().getName());
//        }
//        //jdbcProvider.open();
//        return jdbcProvider;
//    }
}

