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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cheshire.query.engine.calcite.config.CalciteQueryEngineConfig;
import io.cheshire.query.engine.calcite.query.SqlQuery;
import io.cheshire.spi.query.exception.QueryEngineConfigurationException;
import io.cheshire.spi.query.exception.QueryEngineException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CalciteQueryEngineContractTest {

  @Test
  void validateAcceptsValidSqlAndRejectsInvalidSql() throws QueryEngineException {
    try (CalciteQueryEngine engine = openedEngine("calcite-contract-validation")) {
      assertAll(
          () -> assertTrue(engine.validate(new SqlQuery("SELECT 1 AS x", Map.of()))),
          () -> assertFalse(engine.validate(new SqlQuery("SELECT FROM", Map.of()))));
    }
  }

  @Test
  void explainReturnsRelationalPlanForValidSql() throws QueryEngineException {
    try (CalciteQueryEngine engine = openedEngine("calcite-contract-explain")) {
      String plan = engine.explain(new SqlQuery("SELECT 1 AS x", Map.of()));

      assertAll(
          () -> assertFalse(plan.isBlank()),
          () -> assertTrue(plan.contains("Logical"), plan),
          () -> assertTrue(plan.contains("1"), plan));
    }
  }

  @Test
  void supportsStreamingMatchesCurrentMaterializedExecutionModel() {
    CalciteQueryEngine engine =
        new CalciteQueryEngine(
            new CalciteQueryEngineConfig("calcite-contract-streaming", Map.of(), Map.of()));

    assertFalse(engine.supportsStreaming());
  }

  @Test
  void factoryValidationRejectsIncompleteConfiguration() {
    CalciteQueryEngineFactory factory = new CalciteQueryEngineFactory();

    assertAll(
        () ->
            assertThrows(
                QueryEngineConfigurationException.class,
                () ->
                    factory.validate(
                        new CalciteQueryEngineConfig("", Map.of("source", Map.of()), Map.of()))),
        () ->
            assertThrows(
                QueryEngineConfigurationException.class,
                () ->
                    factory.validate(new CalciteQueryEngineConfig("calcite", Map.of(), Map.of()))));
  }

  private CalciteQueryEngine openedEngine(String name) throws QueryEngineException {
    CalciteQueryEngine engine = new CalciteQueryEngine(engineConfig(name));
    engine.open();
    return engine;
  }

  private CalciteQueryEngineConfig engineConfig(String name) {
    Map<String, Object> connection =
        Map.of(
            "url",
            "jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "driver",
            "org.h2.Driver",
            "username",
            "sa",
            "password",
            "");

    Map<String, Object> source =
        Map.of(
            "name",
            "contract-db",
            "type",
            "jdbc",
            "config",
            Map.of(
                "type",
                "jdbc",
                "schema",
                "PUBLIC",
                "connection",
                connection,
                "auto-discover-schema",
                true));

    return new CalciteQueryEngineConfig(name, Map.of("contract-db", source), Map.of());
  }
}
