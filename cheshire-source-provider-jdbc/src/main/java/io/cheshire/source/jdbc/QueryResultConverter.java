/*-
 * #%L
 * Cheshire :: Source Provider :: JDBC
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.source.jdbc;

import io.cheshire.spi.query.result.QueryEngineResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class QueryResultConverter {

  private QueryResultConverter() {}

  public static QueryEngineResult fromRows(List<Map<String, Object>> rows) {
    if (rows == null || rows.isEmpty()) {
      return new QueryEngineResult(Collections.emptyList(), Collections.emptyList());
    }

    // Infer columns from the first row
    Map<String, Object> firstRow = rows.get(0);
    List<QueryEngineResult.Column> columns =
        firstRow.keySet().stream()
            .map(
                key ->
                    new QueryEngineResult.Column(
                        key, inferType(firstRow.get(key)), true // assuming all columns
                        // nullable by default
                        ))
            .collect(Collectors.toList());

    return new QueryEngineResult(columns, rows);
  }

  private static String inferType(Object value) {
    if (value == null) return "Object";
    if (value instanceof Integer) return "Integer";
    if (value instanceof Long) return "Long";
    if (value instanceof Float) return "Float";
    if (value instanceof Double) return "Double";
    if (value instanceof Boolean) return "Boolean";
    if (value instanceof String) return "String";
    if (value instanceof java.util.Date) return "Date";
    return value.getClass().getSimpleName();
  }
}
