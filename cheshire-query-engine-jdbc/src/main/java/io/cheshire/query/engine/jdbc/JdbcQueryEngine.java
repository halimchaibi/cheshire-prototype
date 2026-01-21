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

import io.cheshire.source.jdbc.JdbcSourceProvider;
import io.cheshire.source.jdbc.QueryResultConverter;
import io.cheshire.source.jdbc.SqlSourceProviderQueryResult;
import io.cheshire.spi.query.engine.QueryEngine;
import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.exception.QueryExecutionException;
import io.cheshire.spi.query.request.QueryEngineContext;
import io.cheshire.spi.query.result.QueryEngineResult;
import io.cheshire.spi.source.SourceProvider;
import io.cheshire.spi.source.exception.SourceProviderException;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * Lightweight JDBC query engine for direct SQL execution.
 *
 * <p>This engine provides minimal overhead by passing SQL queries directly to JDBC source providers
 * without planning, optimization, or rewriting. It is designed for:
 *
 * <ul>
 *   <li>Single-source SQL queries
 *   <li>Development and testing
 *   <li>Simple OLTP applications
 *   <li>Low-latency requirements
 * </ul>
 *
 * <h2>Limitations</h2>
 *
 * <ul>
 *   <li><b>Single Source Only</b>: Uses only the first JDBC source from the context
 *   <li><b>No Optimization</b>: Queries are passed through without modification
 *   <li><b>No Planning</b>: No cost-based optimization or query rewriting
 *   <li><b>No Streaming</b>: All results loaded into memory
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * JdbcQueryEngineConfig config = new JdbcQueryEngineConfig("my-engine", List.of("db1"));
 * QueryEngine<SqlQueryEngineRequest> engine = new JdbcQueryEngine(config);
 *
 * QueryEngineContext context = new QueryEngineContext(..., List.of(jdbcSourceProvider), ...);
 * SqlQueryEngineRequest query = new SqlQueryEngineRequest("SELECT * FROM users", Map.of());
 *
 * QueryEngineResult result = engine.execute(query, context);
 * }</pre>
 *
 * @see QueryEngine
 * @see JdbcSourceProvider
 * @since 1.0
 */
@Slf4j
public class JdbcQueryEngine implements QueryEngine<SqlQueryEngineRequest> {

  private final String name;
  private volatile boolean opened = false;

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

  @Override
  public String explain(SqlQueryEngineRequest query) throws QueryExecutionException {
    if (!opened) {
      throw new QueryExecutionException(
          String.format(
              "Query engine '%s' is not open. Call open() before explaining queries.", name));
    }

    if (query == null) {
      throw new QueryExecutionException("Query request cannot be null");
    }

    if (query.sqlQuery() == null || query.sqlQuery().isBlank()) {
      throw new QueryExecutionException("SQL query cannot be null or empty");
    }

    log.debug("Explaining query on engine '{}': {}", name, query.sqlQuery());

    // JDBC engine doesn't provide query planning, return a simple explanation
    String explanation =
        String.format(
            "JDBC Direct Execution Plan:\n"
                + "  Query: %s\n"
                + "  Execution: Direct JDBC execution (no optimization)\n"
                + "  Note: This engine executes queries directly without planning or optimization",
            query.query());

    log.debug("Query explanation generated for engine '{}'", name);
    return explanation;
  }

  @Override
  public boolean validate(SqlQueryEngineRequest query) {

    String sql = query.sqlQuery();
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
  public boolean supportsStreaming() {
    return false;
  }

  @Override
  public boolean isOpen() {
    return opened;
  }

  @Override
  public QueryEngineResult execute(SqlQueryEngineRequest query, QueryEngineContext ctx)
      throws QueryEngineException {

    Objects.requireNonNull(query, "Query cannot be null");
    Objects.requireNonNull(ctx, "QueryEngineContext cannot be null");

    List<SourceProvider<?>> sources = ctx.sources();

    Objects.requireNonNull(sources, "Sources provider cannot be null");
    if (sources.isEmpty()) {
      throw new QueryExecutionException("No source providers available in the context");
    }

    // TODO: This is for prototyping only - need to support multiple sources and types, merge
    // results, this what will be done with an concrete implementation using Calcite.
    JdbcSourceProvider source =
        switch (sources.getFirst()) {
          case JdbcSourceProvider jdbc -> jdbc;
          default ->
              throw new QueryExecutionException(
                  "Only JDBC is supported, found: " + sources.getFirst().name());
        };

    String sql = query.sqlQuery();
    if (sql == null || sql.isBlank()) {
      throw new QueryExecutionException("SQL query cannot be null or empty");
    }

    if (!opened) {
      try {
        this.open();
      } catch (QueryEngineException e) {
        throw new QueryExecutionException(
            "Failed to open query engine before executing the query %s".formatted(query.sqlQuery()),
            e);
      } catch (Exception e) {
        throw new QueryExecutionException(
            "Unexpected error opening query engine before executing the query %s"
                .formatted(query.sqlQuery()),
            e);
      }
    }

    long startTime = System.currentTimeMillis();
    log.debug(
        "Executing query on engine '{}' using source '{}': {}",
        name,
        source.config().schema(),
        sql);

    try {

      SqlSourceProviderQueryResult result = source.execute(query.toSqlQuery());

      QueryEngineResult queryResult;
      if (result.rows().isEmpty()) {
        queryResult = new QueryEngineResult(List.of(), List.of());
      } else {
        queryResult = QueryResultConverter.fromRows(result.rows());
      }

      long duration = System.currentTimeMillis() - startTime;
      log.info(
          "Query executed successfully on engine '{}' in {}ms, returned {} rows",
          name,
          duration,
          queryResult.rowCount());

      return queryResult;

    } catch (SourceProviderException e) {
      String errorMsg =
          String.format(
              "Failed to execute query on engine '%s' against source '%s' for query '%s': %s",
              name, source.config().schema(), sql, e.getMessage());
      log.error(errorMsg, e);
      throw new QueryExecutionException(errorMsg, e);
    } catch (Exception e) {
      String errorMsg =
          String.format(
              "Unexpected error executing query on engine '%s' for query '%s': %s",
              name, sql, e.getMessage());
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

  @Override
  public String name() {
    return name;
  }
}
