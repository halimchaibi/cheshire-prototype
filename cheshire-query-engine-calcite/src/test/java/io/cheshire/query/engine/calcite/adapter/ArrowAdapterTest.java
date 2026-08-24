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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.cheshire.query.engine.calcite.CalciteQueryEngine;
import io.cheshire.query.engine.calcite.config.CalciteQueryEngineConfig;
import io.cheshire.query.engine.calcite.schema.CalciteSchemaAdapter;
import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.exception.QueryEngineInitializationException;
import java.nio.file.Path;
import java.util.Map;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.tools.Frameworks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArrowAdapterTest {

  @TempDir private Path arrowDirectory;

  @Test
  void createsArrowSchemaFromDirectoryBackedSourceConfig()
      throws QueryEngineInitializationException {
    final SchemaPlus rootSchema = Frameworks.createRootSchema(true);
    final Map<String, Object> sourceConfig =
        Map.of(
            "name",
            "warehouse",
            "type",
            "arrow",
            "config",
            Map.of("type", "arrow", "directory", arrowDirectory.toString()));

    final Schema schema =
        new CalciteSchemaAdapter().createSchema("warehouse", sourceConfig, rootSchema);

    assertThat(schema).isNotNull();
    assertThat(schema.getTableNames()).isEmpty();
  }

  @Test
  void opensCalciteEngineWithArrowSourceConfig() throws QueryEngineException {
    final Map<String, Object> sourceConfig =
        Map.of(
            "name",
            "warehouse",
            "type",
            "arrow",
            "config",
            Map.of("type", "arrow", "directory", arrowDirectory.toString()));
    final CalciteQueryEngineConfig engineConfig =
        new CalciteQueryEngineConfig("calcite-arrow", Map.of("warehouse", sourceConfig), Map.of());

    try (CalciteQueryEngine engine = new CalciteQueryEngine(engineConfig)) {
      engine.open();

      assertThat(engine.name()).isEqualTo("calcite-arrow");
    }
  }

  @Test
  void rejectsArrowSourceConfigWithoutDirectory() {
    final SchemaPlus rootSchema = Frameworks.createRootSchema(true);
    final Map<String, Object> sourceConfig =
        Map.of("name", "warehouse", "type", "arrow", "config", Map.of("type", "arrow"));

    assertThatThrownBy(
            () -> new CalciteSchemaAdapter().createSchema("warehouse", sourceConfig, rootSchema))
        .isInstanceOf(QueryEngineInitializationException.class)
        .hasMessageContaining("Failed to create Arrow schema")
        .hasRootCauseMessage("Source config missing required 'directory' field");
  }
}
