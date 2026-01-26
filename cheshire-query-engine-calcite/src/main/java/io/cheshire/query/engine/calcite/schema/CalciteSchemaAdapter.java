/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.schema;

import io.cheshire.common.utils.MapUtils;
import io.cheshire.query.engine.calcite.adapter.JdbcAdapter;
import io.cheshire.spi.query.exception.QueryEngineInitializationException;
import java.util.Map;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;

public class CalciteSchemaAdapter {

  private final JdbcAdapter jdbcAdapter;

  public CalciteSchemaAdapter() {
    this.jdbcAdapter = new JdbcAdapter();
  }

  public Schema createSchema(String name, Map<String, Object> config, SchemaPlus parent)
      throws QueryEngineInitializationException {

    String type =
        MapUtils.mayBeValueFromMapAs(config, "type", String.class)
            .orElseThrow(
                () ->
                    new QueryEngineInitializationException(
                        "Source config for '" + name + "' is missing required 'type' field"));

    return switch (type) {
      case "JDBC" -> jdbcAdapter.createSchema(config, parent);
      default -> throw new UnsupportedOperationException("Unknown source type: " + type);
    };
  }
}
