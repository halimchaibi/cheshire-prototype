/*-
 * #%L
 * Cheshire :: Query Engine :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.query.result;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Map-based QueryResult implementation. Each row is represented as Map<String,Object>. Columns use a simple built-in
 * column type.
 */
public final class MapQueryResult implements QueryResult<Map<String, Object>, MapQueryResult.Column> {

    private final List<Column> columns;
    private final List<Map<String, Object>> rows;

    public MapQueryResult(List<Column> columns, List<Map<String, Object>> rows) {
        this.columns = columns != null ? List.copyOf(columns) : Collections.emptyList();
        this.rows = rows != null ? List.copyOf(rows) : Collections.emptyList();
    }

    @Override
    public List<Column> columns() {
        return columns;
    }

    @Override
    public int rowCount() {
        return rows.size();
    }

    /**
     * Returns all rows as a List.
     */
    @Override
    public List<Map<String, Object>> rows() {
        return rows;
    }

    /**
     * Returns true if no rows exist.
     */
    public boolean empty() {
        return rows.isEmpty();
    }

    /**
     * Returns the row at the specified index.
     */
    public Map<String, Object> row(int index) {
        if (index < 0 || index >= rows.size()) {
            throw new IndexOutOfBoundsException("Invalid row index: " + index);
        }
        return rows.get(index);
    }

    @Override
    public Iterator<Map<String, Object>> iterator() {
        return rows.iterator();
    }

    /**
     * Built-in column type for map-based rows.
     *
     * <p>
     * Represents metadata for a single column in a query result, including the column name, data type, and nullability
     * information.
     * </p>
     *
     * @param name
     *            the column name (case-sensitive as returned by the query)
     * @param type
     *            the data type name (e.g., "VARCHAR", "INTEGER", "DOUBLE")
     * @param nullable
     *            true if the column allows NULL values, false otherwise
     */
    public record Column(String name, String type, boolean nullable) {
        /**
         * Creates a new Column with the specified properties.
         *
         * @param name
         *            the column name, must not be null
         * @param type
         *            the data type name, must not be null
         * @param nullable
         *            whether the column allows NULL values
         * @throws NullPointerException
         *             if name or type is null
         */
        public Column {
            if (name == null) {
                throw new NullPointerException("Column name cannot be null");
            }
            if (type == null) {
                throw new NullPointerException("Column type cannot be null");
            }
        }
    }
}
