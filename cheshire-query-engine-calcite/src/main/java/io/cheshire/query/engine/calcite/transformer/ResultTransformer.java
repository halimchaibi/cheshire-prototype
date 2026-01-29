/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.transformer;

import io.cheshire.spi.query.result.QueryEngineResult;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;

public class ResultTransformer {

  /** Transform Enumerator to QueryEngineResult uses RelNode for type info */
  public QueryEngineResult transform(RelNode plan, Enumerator<Object[]> enumerator) {
    if (enumerator == null) {
      return new QueryEngineResult(Collections.emptyList(), Collections.emptyList());
    }

    // Extract column metadata from the plan
    RelDataType rowType = plan.getRowType();
    List<QueryEngineResult.Column> columns = new ArrayList<>();
    List<String> columnNames = new ArrayList<>();

    for (RelDataTypeField field : rowType.getFieldList()) {
      String columnName = field.getName();
      String typeName = field.getType().getSqlTypeName().toString();
      boolean nullable = field.getType().isNullable();

      columnNames.add(columnName);
      columns.add(new QueryEngineResult.Column(columnName, typeName, nullable));
    }

    List<Map<String, Object>> rows = new ArrayList<>();

    try {
      while (enumerator.moveNext()) {
        Object[] current = enumerator.current();
        if (current == null) {
          continue;
        }

        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(columnNames.size(), current.length); i++) {
          row.put(columnNames.get(i), current[i]);
        }
        rows.add(row);
      }
    } finally {
      enumerator.close();
    }

    return new QueryEngineResult(columns, rows);
  }

  /** Transform ResultSet to QueryEngineResult */
  public QueryEngineResult transform(ResultSet resultSet) throws SQLException {
    if (resultSet == null) {
      return new QueryEngineResult(Collections.emptyList(), Collections.emptyList());
    }

    // Extract column metadata from ResultSet
    ResultSetMetaData metaData = resultSet.getMetaData();
    int columnCount = metaData.getColumnCount();

    List<QueryEngineResult.Column> columns = new ArrayList<>();
    List<String> columnNames = new ArrayList<>();

    for (int i = 1; i <= columnCount; i++) {
      String columnName = metaData.getColumnLabel(i);
      String typeName = metaData.getColumnTypeName(i);
      boolean nullable = metaData.isNullable(i) != ResultSetMetaData.columnNoNulls;

      columnNames.add(columnName);
      columns.add(new QueryEngineResult.Column(columnName, typeName, nullable));
    }

    // Convert rows to Map<String, Object>
    List<Map<String, Object>> rows = new ArrayList<>();

    while (resultSet.next()) {
      Map<String, Object> row = new LinkedHashMap<>();
      for (int i = 1; i <= columnCount; i++) {
        row.put(columnNames.get(i - 1), resultSet.getObject(i));
      }
      rows.add(row);
    }

    return new QueryEngineResult(columns, rows);
  }

  /**
   * Transform ResultSet to QueryEngineResult (with RelNode for type info) Uses Calcite's type
   * information instead of JDBC's
   */
  public QueryEngineResult transform(ResultSet resultSet, RelNode plan) throws SQLException {
    if (resultSet == null) {
      return new QueryEngineResult(Collections.emptyList(), Collections.emptyList());
    }

    // Get column info from RelNode (more accurate for Calcite types)
    RelDataType rowType = plan.getRowType();
    List<QueryEngineResult.Column> columns = new ArrayList<>();
    List<String> columnNames = new ArrayList<>();

    for (RelDataTypeField field : rowType.getFieldList()) {
      String columnName = field.getName();
      String typeName = field.getType().getSqlTypeName().toString();
      boolean nullable = field.getType().isNullable();

      columnNames.add(columnName);
      columns.add(new QueryEngineResult.Column(columnName, typeName, nullable));
    }

    // Convert rows
    List<Map<String, Object>> rows = new ArrayList<>();

    while (resultSet.next()) {
      Map<String, Object> row = new LinkedHashMap<>();
      for (int i = 0; i < columnNames.size(); i++) {
        row.put(columnNames.get(i), resultSet.getObject(i + 1)); // JDBC is 1-indexed
      }
      rows.add(row);
    }

    return new QueryEngineResult(columns, rows);
  }
}
