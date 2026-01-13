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
import io.cheshire.spi.query.engine.QueryEngineConfig;

import java.util.List;
import java.util.Map;

/**
 * Configuration for the JDBC query engine.
 *
 * <p>
 * This configuration specifies:
 * <ul>
 * <li>The engine name/identifier</li>
 * <li>List of source names (for reference, though JDBC engine typically uses a single source)</li>
 * </ul>
 * </p>
 *
 * <p>
 * The JDBC query engine executes queries directly against a single JDBC data source without query planning or
 * optimization. It's suitable for simple, direct database queries.
 * </p>
 *
 * @param name
 *            the unique name/identifier for this query engine instance
 * @param sources
 *            the list of source names (typically contains one source for JDBC engine)
 * @author Cheshire Framework
 * @since 1.0.0
 */
public record JdbcQueryEngineConfig(String name, List<String> sources) implements QueryEngineConfig {

    /**
     * Creates a new JdbcQueryEngineConfig.
     *
     * @param name
     *            the engine name, must not be null or blank
     * @param sources
     *            the list of source names, may be null (treated as empty list)
     * @throws IllegalArgumentException
     *             if name is null or blank
     */
    public JdbcQueryEngineConfig {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Engine name cannot be null or blank");
        }
        // Normalize sources to immutable list
        sources = sources != null ? List.copyOf(sources) : List.of();
    }

    /**
     * Creates a JdbcQueryEngineConfig from a CheshireConfig QueryDefinition.
     *
     * @param name
     *            the engine name
     * @param queryConf
     *            the query definition from CheshireConfig
     * @return a new JdbcQueryEngineConfig instance
     * @throws IllegalArgumentException
     *             if name is null or blank
     */
    public static JdbcQueryEngineConfig from(String name, CheshireConfig.QueryEngine queryConf) {
        return new JdbcQueryEngineConfig(name, queryConf.getSources());
    }

    @Override
    public Map<String, Object> asMap() {
        return Map.of("name", name, "sources", sources);
    }

    @Override
    public boolean validate() {
        return name != null && !name.isBlank();
    }
}
