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

import static org.junit.jupiter.api.Assertions.*;

import io.cheshire.query.engine.calcite.config.CalciteQueryEngineConfig;
import io.cheshire.query.engine.calcite.query.SqlQuery;
import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.request.LogicalQuery;
import io.cheshire.spi.query.request.QueryEngineContext;
import io.cheshire.spi.query.result.QueryEngineResult;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for CalciteQueryEngine using Chinook database with H2.
 *
 * <p>This test class:
 *
 * <ul>
 *   <li>Sets up an in-memory H2 database with Chinook schema and sample data
 *   <li>Initializes CalciteQueryEngine with the H2 connection
 *   <li>Executes various SQL queries against Chinook tables
 *   <li>Verifies query results match expected data
 * </ul>
 */
class CalciteQueryEngineChinookTest {

  private static final Logger log = LoggerFactory.getLogger(CalciteQueryEngineChinookTest.class);
  private static final String DB_URL =
      "jdbc:h2:mem:chinook_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
  private static final String DB_USER = "sa";
  private static final String DB_PASSWORD = "";

  private Connection h2Connection;
  private CalciteQueryEngine engine;

  @BeforeEach
  void setUp() throws Exception {
    log.info("Setting up H2 database with Chinook schema...");

    // Initialize H2 database connection
    h2Connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    try (Statement stmt = h2Connection.createStatement()) {
      stmt.execute("DROP ALL OBJECTS");
    }

    // Load and execute Chinook schema SQL
    try (InputStream schemaStream =
        getClass().getClassLoader().getResourceAsStream("chinook-schema.sql")) {
      if (schemaStream == null) {
        throw new IllegalStateException("chinook-schema.sql not found in test resources");
      }

      String schemaSql;
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(schemaStream, StandardCharsets.UTF_8))) {
        schemaSql =
            reader
                .lines()
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"));
      }

