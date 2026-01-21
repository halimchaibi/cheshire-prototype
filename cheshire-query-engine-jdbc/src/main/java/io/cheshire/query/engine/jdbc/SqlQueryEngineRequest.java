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

import io.cheshire.source.jdbc.SqlSourceProviderQuery;
import io.cheshire.spi.query.request.LogicalQuery;
import java.util.Map;
import java.util.Objects;

public record SqlQueryEngineRequest(String sqlQuery, Map<String, Object> parameters, String dialect)
    implements LogicalQuery {

  public SqlQueryEngineRequest {
    Objects.requireNonNull(sqlQuery, "SQL query cannot be null");
    Objects.requireNonNull(dialect, "SQL dialect cannot be null");
    parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
  }

  public SqlSourceProviderQuery toSqlQuery() {
    return new SqlSourceProviderQuery(sqlQuery, parameters());
  }

  @Override
  public String dialect() {
    return dialect;
  }

  @Override
  public String query() {
    return sqlQuery;
  }

  @Override
  public boolean hasParameters() {
    return !parameters.isEmpty();
  }
}
