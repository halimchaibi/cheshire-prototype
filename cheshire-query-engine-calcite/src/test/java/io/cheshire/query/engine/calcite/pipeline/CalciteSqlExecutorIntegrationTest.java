/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import io.cheshire.core.constant.Key;
import io.cheshire.core.pipeline.MaterializedInput;
import io.cheshire.core.pipeline.MaterializedOutput;
import io.cheshire.query.engine.calcite.CalciteQueryEngine;
import io.cheshire.query.engine.calcite.config.CalciteQueryEngineConfig;
import io.cheshire.spi.pipeline.Context;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Integration test wiring {@link CalciteSqlExecutor} to a live {@link CalciteQueryEngine}. */
class CalciteSqlExecutorIntegrationTest {

  private CalciteQueryEngine engine;

  @BeforeEach
  void setUp() throws Exception {
    final Map<String, Object> connection =
        Map.of(
            "url",
            "jdbc:h2:mem:sql_executor_it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "driver",
            "org.h2.Driver",
            "username",
            "sa",
            "password",
            "");
    final Map<String, Object> source =
        Map.of(
            "name",
            "mem",
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
    engine =
        new CalciteQueryEngine(
            new CalciteQueryEngineConfig("sql-executor-it", Map.of("mem", source), Map.of()));
    engine.open();
  }

  @AfterEach
  void tearDown() {
    if (engine != null) {
      engine.close();
    }
  }

  @Test
  @DisplayName("CalciteSqlExecutor executes template SQL on a live engine")
  void executesTemplateSqlOnLiveEngine() throws Exception {
    final CalciteSqlExecutor executor =
        new CalciteSqlExecutor(
            Map.of("name", "constantQuery", "template", "SELECT 7 AS answer, 'it' AS kind"));
    final MaterializedInput input =
        new MaterializedInput(
            Map.of(Key.PAYLOAD_DATA.key(), Map.of()), Map.of(Key.ENGINE.key(), engine));

    final MaterializedOutput output = executor.apply(input, new TestContext());

    assertThat(output.data()).containsKey("rows").containsKey("columns");
    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> rows = (List<Map<String, Object>>) output.data().get("rows");
    assertThat(rows).hasSize(1);
    assertThat(rows.getFirst()).containsEntry("answer", 7).containsEntry("kind", "it");
    assertThat(output.metadata()).containsEntry("executor-name", "constantQuery");
  }

  private record TestContext(ConcurrentMap<String, Object> attributes) implements Context {
    private TestContext() {
      this(new ConcurrentHashMap<>());
    }
  }
}
