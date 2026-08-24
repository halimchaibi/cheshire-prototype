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

import io.cheshire.spi.source.SourceProviderConfig;
import io.cheshire.spi.source.exception.SourceProviderConfigException;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record JdbcSourceProviderConfig(
    Map<String, Object> pool, Map<String, Object> connection, String schema, String type)
    implements SourceProviderConfig {

  private static final Logger log = LoggerFactory.getLogger(JdbcSourceProviderConfig.class);

  @Override
  public String type() {
    return type;
  }

  @Override
  public Map<String, Object> asMap() {
    return Collections.unmodifiableMap(
        Map.of(
            "pool", pool,
            "connection", connection,
            "schema", schema,
            "type", type));
  }

  @Override
  public String get(String key) {
    // TODO: handle nested keys ...
    Object value =
        switch (key) {
          case "schema" -> schema;
          case "type" -> type;
          case "url" -> connection.get("url").toString();
          case "driver" -> connection.get("driver").toString();
          case "username" -> connection.get("username").toString();
          case "password" -> connection.get("password").toString();
          default -> null;
        };
    return value == null ? null : value.toString();
  }

  @Override
  public String require(String key) throws SourceProviderConfigException {
    // TODO: Useless as of now. Needs refactor, it should provide any key, and should be overloaded
    // to accept a type ...
    String value = get(key);
    if (value == null || value.isBlank()) {
      throw new SourceProviderConfigException("Required configuration key missing: " + key);
    }
    return value;
  }
}
