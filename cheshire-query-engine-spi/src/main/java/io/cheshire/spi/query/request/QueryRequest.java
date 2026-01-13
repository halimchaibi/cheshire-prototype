/*-
 * #%L
 * Cheshire :: Query Engine :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.query.request;

/**
 * Generic query request interface.
 *
 * <p>
 * Represents a query request for any query engine type. The request payload type is parameterized to support different
 * query formats:
 * <ul>
 * <li>SQL queries: {@code QueryRequest<String>}</li>
 * <li>GraphQL queries: {@code QueryRequest<String>}</li>
 * <li>Elasticsearch queries: {@code QueryRequest<Map<String, Object>>}</li>
 * <li>Custom query objects: {@code QueryRequest<CustomQueryType>}</li>
 * </ul>
 * </p>
 *
 * <p>
 * Implementations should be immutable value objects.
 * </p>
 *
 * @param <T>
 *            the type of the query payload (e.g., String for SQL, Map for JSON queries)
 * @author Cheshire Framework
 * @since 1.0.0
 */
public interface QueryRequest<Q, P> {

    /**
     * Returns the engine-specific request content.
     *
     * <p>
     * The content type depends on the query engine:
     * <ul>
     * <li>SQL engines: SQL query string</li>
     * <li>GraphQL engines: GraphQL query string</li>
     * <li>REST/API engines: JSON map or request object</li>
     * <li>Custom engines: engine-specific query representation</li>
     * </ul>
     * </p>
     *
     * @return the query request payload, must not be null
     */
    Q request();

    P parameters();
}
