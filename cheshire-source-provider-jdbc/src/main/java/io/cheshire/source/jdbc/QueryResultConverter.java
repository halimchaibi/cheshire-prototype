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

import io.cheshire.spi.query.result.MapQueryResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts JDBC result rows into {@link MapQueryResult} with column metadata.
 * <p>
 * <strong>Conversion Strategy:</strong>
 * <ol>
 * <li>Infers column names and types from first row</li>
 * <li>Creates {@link MapQueryResult.Column} metadata for each column</li>
 * <li>Assumes all columns nullable</li>
 * <li>Returns empty result for empty input</li>
 * </ol>
 * <p>
 * <strong>Type Inference:</strong>
 * <p>
 * Maps Java types to string representations: Integer, Long, Float, Double, Boolean, String, Date. Falls back to simple
 * class name for other types.
 * <p>
 * <strong>Limitations:</strong>
 * <ul>
 * <li>Type inferred from first row only (assumes homogeneous columns)</li>
 * <li>No precision or scale metadata</li>
 * <li>All columns assumed nullable</li>
 * </ul>
 *
 * @see MapQueryResult
 * @see JdbcDataSourceProvider
 * @since 1.0.0
 */
public final class QueryResultConverter {

    /**
     * Private constructor to prevent instantiation.
     */
    private QueryResultConverter() {
    }

    /**
     * Converts a list of rows (Map<String,Object>) into a MapQueryResult. Column metadata is inferred from the first
     * row.
     *
     * @param rows
     *            list of result rows from JDBC
     * @return MapQueryResult with inferred column metadata
     */
    public static MapQueryResult fromRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return new MapQueryResult(Collections.emptyList(), Collections.emptyList());
        }

        // Infer columns from the first row
        Map<String, Object> firstRow = rows.get(0);
        List<MapQueryResult.Column> columns = firstRow.keySet().stream()
                .map(key -> new MapQueryResult.Column(key, inferType(firstRow.get(key)), true // assuming all columns
                                                                                              // nullable by default
                )).collect(Collectors.toList());

        return new MapQueryResult(columns, rows);
    }

    /**
     * Infers a string representation of the type for a value.
     */
    private static String inferType(Object value) {
        if (value == null)
            return "Object";
        if (value instanceof Integer)
            return "Integer";
        if (value instanceof Long)
            return "Long";
        if (value instanceof Float)
            return "Float";
        if (value instanceof Double)
            return "Double";
        if (value instanceof Boolean)
            return "Boolean";
        if (value instanceof String)
            return "String";
        if (value instanceof java.util.Date)
            return "Date";
        return value.getClass().getSimpleName();
    }
}
