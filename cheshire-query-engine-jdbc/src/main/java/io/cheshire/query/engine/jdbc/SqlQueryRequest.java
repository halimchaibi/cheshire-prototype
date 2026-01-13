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

import io.cheshire.source.jdbc.SqlQuery;
import io.cheshire.spi.query.request.QueryRequest;

import java.util.Map;

/**
 * SQL query request implementation.
 *
 * <p>
 * Wraps a SQL query string for execution by query engines that support SQL. This is an immutable value object.
 * </p>
 *
 * @param sql
 *            the SQL query string, must not be null
 * @author Cheshire Framework
 * @since 1.0.0
 */
public record SqlQueryRequest(String sql,
        Map<String, Object> parameters) implements QueryRequest<String, Map<String, Object>> {

    /**
     * Creates a new SqlQueryRequest.
     *
     * @param sql
     *            the SQL query string, must not be null
     * @throws NullPointerException
     *             if sql is null
     */
    public SqlQueryRequest {
        if (sql == null) {
            throw new NullPointerException("SQL query cannot be null");
        }
        if (parameters == null) {
            parameters = Map.of();
        }
    }

    public SqlQuery toSqlQuery() {
        return new SqlQuery(sql(), parameters());
    }

    /**
     * Returns the engine-specific request content.
     *
     * <p>
     * The content type depends on the query engine:
     * <ul>
     * <li>SQL engines: SQL query string</li>
     * <li>GraphQL engines: GraphQL query string</li>
     * <li>REST/API engines: JSON map or request object</li>
     * <li>Custom engines: engine-specific query representation</li>
     * </ul>
     * </p>
     *
     * @return the query request payload, must not be null
     */
    @Override
    public String request() {
        return sql;
    }
}
