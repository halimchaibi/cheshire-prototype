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

import static io.cheshire.core.server.CheshireServerFactory.log;

import io.cheshire.spi.source.SourceProviderConfigAdapter;
import java.util.Collections;
import java.util.Map;

public class JdbcSourceProviderConfigAdapter
    implements SourceProviderConfigAdapter<JdbcSourceProviderConfig> {

  @Override
  public JdbcSourceProviderConfig adapt(Map<String, Object> config) {

    log.debug("Creating JdbcDataSourceConfig from source definition: {}", config);

    Map<String, Object> pool =
        config != null && config.containsKey("pool")
            ? (Map<String, Object>) config.get("pool")
            : Collections.emptyMap();

    Map<String, Object> connection =
        config != null && config.containsKey("connection")
            ? (Map<String, Object>) config.get("connection")
            : Collections.emptyMap();

    String schema =
        config != null && config.containsKey("schema") ? config.get("schema").toString() : null;

    String type =
        config != null && config.containsKey("type") ? config.get("type").toString() : null;

    if (connection == null || connection.isEmpty()) {
      throw new IllegalArgumentException("Missing 'connection' section in JDBC source config");
    }

    if (pool == null || pool.isEmpty()) {
      throw new IllegalArgumentException(
          "JDBC 'driver' and 'url' must be defined in connection config");
    }

    // TODO: Use a builder pattern here.
    return new JdbcSourceProviderConfig(pool, connection, schema, type);
  }
}
