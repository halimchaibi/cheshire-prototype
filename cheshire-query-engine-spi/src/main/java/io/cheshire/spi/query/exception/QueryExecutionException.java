package io.cheshire.spi.query.exception;

/**
 * Exception thrown when query execution fails.
 *
 * <p>This exception indicates that a query could not be executed successfully.
 * Common causes include:
 * <ul>
 *   <li>SQL syntax errors</li>
 *   <li>Schema validation failures</li>
 *   <li>Connection failures</li>
 *   <li>Query timeout</li>
 *   <li>Resource exhaustion</li>
 *   <li>Data type mismatches</li>
 * </ul>
 * </p>
 *
 * <p>This exception is distinct from {@link io.cheshire.spi.query.engine.QueryEngineException},
 * which indicates engine-level failures (initialization, configuration, etc.).</p>
 *
 * @author Cheshire Framework
 * @since 1.0.0
 */
public class QueryExecutionException extends Exception {

    /**
     * Constructs a new QueryExecutionException with the specified detail message.
     *
     * @param message the detail message explaining the execution failure
     */
    public QueryExecutionException(String message) {
        super(message);
    }

    /**
     * Constructs a new QueryExecutionException with the specified detail message and cause.
     *
     * @param message the detail message explaining the execution failure
     * @param cause   the underlying cause of this exception
     */
    public QueryExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
