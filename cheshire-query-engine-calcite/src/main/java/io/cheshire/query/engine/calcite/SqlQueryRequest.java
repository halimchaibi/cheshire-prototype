/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite;

import io.cheshire.spi.query.request.QueryRequest;

import java.util.Map;

public class SqlQueryRequest implements QueryRequest<String, Map<String, Object>> {

    private final String sql;
    private final Map<String, Object> parameters;

    public SqlQueryRequest(String sql, Map<String, Object> parameters) {
        this.sql = sql;
        this.parameters = parameters;
    }

    @Override
    public String request() {
        return sql;
    }

    @Override
    public Map<String, Object> parameters() {
        return parameters;
    }
}
