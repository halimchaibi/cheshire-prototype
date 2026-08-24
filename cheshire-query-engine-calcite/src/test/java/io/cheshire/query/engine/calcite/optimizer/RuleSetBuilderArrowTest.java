/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.optimizer;

import static org.assertj.core.api.Assertions.assertThat;

import io.cheshire.query.engine.calcite.schema.SchemaManager;
import io.cheshire.spi.query.exception.QueryEngineInitializationException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.calcite.adapter.arrow.ArrowRules;
import org.apache.calcite.tools.Frameworks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RuleSetBuilderArrowTest {

  @TempDir private Path arrowDirectory;

  @Test
  void includesArrowAdapterRulesForArrowSources() throws QueryEngineInitializationException {
    final SchemaManager schemaManager =
        SchemaManager.builder()
            .withRootSchema(Frameworks.createRootSchema(true))
            .addSource(
                "warehouse",
                Map.of(
                    "name",
                    "warehouse",
                    "type",
                    "arrow",
                    "config",
                    Map.of("type", "arrow", "directory", arrowDirectory.toString())))
            .build();

    final RuleSetManager ruleSet =
        RuleSetBuilder.forSources(List.of("warehouse")).withSchemaManager(schemaManager).build();

    assertThat(ruleSet.getRules())
        .contains(ArrowRules.FILTER_SCAN, ArrowRules.PROJECT_SCAN, ArrowRules.TO_ENUMERABLE);
  }
}
