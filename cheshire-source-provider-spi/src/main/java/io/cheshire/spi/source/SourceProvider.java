/*-
 * #%L
 * Cheshire :: Source Provider :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.source;

import io.cheshire.spi.source.exception.SourceProviderConnectionException;
import io.cheshire.spi.source.exception.SourceProviderException;
import io.cheshire.spi.source.exception.SourceProviderExecutionException;

/**
 * Service Provider Interface (SPI) for pluggable data source providers.
 *
 * <p>A {@code SourceProvider} represents a connection to a data source (database, REST API, file
 * system, etc.) and provides a uniform interface for executing queries against heterogeneous data
 * sources.
 *
 * <h2>Lifecycle</h2>
 *
 * <p>Source providers follow a managed lifecycle:
 *
 * <ol>
 *   <li><b>Creation</b>: Instantiated via {@link SourceProviderFactory}
 *   <li><b>Opening</b>: Resources allocated via {@link #open()}
 *   <li><b>Execution</b>: Queries executed via {@link #execute(SourceProviderQuery)}
 *   <li><b>Closing</b>: Resources released via {@link #close()}
 * </ol>
 *
 * <h2>Thread Safety</h2>
 *
 * <p>Implementations should be thread-safe for concurrent query execution after {@link #open()} is
 * called. The {@link #open()} and {@link #close()} methods may be synchronized to prevent
 * concurrent lifecycle changes.
 *
 * <h2>Resource Management</h2>
 *
 * <p>This interface extends {@link AutoCloseable} to support try-with-resources:
 *
 * <pre>{@code
 * try (SourceProvider<SqlQuery> provider = factory.create(config)) {
 *     provider.open();
 *     SourceProviderQueryResult result = provider.execute(query);
 *     // Process result
 * }
 * }</pre>
 *
 * <h2>Example Implementation</h2>
 *
 * <pre>{@code
 * public class JdbcSourceProvider implements SourceProvider<SqlQuery> {
 *     private final JdbcSourceProviderConfig config;
 *     private volatile DataSource dataSource;
 *     private final AtomicBoolean isOpen = new AtomicBoolean(false);
 *
 *     @Override
 *     public void open() throws SourceProviderConnectionException {
 *         if (isOpen.compareAndSet(false, true)) {
 *             dataSource = createDataSource(config);
 *         }
 *     }
 *
 *     @Override
 *     public SourceProviderQueryResult execute(SqlQuery query)
 *             throws SourceProviderException {
 *         if (!isOpen.get()) {
 *             throw new SourceProviderExecutionException("Provider not open");
 *         }
 *         // Execute query against dataSource
 *     }
 * }
 * }</pre>
 *
 * @param <Q> the query type extending {@link SourceProviderQuery}
 * @see SourceProviderFactory
 * @see SourceProviderConfig
 * @see SourceProviderQuery
 * @see SourceProviderQueryResult
 * @since 1.0
 */
public interface SourceProvider<Q extends SourceProviderQuery> extends AutoCloseable {

  /**
   * Returns the unique name or identifier of this source provider.
   *
   * <p>This name is used for logging, monitoring, and distinguishing between multiple source
   * providers. It should be stable across restarts and uniquely identify this source within the
   * application context.
   *
   * @return the source provider name, never {@code null}
   */
  String name();

  /**
   * Opens the connection to the data source and allocates necessary resources.
   *
   * <p>This method must be called before {@link #execute(SourceProviderQuery)} can be used. Calling
   * {@code open()} on an already open provider should be idempotent (no-op).
   *
   * <h3>Implementation Notes</h3>
   *
   * <ul>
   *   <li>Establish connections (database, HTTP client, file handles, etc.)
   *   <li>Initialize connection pools if applicable
   *   <li>Validate connectivity to the data source
   *   <li>Should be idempotent (safe to call multiple times)
   *   <li>Should set {@link #isOpen()} to return {@code true} upon success
   * </ul>
   *
   * @throws SourceProviderConnectionException if the connection cannot be established
   * @throws io.cheshire.spi.source.exception.SourceProviderInitializationException if
   *     initialization fails
   * @see #isOpen()
   * @see #close()
   */
  void open() throws SourceProviderConnectionException;

  /**
   * Checks whether this source provider is currently open and ready to execute queries.
   *
   * @return {@code true} if the provider is open and operational, {@code false} otherwise
   * @see #open()
   */
  boolean isOpen();

  /**
   * Returns the configuration used by this source provider.
   *
   * <p>The configuration is immutable and contains all settings used to create this provider.
   *
   * @return the source provider configuration, never {@code null}
   */
  SourceProviderConfig config();

  /**
   * Executes a query against the data source and returns the results.
   *
   * <p>This is the primary method for interacting with the data source. The query type {@code Q} is
   * specific to the implementation (e.g., {@code SqlQuery}, {@code RestQuery}, {@code FileQuery}).
   *
   * <h3>Preconditions</h3>
   *
   * <ul>
   *   <li>The provider must be open ({@link #isOpen()} returns {@code true})
   *   <li>The query must not be {@code null}
   *   <li>The query must be valid for this provider type
   * </ul>
   *
   * <h3>Thread Safety</h3>
   *
   * <p>This method should be thread-safe and support concurrent execution by multiple threads.
   *
   * @param query the query to execute, must not be {@code null}
   * @return the query results, never {@code null}
   * @throws SourceProviderExecutionException if query execution fails
   * @throws io.cheshire.spi.source.exception.SourceProviderTimeoutException if execution times out
   * @throws SourceProviderException if any other error occurs
   * @see SourceProviderQuery
   * @see SourceProviderQueryResult
   */
  SourceProviderQueryResult execute(Q query) throws SourceProviderException;

  /**
   * Closes the source provider and releases all associated resources.
   *
   * <p>This method should:
   *
   * <ul>
   *   <li>Close connections (database, HTTP, file handles, etc.)
   *   <li>Shutdown connection pools
   *   <li>Release any allocated resources
   *   <li>Be idempotent (safe to call multiple times)
   *   <li>Set {@link #isOpen()} to return {@code false}
   * </ul>
   *
   * <p>After calling {@code close()}, the provider should not be used for query execution. Attempts
   * to execute queries after closing should throw {@link SourceProviderExecutionException}.
   *
   * @throws SourceProviderException if an error occurs during cleanup
   */
  @Override
  void close() throws SourceProviderException;
}
