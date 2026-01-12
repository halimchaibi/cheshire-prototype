package io.cheshire.spi.query.engine;

import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.exception.QueryExecutionException;
import io.cheshire.spi.query.request.QueryRequest;
import io.cheshire.spi.query.result.MapQueryResult;

/**
 * Generic interface for a query engine.
 *
 * <p>A query engine is responsible for parsing, validating, planning, optimizing,
 * and executing queries against one or more data sources. Implementations may use
 * different underlying technologies (e.g., Apache Calcite, direct JDBC) but provide
 * a unified interface for query execution.</p>
 *
 * <p><strong>Lifecycle:</strong></p>
 * <ul>
 *   <li>Create engine instance via factory</li>
 *   <li>Call {@link #open()} to initialize and prepare the engine</li>
 *   <li>Execute queries using {@link #execute(QueryRequest, Object)}</li>
 *   <li>Call {@link #close()} when done to release resources</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> Implementations should document their thread-safety
 * guarantees. Generally, engines should be safe for concurrent query execution but
 * may require external synchronization for manager operations (open/close).</p>
 *
 * <p><strong>Resource Management:</strong> This interface extends {@link AutoCloseable}.
 * Always ensure proper resource cleanup by calling {@link #close()} or using
 * try-with-resources patterns.</p>
 *
 * @param <Q> type of query request payload (e.g., SqlQueryRequest)
 * @param <T> type of context object (e.g., SchemaManager, SourceProvider)
 * @author Cheshire Framework
 * @since 1.0.0
 */
public interface QueryEngine<Q extends QueryRequest<?, ?>, T> extends AutoCloseable {

    String name();

    /**
     * Initialize and open the engine for queries.
     *
     * <p>This method prepares the engine for query execution. It may:
     * <ul>
     *   <li>Initialize connection pools</li>
     *   <li>Register schemas and data sources</li>
     *   <li>Load configuration and validate settings</li>
     *   <li>Prepare query parsers and optimizers</li>
     * </ul>
     * </p>
     *
     * <p>This method must be called before any query operations. Calling it multiple
     * times should be idempotent.</p>
     *
     * @throws QueryEngineException if initialization fails (e.g., invalid configuration,
     *                              connection failures, resource allocation errors)
     */
    void open() throws QueryEngineException;

    /**
     * Execute a query request and return a typed result.
     *
     * <p>This method performs the complete query execution pipeline:
     * <ol>
     *   <li>Parse the query</li>
     *   <li>Validate against schemas</li>
     *   <li>Create and optimize execution plan</li>
     *   <li>Execute the plan</li>
     *   <li>Transform and return results</li>
     * </ol>
     * </p>
     *
     * <p>The context parameter provides access to data sources, schemas, or other
     * runtime information needed for query execution.</p>
     *
     * @param query   the query request to execute
     * @param context the execution context (e.g., schema manager, source provider)
     * @return the query result containing rows and column metadata
     * @throws QueryExecutionException if query execution fails (e.g., syntax errors,
     *                                 validation errors, execution failures, timeout)
     * @throws IllegalStateException   if the engine is not open
     */
    MapQueryResult execute(Q query, T context) throws QueryExecutionException;

    /**
     * Explain how the query will be executed (query plan, steps, etc.).
     *
     * <p>This method generates an execution plan without actually executing the query.
     * The returned string typically includes:
     * <ul>
     *   <li>Logical query plan</li>
     *   <li>Optimized physical plan</li>
     *   <li>Estimated costs</li>
     *   <li>Execution steps</li>
     * </ul>
     * </p>
     *
     * <p>Useful for debugging, optimization, and understanding query behavior.</p>
     *
     * @param query the query request to explain
     * @return a human-readable string representation of the execution plan
     * @throws QueryExecutionException if planning fails (e.g., invalid query syntax,
     *                                 validation errors)
     * @throws IllegalStateException   if the engine is not open
     */
    String explain(Q query) throws QueryExecutionException;

    /**
     * Validate the query request before execution.
     *
     * <p>This method performs syntax and semantic validation without executing the query.
     * It checks:
     * <ul>
     *   <li>Query syntax correctness</li>
     *   <li>Schema and table existence</li>
     *   <li>Column references</li>
     *   <li>Type compatibility</li>
     * </ul>
     * </p>
     *
     * <p>Note: This is a lightweight validation. Full validation may occur during
     * {@link #execute(QueryRequest, Object)}.</p>
     *
     * @param query the query request to validate
     * @return true if the query is valid, false otherwise
     */
    boolean validate(Q query);

    boolean isOpen();

    /**
     * Close and clean up resources.
     *
     * <p>This method releases all resources associated with the engine:
     * <ul>
     *   <li>Closes connections and connection pools</li>
     *   <li>Unregisters schemas</li>
     *   <li>Cleans up internal caches</li>
     *   <li>Shuts down background threads</li>
     * </ul>
     * </p>
     *
     * <p>After calling this method, the engine should not be used. Calling it multiple
     * times should be safe (idempotent).</p>
     *
     * @throws QueryEngineException if an error occurs during cleanup
     */
    @Override
    void close() throws QueryEngineException;
}
