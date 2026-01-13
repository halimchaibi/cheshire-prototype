/*-
 * #%L
 * Cheshire :: Query Engine :: JDBC
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.jdbc;

import io.cheshire.core.config.CheshireConfig;
import io.cheshire.spi.query.engine.ConfigAdapter;
import io.cheshire.spi.query.engine.QueryEngineFactory;
import io.cheshire.spi.query.exception.QueryEngineException;

/**
 * Factory for creating {@link JdbcQueryEngine} instances.
 * <p>
 * <strong>SPI Implementation:</strong>
 * <p>
 * This factory is discovered via Java's Service Provider Interface (SPI). It's registered in
 * `META-INF/services/io.cheshire.spi.query.engine.QueryEngineFactory`.
 * <p>
 * <strong>Configuration Adaptation:</strong>
 * <p>
 * Converts raw YAML configuration ({@link CheshireConfig.QueryEngine}) into typed {@link JdbcQueryEngineConfig} via the
 * adapter pattern.
 * <p>
 * <strong>Example Configuration:</strong>
 *
 * <pre>{@code
 * query-engines:
 *   jdbc-engine:
 *     engine: io.cheshire.query.engine.jdbc.JdbcQueryEngineFactory
 *     sources: [blog-db]
 *     config:
 *       fetchSize: 100
 *       queryTimeout: 30
 * }</pre>
 *
 * @see JdbcQueryEngine
 * @see JdbcQueryEngineConfig
 * @see QueryEngineFactory
 * @since 1.0.0
 */
public class JdbcQueryEngineFactory
        implements QueryEngineFactory<JdbcQueryEngineConfig, JdbcQueryEngine, CheshireConfig.QueryEngine> {

    /**
     * Public no-arg constructor required by ServiceLoader.
     */
    public JdbcQueryEngineFactory() {
        // For ServiceLoader
    }

    /**
     * Creates a new JDBC query engine from configuration.
     *
     * @param config
     *            typed JDBC engine configuration
     * @return initialized query engine instance
     * @throws QueryEngineException
     *             if configuration is invalid or creation fails
     */
    @Override
    public JdbcQueryEngine create(JdbcQueryEngineConfig config) throws QueryEngineException {
        try {
            return new JdbcQueryEngine(config);
        } catch (IllegalArgumentException e) {
            throw new QueryEngineException("Failed to create JdbcQueryEngine", e);
        }
    }

    @Override
    public ConfigAdapter<CheshireConfig.QueryEngine> adapter() {
        return (name, queryConf) -> JdbcQueryEngineConfig.from(name, queryConf);
    }

    @Override
    public Class<JdbcQueryEngineConfig> configClass() {
        return JdbcQueryEngineConfig.class;
    }

    @Override
    public Class<JdbcQueryEngine> queryEngineClass() {
        return JdbcQueryEngine.class;
    }

    @Override
    public void validate(JdbcQueryEngineConfig config) throws QueryEngineException {
        QueryEngineFactory.super.validate(config);
    }
}
