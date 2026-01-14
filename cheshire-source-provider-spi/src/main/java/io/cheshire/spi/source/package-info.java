/*-
 * #%L
 * Cheshire :: Source Provider :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

/**
 * Service Provider Interface (SPI) for data source provider implementations.
 * <p>
 * <strong>Package Overview:</strong>
 * <p>
 * This package defines the SPI for pluggable data source providers:
 * <ul>
 * <li><strong>SourceProvider</strong> - Main data source interface</li>
 * <li><strong>SourceProviderFactory</strong> - SPI factory for provider creation</li>
 * <li><strong>SourceConfig</strong> - Source configuration interface</li>
 * <li><strong>Query</strong> - Abstract query interface</li>
 * <li><strong>QueryResult</strong> - Abstract result interface</li>
 * </ul>
 * <p>
 * <strong>Source Provider Implementations:</strong>
 *
 * <pre>
 * SourceProvider (SPI)
 *   ├─ JdbcDataSourceProvider - SQL databases (PostgreSQL, MySQL, H2, etc.)
 *   ├─ VectorStoreProvider - Vector databases (ChromaDB, Pinecone)
 *   ├─ RestApiProvider - REST API integration
 *   └─ SparkProvider - Apache Spark clusters
 * </pre>
 * <p>
 * <strong>SPI Discovery:</strong>
 * <p>
 * Source providers are discovered via Java's ServiceLoader:
 * <ol>
 * <li>Implement {@link io.cheshire.spi.source.SourceProviderFactory}</li>
 * <li>Register in {@code META-INF/services/io.cheshire.spi.source.SourceProviderFactory}</li>
 * <li>Framework automatically discovers and loads</li>
 * </ol>
 * <p>
 * <strong>Example Implementation:</strong>
 *
 * <pre>
 * {@code
 * // 1. Implement SourceProvider
 * public class JdbcDataSourceProvider implements SourceProvider<SqlQuery, SqlQueryResult> {
 *     private final DataSource dataSource;
 *
 *     &#64;Override
 *     public SqlQueryResult execute(SqlQuery query) {
 *         try (Connection conn = dataSource.getConnection();
 *              PreparedStatement stmt = prepareStatement(conn, query)) {
 *
 *             ResultSet rs = stmt.executeQuery();
 *             List<Map<String, Object>> rows = extractRows(rs);
 *             return new SqlQueryResult(rows);
 *         }
 *     }
 * }
 *
 * // 2. Implement SourceProviderFactory
 * public class JdbcDataSourceProviderFactory implements SourceProviderFactory<...> {
 *     &#64;Override
 *     public JdbcDataSourceProvider create(JdbcDataSourceConfig config) {
 *         DataSource ds = createDataSource(config);
 *         return new JdbcDataSourceProvider(config, ds);
 *     }
 *
 *     &#64;Override
 *     public ConfigAdapter<CheshireConfig.Source> adapter() {
 *         return (name, sourceDef) -> JdbcDataSourceConfig.from(name, sourceDef);
 *     }
 * }
 *
 * // 3. Register in META-INF/services/io.cheshire.spi.source.SourceProviderFactory
 * io.cheshire.source.jdbc.JdbcDataSourceProviderFactory
 * }
 * </pre>
 * <p>
 * <strong>Configuration:</strong>
 *
 * <pre>{@code
 * sources:
 *   my-db:
 *     factory: io.cheshire.source.jdbc.JdbcDataSourceProviderFactory
 *     type: jdbc
 *     description: "PostgreSQL database"
 *     config:
 *       connection:
 *         driver: org.postgresql.Driver
 *         url: jdbc:postgresql://localhost:5432/mydb
 *         username: user
 *         password: password
 *       pool:
 *         maxSize: 20
 *         minIdle: 5
 * }</pre>
 * <p>
 * <strong>Query Execution Flow:</strong>
 *
 * <pre>
 * QueryEngine
 *      ↓
 * QueryEngine.execute(QueryRequest, SourceProvider)
 *      ↓
 * SourceProvider.execute(Query)
 *      ↓
 * Physical Resource (Database, API, etc.)
 *      ↓
 * QueryResult
 * </pre>
 * <p>
 * <strong>Connection Management:</strong>
 *
 * <pre>{@code
 * public interface SourceProvider<Q extends Query, R extends QueryResult> {
 *     // Open connections, initialize pools
 *     void open() throws SourceProviderException;
 *
 *     // Execute query
 *     R execute(Q query) throws SourceProviderException;
 *
 *     // Close connections, release resources
 *     void close() throws SourceProviderException;
 *
 *     // Health check
 *     boolean isHealthy();
 * }
 * }</pre>
 * <p>
 * <strong>Parameter Binding:</strong>
 * <p>
 * Source providers handle parameter binding for security:
 *
 * <pre>{@code
 * // Named parameters in query
 * SqlQuery query = SqlQuery.of("SELECT * FROM users WHERE id = :userId AND status = :status",
 *         Map.of("userId", 123, "status", "active"));
 *
 * // Provider converts to prepared statement
 * PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ? AND status = ?");
 * stmt.setInt(1, 123);
 * stmt.setString(2, "active");
 * }</pre>
 * <p>
 * <strong>Result Conversion:</strong>
 * <p>
 * Providers convert native results to standard format:
 *
 * <pre>{@code
 * // JDBC ResultSet → List<Map<String, Object>>
 * List<Map<String, Object>> rows = new ArrayList<>();
 * while (rs.next()) {
 *     Map<String, Object> row = new HashMap<>();
 *     ResultSetMetaData meta = rs.getMetaData();
 *     for (int i = 1; i <= meta.getColumnCount(); i++) {
 *         row.put(meta.getColumnName(i), rs.getObject(i));
 *     }
 *     rows.add(row);
 * }
 * }</pre>
 * <p>
 * <strong>Design Patterns:</strong>
 * <ul>
 * <li><strong>Adapter:</strong> Adapt various data sources to common interface</li>
 * <li><strong>Factory Method:</strong> Provider creation via SPI</li>
 * <li><strong>Template Method:</strong> Consistent lifecycle management</li>
 * <li><strong>Resource Acquisition:</strong> Try-with-resources for cleanup</li>
 * </ul>
 *
 * @see io.cheshire.spi.source.SourceProvider
 * @see io.cheshire.spi.source.SourceProviderFactory
 * @see io.cheshire.spi.source.Query
 * @see io.cheshire.spi.source.QueryResult
 * @since 1.0.0
 */
package io.cheshire.spi.source;
