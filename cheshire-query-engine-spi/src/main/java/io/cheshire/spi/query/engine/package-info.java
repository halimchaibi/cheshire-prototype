/*-
 * #%L
 * Cheshire :: Query Engine :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

/**
 * Service Provider Interface (SPI) for query engine implementations.
 * <p>
 * <strong>Package Overview:</strong>
 * <p>
 * This package defines the SPI for pluggable query engines:
 * <ul>
 * <li><strong>QueryEngine</strong> - Main query execution interface</li>
 * <li><strong>QueryEngineFactory</strong> - SPI factory for engine creation</li>
 * <li><strong>QueryEngineConfig</strong> - Engine configuration interface</li>
 * </ul>
 * <p>
 * <strong>Query Engine Implementations:</strong>
 *
 * <pre>
 * QueryEngine (SPI)
 *   ├─ JdbcQueryEngine - Direct SQL execution
 *   └─ CalciteQueryEngine - Federated query processing
 * </pre>
 * <p>
 * <strong>SPI Discovery:</strong>
 * <p>
 * Query engines are discovered via Java's ServiceLoader mechanism:
 * <ol>
 * <li>Implement {@link io.cheshire.spi.query.engine.QueryEngineFactory}</li>
 * <li>Register in {@code META-INF/services/io.cheshire.spi.query.engine.QueryEngineFactory}</li>
 * <li>Framework automatically discovers and loads</li>
 * </ol>
 * <p>
 * <strong>Example Implementation:</strong>
 *
 * <pre>
 * {@code
 * // 1. Implement QueryEngine
 * public class JdbcQueryEngine implements QueryEngine<SqlQueryRequest, JdbcDataSourceProvider> {
 *     &#64;Override
 *     public MapQueryResult execute(SqlQueryRequest request, JdbcDataSourceProvider provider) {
 *         // Convert request to SQL query
 *         SqlQuery query = request.toSqlQuery();
 *
 *         // Execute via provider
 *         SqlQueryResult result = provider.execute(query);
 *
 *         // Convert to standard format
 *         return QueryResultConverter.fromRows(result.rows());
 *     }
 * }
 *
 * // 2. Implement QueryEngineFactory
 * public class JdbcQueryEngineFactory implements QueryEngineFactory<...> {
 *     &#64;Override
 *     public JdbcQueryEngine create(JdbcQueryEngineConfig config) {
 *         return new JdbcQueryEngine(config);
 *     }
 *
 *     @Override
 *     public ConfigAdapter<CheshireConfig.QueryEngine> adapter() {
 *         return (name, engineDef) -> JdbcQueryEngineConfig.from(name, engineDef);
 *     }
 * }
 *
 * // 3. Register in META-INF/services/io.cheshire.spi.query.engine.QueryEngineFactory
 * io.cheshire.query.engine.jdbc.JdbcQueryEngineFactory
 * }
 * </pre>
 * <p>
 * <strong>Configuration:</strong>
 *
 * <pre>{@code
 * query-engines:
 *   jdbc-engine:
 *     engine: io.cheshire.query.engine.jdbc.JdbcQueryEngineFactory
 *     sources: [my-db]
 *
 *   calcite-engine:
 *     engine: io.cheshire.query.engine.calcite.CalciteQueryEngineFactory
 *     sources: [db1, db2, api1]
 *     config:
 *       optimizer: true
 *       parallelism: 4
 * }</pre>
 * <p>
 * <strong>Query Request Types:</strong>
 * <p>
 * Different engines support different request types:
 * <ul>
 * <li><strong>SqlQueryRequest:</strong> SQL query with parameters</li>
 * <li><strong>CalciteQueryRequest:</strong> Relational algebra expression</li>
 * <li><strong>GraphQLQueryRequest:</strong> GraphQL query string</li>
 * </ul>
 * <p>
 * <strong>Query Result Format:</strong>
 * <p>
 * All engines return {@code MapQueryResult} for consistency:
 *
 * <pre>{@code
 * public record MapQueryResult(
 *     List<Column> columns,
 *     List<Map<String, Object>> rows
 * ) {
 *     public record Column(String name, String type, boolean nullable) {
 *     }
 * }
 * }</pre>
 * <p>
 * <strong>Lifecycle:</strong>
 * <ol>
 * <li><strong>Discovery:</strong> ServiceLoader finds all QueryEngineFactory implementations</li>
 * <li><strong>Configuration:</strong> Factory.adapter() converts YAML to typed config</li>
 * <li><strong>Creation:</strong> Factory.create() instantiates engine</li>
 * <li><strong>Initialization:</strong> Engine.open() prepares for execution</li>
 * <li><strong>Execution:</strong> Engine.execute() processes queries</li>
 * <li><strong>Shutdown:</strong> Engine.close() releases resources</li>
 * </ol>
 * <p>
 * <strong>Design Patterns:</strong>
 * <ul>
 * <li><strong>Strategy:</strong> Pluggable query engines</li>
 * <li><strong>Factory Method:</strong> Engine creation via SPI</li>
 * <li><strong>Adapter:</strong> Protocol-specific request/response conversion</li>
 * <li><strong>Template Method:</strong> Consistent lifecycle management</li>
 * </ul>
 *
 * @see io.cheshire.spi.query.engine.QueryEngine
 * @see io.cheshire.spi.query.engine.QueryEngineFactory
 * @see io.cheshire.spi.query.request.QueryRequest
 * @see io.cheshire.spi.query.result.QueryResult
 * @since 1.0.0
 */
package io.cheshire.spi.query.engine;
