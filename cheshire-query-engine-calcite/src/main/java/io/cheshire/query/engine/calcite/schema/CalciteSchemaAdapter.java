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
import io.cheshire.core.config.ConfigKeys;
import io.cheshire.query.engine.calcite.adapter.SourceAdapterRegistry;
import io.cheshire.spi.query.exception.QueryEngineInitializationException;
import java.util.Map;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;

public final class CalciteSchemaAdapter {

  private final SourceAdapterRegistry registry;

  public CalciteSchemaAdapter() {
    this(SourceAdapterRegistry.load());
  }

  CalciteSchemaAdapter(final SourceAdapterRegistry registry) {
    this.registry = registry;
  }

  public Schema createSchema(
      final String name, final Map<String, Object> config, final SchemaPlus parent)
      throws QueryEngineInitializationException {

    final String type =
        MapUtils.someValueFromMapAs(config, ConfigKeys.TYPE.key(), String.class)
            .orElseThrow(
                () ->
                    new QueryEngineInitializationException(
                        "Source config for '" + name + "' is missing required 'type' field"));

    return registry.adapterFor(type).createSchema(name, config, parent);
  }
}
