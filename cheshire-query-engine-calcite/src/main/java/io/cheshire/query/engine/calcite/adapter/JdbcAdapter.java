/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.adapter;

import io.cheshire.common.utils.MapUtils;
import io.cheshire.spi.query.exception.QueryEngineConfigurationException;
import io.cheshire.spi.query.exception.QueryEngineInitializationException;
import java.util.HashMap;
import java.util.Map;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;

public class JdbcAdapter implements SourceAdapter {

  @Override
  public Schema createSchema(Map<String, Object> config, SchemaPlus schema)
      throws QueryEngineInitializationException {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> jdbcConfig =
          MapUtils.mayBeValueFromMapAs(config, "config", Map.class)
              .orElseThrow(
                  () ->
                      new QueryEngineConfigurationException(
                          "Source configuration missing, required 'config' field"));

      validate(jdbcConfig);

      return createJdbcSchema(jdbcConfig, schema);

    } catch (Exception e) {
      String name = MapUtils.mayBeValueFromMapAs(config, "name", String.class).orElse("unknown");
      throw new QueryEngineInitializationException(
          "Failed to create JDBC schema for source: " + name, e);
    }
  }

  private Schema createJdbcSchema(Map<String, Object> config, SchemaPlus rootSchema)
      throws QueryEngineConfigurationException {

    String schemaName =
        MapUtils.mayBeValueFromMapAs(config, "schema", String.class)
            .orElseThrow(
                () ->
                    new QueryEngineConfigurationException(
                        "Source config missing required 'type' field"))
            .toUpperCase();

    if (rootSchema.getSubSchema(schemaName) != null) {
      return rootSchema.getSubSchema(schemaName);
    }

    Map<String, Object> operand = asCalciteOperand(config);
    return JdbcSchema.create(rootSchema, schemaName, operand);
  }

  private Map<String, Object> asCalciteOperand(Map<String, Object> config)
      throws QueryEngineConfigurationException {

    Map<String, Object> operand = new HashMap<>();

    @SuppressWarnings("unchecked")
    Map<String, Object> connection =
        MapUtils.mayBeValueFromMapAs(config, "connection", Map.class)
            .orElseThrow(
                () ->
                    new QueryEngineConfigurationException(
                        "Source config missing required 'connection' config"));

    String jdbcUrl =
        MapUtils.mayBeValueFromMapAs(connection, "url", String.class)
            .orElseThrow(
                () ->
                    new QueryEngineConfigurationException(
                        "Source config missing required 'url' field"));

    String jdbcDriver =
        MapUtils.mayBeValueFromMapAs(connection, "driver", String.class)
            .orElseThrow(
                () ->
                    new QueryEngineConfigurationException(
                        "Source config missing required 'driver' field"));

    String username =
        MapUtils.mayBeValueFromMapAs(connection, "username", String.class).orElse(null);
    String password =
        MapUtils.mayBeValueFromMapAs(connection, "password", String.class).orElse(null);

    operand.put("jdbcUrl", jdbcUrl);
    operand.put("jdbcDriver", jdbcDriver);

    if (username != null) {
      operand.put("jdbcUser", username);
    }
    if (password != null) {
      operand.put("jdbcPassword", password);
    }

    return operand;
  }

  @Override
  public String supportedType() {
    return "JDBC";
  }
}
