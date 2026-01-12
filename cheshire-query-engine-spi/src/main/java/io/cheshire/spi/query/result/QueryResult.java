package io.cheshire.spi.query.result;

import java.util.List;

/**
 * Generic query result interface.
 *
 * @param <R> type of a single row
 * @param <C> type of column metadata
 */
public interface QueryResult<R, C> extends Iterable<R> {

    /**
     * Returns the columns metadata.
     * Never returns null; may return empty list.
     */
    List<C> columns();

    /**
     * Returns the number of rows.
     */
    int rowCount();

    /**
     * Returns all rows as a List.
     * Default implementation returns an empty list.
     */
    List<R> rows();

}
