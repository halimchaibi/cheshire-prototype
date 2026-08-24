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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.cheshire.query.engine.calcite.config.CalciteQueryEngineConfig;
import io.cheshire.query.engine.calcite.query.SqlQuery;
import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.request.QueryEngineContext;
import io.cheshire.spi.query.result.QueryEngineResult;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration coverage for the staged Calcite pipeline: validate → explain → execute against a
 * real H2 Chinook schema through {@link CalciteQueryEngine}.
 */
class CalciteQueryEnginePipelineIntegrationTest {

  private static final String DB_URL =
      "jdbc:h2:mem:chinook_pipeline_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
  private static final String DB_USER = "sa";
  private static final String DB_PASSWORD = "";

  private Connection h2Connection;
  private CalciteQueryEngine engine;

  @BeforeEach
  void setUp() throws Exception {
    h2Connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    try (Statement stmt = h2Connection.createStatement()) {
      stmt.execute("DROP ALL OBJECTS");
    }
    loadChinookSchema();

    final Map<String, Object> connection =
        Map.of(
            "url", DB_URL,
            "driver", "org.h2.Driver",
            "username", DB_USER,
            "password", DB_PASSWORD);
    final Map<String, Object> source =
        Map.of(
            "name",
            "chinook-db",
            "type",
            "jdbc",
            "config",
            Map.of(
                "type",
                "jdbc",
                "schema",
                "public",
                "connection",
                connection,
                "auto-discover-schema",
                true));

    engine =
        new CalciteQueryEngine(
            new CalciteQueryEngineConfig("pipeline-it", Map.of("chinook-db", source), Map.of()));
    engine.open();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (engine != null) {
      engine.close();
    }
    if (h2Connection != null && !h2Connection.isClosed()) {
      h2Connection.close();
    }
  }

  @Nested
  @DisplayName("Pipeline cutoffs")
  class PipelineCutoffs {

    @Test
    void validateExplainAndExecuteShareConsistentResults() throws QueryEngineException {
      final String sql =
          "SELECT artistid, name FROM `chinook-db`.artist WHERE artistid = 1";

      assertThat(engine.validate(new SqlQuery(sql, Map.of()))).isTrue();

      final String plan = engine.explain(new SqlQuery(sql, Map.of()));
      assertThat(plan).isNotBlank().containsIgnoringCase("Logical");

      final QueryEngineResult result =
          engine.execute(new SqlQuery(sql, Map.of()), QueryEngineContext.empty());

      assertThat(result.rows().size()).isEqualTo(1);
      assertThat(result.row(0))
          .containsEntry("artistid", 1)
          .containsEntry("name", "AC/DC");
    }

    @Test
    void validateRejectsMalformedSql() throws QueryEngineException {
      assertThat(engine.validate(new SqlQuery("SELECT FROM", Map.of()))).isFalse();
    }

    @Test
    void executeRejectsMalformedSql() {
      assertThatThrownBy(
              () ->
                  engine.execute(
                      new SqlQuery("SELECT FROM", Map.of()), QueryEngineContext.empty()))
          .isInstanceOf(QueryEngineException.class);
    }
  }

  @Nested
  @DisplayName("Staged optimization path")
  class StagedOptimizationPath {

    @Test
    void joinAndAggregationExecuteThroughOptimizedPipeline() throws QueryEngineException {
      final String sql =
          """
          SELECT a.name AS name, COUNT(al.albumid) AS albums
          FROM `chinook-db`.artist a
          JOIN `chinook-db`.album al ON a.artistid = al.artistid
          WHERE a.artistid = 1
          GROUP BY a.name
          """;

      final String plan = engine.explain(new SqlQuery(sql, Map.of()));
      assertThat(plan).isNotBlank();

      final QueryEngineResult result =
          engine.execute(new SqlQuery(sql, Map.of()), QueryEngineContext.empty());

      assertThat(result.rows().size()).isEqualTo(1);
      assertThat(result.row(0)).containsEntry("name", "AC/DC");
      assertThat(((Number) result.row(0).get("albums")).longValue()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void constantQueryReturnsMaterializedRows() throws QueryEngineException {
      final QueryEngineResult result =
          engine.execute(
              new SqlQuery("SELECT 42 AS answer, 'pipeline' AS stage", Map.of()),
              QueryEngineContext.empty());

      assertThat(result.rows().size()).isEqualTo(1);
      assertThat(result.row(0))
          .containsEntry("answer", 42)
          .containsEntry("stage", "pipeline");
    }
  }

  private void loadChinookSchema() throws Exception {
    try (InputStream schemaStream =
        getClass().getClassLoader().getResourceAsStream("chinook-schema.sql")) {
      if (schemaStream == null) {
        throw new IllegalStateException("chinook-schema.sql not found on test classpath");
      }
      final String schemaSql;
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(schemaStream, StandardCharsets.UTF_8))) {
        schemaSql =
            reader
                .lines()
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"));
      }
      try (Statement stmt = h2Connection.createStatement()) {
        for (final String statement : schemaSql.split(";")) {
          final String trimmed = statement.trim();
          if (!trimmed.isEmpty()) {
            stmt.execute(trimmed);
          }
        }
      }
    }
  }
}