      // Split by semicolon and execute each statement
      try (Statement stmt = h2Connection.createStatement()) {
        String[] statements = schemaSql.split(";");
        for (String statement : statements) {
          String trimmed = statement.trim();
          if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
            try {
              stmt.execute(trimmed);
              log.debug(
                  "Executed SQL statement: {}",
                  trimmed.substring(0, Math.min(50, trimmed.length())));
            } catch (Exception e) {
              log.warn("Failed to execute statement (may be expected): {}", e.getMessage());
            }
          }
        }
      }

      log.info("Chinook schema loaded successfully");
    }

    // Verify schema was created by checking table count
    try (Statement stmt = h2Connection.createStatement();
        ResultSet rs =
            stmt.executeQuery(
                "SELECT COUNT(*) as table_count FROM INFORMATION_SCHEMA.TABLES "
                    + "WHERE LOWER(TABLE_SCHEMA) = 'public'")) {
      if (rs.next()) {
        int tableCount = rs.getInt("table_count");
        log.info("Database initialized with {} tables", tableCount);
        assertTrue(tableCount > 0, "At least one table should exist");
      }
    }

    // Configure CalciteQueryEngine with H2 connection
    Map<String, Object> h2ConnectionConfig =
        Map.of(
            "url", DB_URL,
            "driver", "org.h2.Driver",
            "username", DB_USER,
            "password", DB_PASSWORD);

    Map<String, Object> h2SourceConfig =
        Map.of(
            "name", "chinook-db",
            "type", "jdbc",
            "schema", "public",
            "config",
                Map.of(
                    "type",
                    "jdbc",
                    "schema",
                    "public",
                    "connection",
                    h2ConnectionConfig,
                    "auto-discover-schema",
                    true));

    Map<String, Object> sources = Map.of("chinook-db", h2SourceConfig);

    CalciteQueryEngineConfig engineConfig =
        new CalciteQueryEngineConfig("chinook-calcite", sources, Map.of());

    engine = new CalciteQueryEngine(engineConfig);
    engine.open();

    log.info("CalciteQueryEngine initialized and opened successfully");
  }

  @AfterEach
  void tearDown() throws Exception {
    if (engine != null) {
      try {
        engine.close();
        log.info("CalciteQueryEngine closed");
      } catch (Exception e) {
        log.warn("Error closing engine: {}", e.getMessage());
      }
    }

    if (h2Connection != null && !h2Connection.isClosed()) {
      try {
        h2Connection.close();
        log.info("H2 connection closed");
      } catch (Exception e) {
        log.warn("Error closing H2 connection: {}", e.getMessage());
      }
    }
  }

  @Test
  @DisplayName("Query: SELECT simple constant")
  void testSimpleConstantQuery() throws QueryEngineException {
    // Given
    String sql = "SELECT 1 AS x, 'Hello' AS greeting";
    LogicalQuery logicalQuery = new SqlQuery(sql, Map.of());
    QueryEngineContext context = QueryEngineContext.empty();

    log.info("Executing query: {}", sql);

    // When
    QueryEngineResult result = engine.execute(logicalQuery, context);

    // Then
    assertNotNull(result, "Result should not be null");
    assertEquals(1, result.rows().size(), "Should return exactly one row");
    assertEquals(2, result.columns().size(), "Should return exactly two columns");

    Map<String, Object> row = result.rows().get(0);
    assertEquals(1, row.get("x"), "First column should be 1");
    assertEquals("Hello", row.get("greeting"), "Second column should be 'Hello'");

    log.info("Query result: {}", result);
  }

  @Test
  @DisplayName("Query: SELECT all artists")
  void testSelectAllArtists() throws QueryEngineException {
    // Given
    String sql = "SELECT artistid, name FROM `chinook-db`.artist ORDER BY artistid";
    LogicalQuery logicalQuery = new SqlQuery(sql, Map.of());
    QueryEngineContext context = QueryEngineContext.empty();

    log.info("Executing query: {}", sql);

    // When
    QueryEngineResult result = engine.execute(logicalQuery, context);

    // Then
    assertNotNull(result, "Result should not be null");
    assertTrue(result.rows().size() >= 5, "Should return at least 5 artists");
    assertEquals(2, result.columns().size(), "Should return exactly two columns");

    // Verify first artist
    Map<String, Object> firstRow = result.rows().get(0);
    assertEquals(1, firstRow.get("artistid"), "First artist ID should be 1");
    assertEquals("AC/DC", firstRow.get("name"), "First artist name should be AC/DC");

    log.info("Query returned {} artists", result.rows().size());
    log.info("First artist: {}", firstRow);
  }

  @Test
  @DisplayName("Query: SELECT artists with WHERE clause")
  void testSelectArtistsWithWhere() throws QueryEngineException {
    // Given
    String sql = "SELECT artistid, name FROM `chinook-db`.artist WHERE artistid = 1";
    LogicalQuery logicalQuery = new SqlQuery(sql, Map.of());
    QueryEngineContext context = QueryEngineContext.empty();

    log.info("Executing query: {}", sql);

    // When
    QueryEngineResult result = engine.execute(logicalQuery, context);

    // Then
    assertNotNull(result, "Result should not be null");
    assertEquals(1, result.rows().size(), "Should return exactly one row");
    assertEquals(2, result.columns().size(), "Should return exactly two columns");

    Map<String, Object> row = result.rows().get(0);
    assertEquals(1, row.get("artistid"), "Artist ID should be 1");
    assertEquals("AC/DC", row.get("name"), "Artist name should be AC/DC");

    log.info("Query result: {}", result);
  }

  @Test
  @DisplayName("Query: SELECT with JOIN (Artists and Albums)")
  void testSelectWithJoin() throws QueryEngineException {
    // Given
    String sql =
        "SELECT a.artistid, a.name AS artistname, al.albumid, al.title AS albumtitle "
            + "FROM `chinook-db`.artist a "
            + "JOIN `chinook-db`.album al ON a.artistid = al.artistid "
            + "WHERE a.artistid = 1 "
            + "ORDER BY al.albumid";
    LogicalQuery logicalQuery = new SqlQuery(sql, Map.of());
    QueryEngineContext context = QueryEngineContext.empty();

    log.info("Executing query: {}", sql);

    // When
    QueryEngineResult result = engine.execute(logicalQuery, context);

    // Then
    assertNotNull(result, "Result should not be null");
    assertTrue(result.rows().size() >= 1, "Should return at least one album for AC/DC");
    assertEquals(4, result.columns().size(), "Should return exactly four columns");

    // Verify first result
    Map<String, Object> firstRow = result.rows().get(0);
    assertEquals(1, firstRow.get("artistid"), "Artist ID should be 1");
    assertEquals("AC/DC", firstRow.get("artistname"), "Artist name should be AC/DC");
    assertNotNull(firstRow.get("albumid"), "Album ID should not be null");
    assertNotNull(firstRow.get("albumtitle"), "Album title should not be null");

    log.info("Query returned {} albums for AC/DC", result.rows().size());
    log.info("First result: {}", firstRow);
  }

  @Test
  @DisplayName("Query: SELECT with aggregation (COUNT tracks per album)")
  void testSelectWithAggregation() throws QueryEngineException {
    // Given
    String sql =
        "SELECT al.albumid, al.title, COUNT(t.trackid) AS trackcount "
            + "FROM `chinook-db`.album al "
            + "LEFT JOIN `chinook-db`.track t ON al.albumid = t.albumid "
            + "GROUP BY al.albumid, al.title "
            + "ORDER BY trackcount DESC, al.albumid";
    LogicalQuery logicalQuery = new SqlQuery(sql, Map.of());
    QueryEngineContext context = QueryEngineContext.empty();

    log.info("Executing query: {}", sql);

    // When
    QueryEngineResult result = engine.execute(logicalQuery, context);

    // Then
    assertNotNull(result, "Result should not be null");
    assertTrue(result.rows().size() >= 1, "Should return at least one album");
    assertEquals(3, result.columns().size(), "Should return exactly three columns");

    // Verify aggregation column exists
    assertTrue(
        result.columns().stream()
            .map(QueryEngineResult.Column::name)
            .anyMatch(name -> name.equals("trackcount")),
        "Result should contain TrackCount column");

    log.info("Query returned {} albums with track counts", result.rows().size());
    if (!result.rows().isEmpty()) {
      log.info("First result: {}", result.rows().get(0));
    }
  }

  @Test
  @DisplayName("Query: SELECT with ORDER BY and LIMIT")
  void testSelectWithOrderByAndLimit() throws QueryEngineException {
    // Given
    String sql =
        "SELECT trackid, name, milliseconds FROM `chinook-db`.track "
            + "ORDER BY milliseconds DESC "
            + "LIMIT 5";
    LogicalQuery logicalQuery = new SqlQuery(sql, Map.of());
    QueryEngineContext context = QueryEngineContext.empty();

    log.info("Executing query: {}", sql);

    // When
    QueryEngineResult result = engine.execute(logicalQuery, context);

    // Then
    assertNotNull(result, "Result should not be null");
    assertTrue(result.rows().size() <= 5, "Should return at most 5 rows");
    assertEquals(3, result.columns().size(), "Should return exactly three columns");

    // Verify ordering (if multiple rows)
    if (result.rows().size() > 1) {
      for (int i = 0; i < result.rows().size() - 1; i++) {
        Map<String, Object> current = result.rows().get(i);
        Map<String, Object> next = result.rows().get(i + 1);
        Integer currentMs = (Integer) current.get("milliseconds");
        Integer nextMs = (Integer) next.get("milliseconds");
        assertTrue(
            currentMs >= nextMs,
            "Results should be ordered by Milliseconds DESC: " + currentMs + " >= " + nextMs);
      }
    }

    log.info("Query returned {} tracks", result.rows().size());
    if (!result.rows().isEmpty()) {
      log.info("Longest track: {}", result.rows().get(0));
    }
  }

  @Test
  @DisplayName("Query: SELECT with multiple tables and complex WHERE")
  void testSelectWithComplexWhere() throws QueryEngineException {
    // Given
    String sql =
        "SELECT t.trackid, t.name AS trackname, t.milliseconds, "
            + "a.name AS artistname, al.title AS albumtitle "
            + "FROM `chinook-db`.track t "
            + "JOIN `chinook-db`.album al ON t.albumid = al.albumid "
            + "JOIN `chinook-db`.artist a ON al.artistid = a.artistid "
            + "WHERE a.artistid = 1 AND t.milliseconds > 200000 "
            + "ORDER BY t.milliseconds DESC";
    LogicalQuery logicalQuery = new SqlQuery(sql, Map.of());
    QueryEngineContext context = QueryEngineContext.empty();

    log.info("Executing query: {}", sql);

    // When
    QueryEngineResult result = engine.execute(logicalQuery, context);

    // Then
    assertNotNull(result, "Result should not be null");
    assertTrue(result.rows().size() >= 1, "Should return at least one track");
    assertEquals(5, result.columns().size(), "Should return exactly five columns");

    // Verify all tracks are from AC/DC and have duration > 200000ms
    for (Map<String, Object> row : result.rows()) {
      assertEquals("AC/DC", row.get("artistname"), "All tracks should be from AC/DC");
      Integer milliseconds = (Integer) row.get("milliseconds");
      assertTrue(
          milliseconds > 200000,
          "All tracks should have duration > 200000ms, got: " + milliseconds);
    }

    log.info("Query returned {} tracks from AC/DC with duration > 200s", result.rows().size());
    if (!result.rows().isEmpty()) {
      log.info("First result: {}", result.rows().get(0));
    }
  }
}
