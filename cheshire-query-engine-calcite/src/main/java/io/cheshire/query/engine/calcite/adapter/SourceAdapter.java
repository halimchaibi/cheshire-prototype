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
import java.util.Map;
import java.util.Objects;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;

public interface SourceAdapter {

  default void validate(Map<String, Object> config) throws QueryEngineConfigurationException {
    String type =
        MapUtils.someValueFromMapAs(config, "type", String.class)
            .orElseThrow(
                () ->
                    new QueryEngineConfigurationException(
                        "Source config missing required 'type' field"))
            .toUpperCase();

    if (!Objects.equals(type, supportedType())) {
      throw new QueryEngineConfigurationException(
          "Unsupported source type: " + type + ", expected: " + supportedType());
    }
  }

  Schema createSchema(Map<String, Object> config, SchemaPlus schema)
      throws QueryEngineInitializationException;

  String supportedType();
}
