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

import io.cheshire.spi.source.SourceProviderQuery;
import java.util.Map;

public record SqlSourceProviderQuery(String sql, Map<String, Object> params)
    implements SourceProviderQuery {

  public static SqlSourceProviderQuery of(String sql, Map<String, Object> params) {
    return new SqlSourceProviderQuery(sql, params);
  }

  public static SqlSourceProviderQuery of(String sql) {
    return new SqlSourceProviderQuery(sql, Map.of());
  }

  @Override
  public Map<String, Object> parameters() {
    return parameters();
  }

  @Override
  public Object query() {
    return sql;
  }

  @Override
  public boolean hasParameters() {
    return !params.isEmpty();
  }
}
