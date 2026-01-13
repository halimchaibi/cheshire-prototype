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

import io.cheshire.spi.source.QueryResult;
import io.cheshire.spi.source.SourceProviderException;

import java.util.List;
import java.util.Map;

/**
 * SQL query result containing rows as maps.
 * <p>
 * Each row is represented as {@code Map<String, Object>} where keys are column names and values are column values.
 * <p>
 * <strong>Data Representation:</strong>
 *
 * <pre>{@code
 * // Example result:
 * [
 *   {"id": 1, "name": "Alice", "age": 30},
 *   {"id": 2, "name": "Bob", "age": 25}
 * ]
 * }</pre>
 * <p>
 * <strong>Resource Management:</strong> Currently no resources to close. For streaming results, consider extending with
 * ResultSet management.
 *
 * @param rows
 *            list of result rows, each row is a column-name to value map
 * @see JdbcDataSourceProvider
 * @since 1.0.0
 */
public record SqlQueryResult(List<Map<String, Object>> rows) implements QueryResult {

    public static SqlQueryResult of(List<Map<String, Object>> rows) {
        return new SqlQueryResult(rows);
    }

    @Override
    public void close() throws SourceProviderException {
        // TODO Auto-generated method stub
    }
}
