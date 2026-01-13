/*-
 * #%L
 * Cheshire :: Source Provider :: JDBC
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.source.jdbc;

import io.cheshire.spi.source.Query;

import java.util.Map;

/**
 * SQL query representation with named parameters.
 * <p>
 * Immutable record containing SQL string and parameter map for execution by {@link JdbcDataSourceProvider}.
 * <p>
 * <strong>Named Parameters:</strong> Use `:paramName` syntax in SQL, parameters provided in map with matching keys.
 * <p>
 * <strong>Example:</strong>
 *
 * <pre>{@code
 * SqlQuery query = SqlQuery.of("SELECT * FROM users WHERE id = :userId AND status = :status",
 *         Map.of("userId", 123, "status", "active"));
 * }</pre>
 *
 * @param sql
 *            SQL query string with named parameters
 * @param params
 *            parameter values keyed by parameter name
 * @see JdbcDataSourceProvider
 * @since 1.0.0
 */
public record SqlQuery(String sql, Map<String, Object> params) implements Query {

    public static SqlQuery of(String sql, Map<String, Object> params) {
        return new SqlQuery(sql, params);
    }

    public static SqlQuery of(String sql) {
        return new SqlQuery(sql, Map.of());
    }

}
