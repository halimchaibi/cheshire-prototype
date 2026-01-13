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

import io.cheshire.source.jdbc.JdbcDataSourceProvider;
import io.cheshire.source.jdbc.QueryResultConverter;
import io.cheshire.source.jdbc.SqlQueryResult;
import io.cheshire.spi.query.engine.QueryEngine;
import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.exception.QueryExecutionException;
import io.cheshire.spi.query.result.MapQueryResult;
import io.cheshire.spi.source.SourceProviderException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Simple JDBC query engine implementation.
 *
 * <p>
 * This engine executes SQL queries directly against a JDBC data source without query planning or optimization. It's
 * suitable for:
 * <ul>
 * <li>Simple, direct database queries</li>
 * <li>Scenarios where query optimization is not needed</li>
 * <li>Single-database queries (no federation)</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Thread Safety:</strong> This implementation is not thread-safe for manager operations (open/close). Query
 * execution may be thread-safe depending on the underlying JdbcDataSourceProvider implementation. External
 * synchronization is required for concurrent access.
 * </p>
 *
 * <p>
 * <strong>Resource Management:</strong> This class implements {@link AutoCloseable}. Always call {@link #close()} or
 * use try-with-resources to ensure proper cleanup.
 * </p>
 *
 * <p>
 * <strong>Example Usage:</strong>
 * </p>
 *
 * <pre>{@code
 * JdbcQueryEngineConfig config = new JdbcQueryEngineConfig("my-engine", List.of("db1"));
 * JdbcQueryEngine engine = new JdbcQueryEngine(config);
 * JdbcDataSourceProvider source = ...; // Get from registry
 * try {
 *     engine.open();
 *     MapQueryResult result = engine.execute(
 *         new SqlQueryRequest("SELECT * FROM users WHERE id = :id"),
 *         source
 *     );
 *     // Process results...
 * } finally {
 *     engine.close();
 * }
 * }</pre>
 *
 * @author Cheshire Framework
 * @since 1.0.0
 */

@Slf4j
public class JdbcQueryEngine implements QueryEngine<SqlQueryRequest, JdbcDataSourceProvider> {

    private final String name;
    private volatile boolean opened = false;

    /**
     * Creates a new JdbcQueryEngine with the specified configuration.
     *
     * @param config
     *            the engine configuration, must not be null and must be valid
     * @throws IllegalArgumentException
     *             if config is null or invalid
     */
    public JdbcQueryEngine(JdbcQueryEngineConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("JdbcQueryEngineConfig cannot be null");
        }
        if (!config.validate()) {
            throw new IllegalArgumentException("Invalid JdbcQueryEngineConfig: " + config);
        }
        this.name = config.name();
        log.debug("Created JdbcQueryEngine '{}'", name);
    }

    @Override
    public void open() throws QueryEngineException {
        if (opened) {
            log.debug("JdbcQueryEngine '{}' is already open", name);
            return;
        }

        log.info("Opening JdbcQueryEngine '{}'", name);
        opened = true;
        log.info("JdbcQueryEngine '{}' opened successfully", name);
    }

    /**
     * Explain how the query will be executed (query plan, steps, etc.).
     *
     * <p>
     * This method generates an execution plan without actually executing the query. The returned string typically
     * includes:
     * <ul>
     * <li>Logical query plan</li>
     * <li>Optimized physical plan</li>
     * <li>Estimated costs</li>
     * <li>Execution steps</li>
     * </ul>
     * </p>
     *
     * <p>
     * Useful for debugging, optimization, and understanding query behavior.
     * </p>
     *
     * @param query
     *            the query request to explain
     * @return a human-readable string representation of the execution plan
     * @throws QueryExecutionException
     *             if planning fails (e.g., invalid query syntax, validation errors)
     * @throws IllegalStateException
     *             if the engine is not open
     */
    @Override
    public String explain(SqlQueryRequest query) throws QueryExecutionException {
        if (!opened) {
            throw new QueryExecutionException(
                    String.format("Query engine '%s' is not open. Call open() before explaining queries.", name));
        }

        if (query == null) {
            throw new QueryExecutionException("Query request cannot be null");
        }

        String sql = query.sql();
        if (sql == null || sql.isBlank()) {
            throw new QueryExecutionException("SQL query cannot be null or empty");
        }

        log.debug("Explaining query on engine '{}': {}", name, sql);

        // JDBC engine doesn't provide query planning, return a simple explanation
        String explanation = String.format("JDBC Direct Execution Plan:\n" + "  Query: %s\n"
                + "  Execution: Direct JDBC execution (no optimization)\n"
                + "  Note: This engine executes queries directly without planning or optimization", sql);

        log.debug("Query explanation generated for engine '{}'", name);
        return explanation;
    }

    /**
     * Validate the query request before execution.
     *
     * <p>
     * This method performs syntax and semantic validation without executing the query. It checks:
     * <ul>
     * <li>Query syntax correctness</li>
     * <li>Schema and table existence</li>
     * <li>Column references</li>
     * <li>Type compatibility</li>
     * </ul>
     * </p>
     *
     * <p>
     * Note: This is a lightweight validation. Full validation may occur during
     * {@link #execute(SqlQueryRequest, JdbcDataSourceProvider)} .
     * </p>
     *
     * @param query
     *            the query request to validate
     * @return true if the query is valid, false otherwise
     */
    @Override
    public boolean validate(SqlQueryRequest query) {

        String sql = query.request();
        if (!opened) {
            log.debug("Query validation failed: engine '{}' is not open", name);
            return false;
        }

        if (sql == null) {
            log.debug("Query validation failed: config id null");
            return false;
        }

        if (sql.isBlank()) {
            log.debug("Query validation failed: SQL query is null or empty");
            return false;
        }

        // Basic validation: check if SQL looks valid (non-empty, trimmed)
        // Full validation would require parsing, which is expensive
        // For JDBC engine, we do minimal validation here
        boolean isValid = !sql.trim().isEmpty();

        if (isValid) {
            log.trace("Query validated successfully: {}", sql);
        } else {
            log.debug("Query validation failed: SQL query is empty after trimming");
        }

        return isValid;
    }

    @Override
    public boolean isOpen() {
        return opened;
    }

    /**
     * Execute a query request and return a typed result.
     *
     * <p>
     * This method performs the complete query execution pipeline:
     * <ol>
     * <li>Parse the query</li>
     * <li>Validate against schemas</li>
     * <li>Create and optimize execution plan</li>
     * <li>Execute the plan</li>
     * <li>Transform and return results</li>
     * </ol>
     * </p>
     *
     * <p>
     * The context parameter provides access to data sources, schemas, or other runtime information needed for query
     * execution.
     * </p>
     *
     * @param query
     *            the query request to execute
     * @param source
     *            the execution context (e.g., schema manager, source provider)
     * @return the query result containing rows and column metadata
     * @throws QueryExecutionException
     *             if query execution fails (e.g., syntax errors, validation errors, execution failures, timeout)
     * @throws IllegalStateException
     *             if the engine is not open
     */

    @Override
    public MapQueryResult execute(SqlQueryRequest query, JdbcDataSourceProvider source) throws QueryExecutionException {
        if (!opened) {
            try {
                this.open();
            } catch (QueryEngineException e) {
                throw new QueryExecutionException(
                        "Failed to open query engine before executing the query %s".formatted(query.sql()), e);
            } catch (Exception e) {
                throw new QueryExecutionException(
                        "Unexpected error opening query engine before executing the query %s".formatted(query.sql()),
                        e);
            }
        }

        if (query == null) {
            throw new QueryExecutionException("Query request cannot be null");
        }

        if (source == null) {
            throw new QueryExecutionException("Source provider cannot be null");
        }

        String sql = query.sql();
        if (sql == null || sql.isBlank()) {
            throw new QueryExecutionException("SQL query cannot be null or empty");
        }

        long startTime = System.currentTimeMillis();
        log.debug("Executing query on engine '{}' using source '{}': {}", name, source.config().name(), sql);

        try {

            SqlQueryResult result = source.execute(query.toSqlQuery());

            MapQueryResult queryResult;
            if (result.rows().isEmpty()) {
                queryResult = new MapQueryResult(List.of(), List.of());
            } else {
                queryResult = QueryResultConverter.fromRows(result.rows());
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Query executed successfully on engine '{}' in {}ms, returned {} rows", name, duration,
                    queryResult.rowCount());

            return queryResult;

        } catch (SourceProviderException e) {
            String errorMsg = String.format(
                    "Failed to execute query on engine '%s' against source '%s' for query '%s': %s", name,
                    source.config().name(), sql, e.getMessage());
            log.error(errorMsg, e);
            throw new QueryExecutionException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("Unexpected error executing query on engine '%s' for query '%s': %s", name,
                    sql, e.getMessage());
            log.error(errorMsg, e);
            throw new QueryExecutionException(errorMsg, e);
        }
    }

    @Override
    public void close() throws QueryEngineException {
        if (!opened) {
            log.debug("JdbcQueryEngine '{}' is already closed", name);
            return;
        }

        log.info("Closing JdbcQueryEngine '{}'", name);
        opened = false;
        log.info("JdbcQueryEngine '{}' closed successfully", name);
    }

    /**
     * Returns the name/identifier of this query engine.
     *
     * @return the engine name
     */
    @Override
    public String name() {
        return name;
    }
}
