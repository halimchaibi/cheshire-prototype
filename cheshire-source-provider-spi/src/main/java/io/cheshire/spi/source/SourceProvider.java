package io.cheshire.spi.source;

/**
 * Represents a **runtime data source** in the Cheshire query engine SPI.
 *
 * <p>A SourceProvider is responsible for managing a source’s manager,
 * exposing its configuration, and optionally executing queries or exposing
 * metadata. It abstracts away the underlying technology (JDBC, REST, CSV, etc.)
 * so the engine can interact with it in a uniform way.</p>
 *
 * <p>All implementations must be thread-safe and properly manage resources.</p>
 */
public interface SourceProvider<Q extends Query, R extends QueryResult> extends AutoCloseable {

    /**
     * Initializes the source.
     *
     * <p>This may include opening connections, initializing connection pools,
     * or preparing caches. Must be called before any operation on the source.</p>
     *
     * @throws SourceProviderException if initialization fails
     */
    void open() throws SourceProviderException;

    /**
     * Returns whether the source is currently open and ready for use.
     *
     * @return true if open, false if closed
     */
    boolean isOpen();

    /**
     * Returns the immutable configuration for this source.
     *
     * <p>The configuration is typically built from a {@link SourceConfig}
     * and should include all required connection details, source type,
     * and any optional settings.</p>
     *
     * @return the SourceConfigT instance
     */
    SourceConfig config();

    /**
     * Execute a query or statement of any type.
     * <p>
     * The source provider decides the semantics based on the input query.
     *
     * @param query engine-specific query payload (SQL, DSL, etc.)
     * @return result object representing the execution:
     * - SELECT returns List<Map<String,Object>>
     * - UPDATE/INSERT/DELETE returns number of affected rows (Integer)
     * - DDL or no-result operations can return null or empty object
     * @throws SourceProviderException on failure
     */
    R execute(Q query) throws SourceProviderException;

    /**
     * Closes the source and releases all resources.
     *
     * <p>This may include closing connections, shutting down pools,
     * and clearing internal caches. After calling this method,
     * {@link #isOpen()} must return false.</p>
     *
     * @throws SourceProviderException if an error occurs during closure
     */
    @Override
    void close() throws SourceProviderException;
}
