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

import io.cheshire.common.utils.LambdaUtils;
import io.cheshire.query.engine.calcite.config.CalciteQueryEngineConfig;
import io.cheshire.query.engine.calcite.optimizer.OptimizationContext;
import io.cheshire.query.engine.calcite.optimizer.QueryRuntimeContext;
import io.cheshire.query.engine.calcite.optimizer.RuleSetBuilder;
import io.cheshire.query.engine.calcite.optimizer.RuleSetManager;
import io.cheshire.query.engine.calcite.query.QueryCharacteristics;
import io.cheshire.query.engine.calcite.query.QueryType;
import io.cheshire.query.engine.calcite.query.SqlQuery;
import io.cheshire.query.engine.calcite.schema.SchemaManager;
import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.request.LogicalQuery;
import io.cheshire.spi.query.request.QueryEngineContext;
import io.cheshire.spi.query.result.QueryEngineResult;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Phase 1: basic instantiation tests for {@link CalciteQueryEngine}.
 *
 * <p>These tests intentionally do NOT call {@code open()}, {@code execute()}, or {@code explain()}.
 * They only verify that a new engine instance can be created from:
 *
 * <ul>
 *   <li>a {@link CalciteQueryEngineConfig}
 *   <li>the {@link CalciteQueryEngineFactory} + {@link CalciteQueryEngineConfigAdapter}
 * </ul>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CalciteQueryEngineTest {

  private static final Logger log = LoggerFactory.getLogger(CalciteQueryEngineTest.class);

  @Test
  @Order(1)
  @DisplayName("Phase 1: create CalciteQueryEngine directly from config")
  void canCreateEngineDirectlyFromConfig() {
    // Given
    String engineName = "calcite-direct";
    Map<String, Object> sources = Map.of(); // empty is fine for Phase 1
    Map<String, Object> config = Map.of(); // empty is fine for/init Phase 1

    CalciteQueryEngineConfig engineConfig =
        new CalciteQueryEngineConfig(engineName, sources, config);

    // When
    CalciteQueryEngine engine = new CalciteQueryEngine(engineConfig);

    // Then
    assertNotNull(engine, "Engine instance should not be null");
    log.info(
        "Created CalciteQueryEngine directly with name='{}', sources={}, config={}",
        engine.name(),
        sources,
        config);
    assertEquals(engineName, engine.name(), "Engine name should match config");
  }

  @Test
  @Order(2)
  @DisplayName("Phase 1: create CalciteQueryEngine via factory + adapter")
  void canCreateEngineViaFactoryAndAdapter() throws QueryEngineException {
    // Given: raw engine configuration as it would come from CheshireConfig
    Map<String, Object> engineConfigMap =
        Map.of("name", "calcite-factory", "sources", Map.of(), "config", Map.of());

    CalciteQueryEngineConfigAdapter adapter = new CalciteQueryEngineConfigAdapter();
    CalciteQueryEngineConfig engineConfig = adapter.adapt(engineConfigMap);

    CalciteQueryEngineFactory factory = new CalciteQueryEngineFactory();

    // When
    CalciteQueryEngine engine = factory.create(engineConfig);

    // Then
    assertNotNull(engine, "Engine instance created by factory should not be null");
    assertEquals("calcite-factory", engine.name(), "Engine name should match adapted config");
    assertEquals(
        CalciteQueryEngineConfig.class,
        factory.configType(),
        "Factory should advertise CalciteQueryEngineConfig as config type");

    log.info(
        "Created CalciteQueryEngine via factory with name='{}', rawConfig={}",
        engine.name(),
        engineConfigMap);
  }

  @Test
  @Order(3)
  @DisplayName(
      "Phase 1: instantiate CalciteQueryEngineConfig from Chinook-like template via adapter")
  void canInstantiateConfigFromChinookTemplateLikeConfig() throws QueryEngineException {
    // Given: a configuration map shaped like the calcite entry in app-template.yaml,
    // adapted for a Chinook database and normalized to what CalciteQueryEngineConfigAdapter
    // expects.
    //
    // In the YAML, the calcite engine section looks roughly like:
    //   query-engines:
    //     calcite:
    //       name: calcite
    //       sources: [ app-db ]
    //       config: { defaultLimit, maxLimit, timeoutMs }
    //
    // For the adapter we need:
    //   - name: String
    //   - sources: Map<String,Object>  (sourceName -> sourceConfig)
    //   - config: Map<String,Object>   (engine-specific config)

    Map<String, Object> chinookSourceConfig =
        Map.of(
            "type",
            "jdbc",
            "schema",
            "public",
            "connection",
            Map.of(
                "url", "jdbc:postgresql://localhost:5432/chinook",
                "username", "chinook_user",
                "password", "chinook_password",
                "driver", "org.postgresql.Driver"),
            "auto-discover-schema",
            true);

    Map<String, Object> engineConfigMap =
        Map.of(
            "name", "chinook-calcite",
            // single Chinook source, keyed by name
            "sources", Map.of("chinook-db", chinookSourceConfig),
            "config",
                Map.of(
                    "defaultLimit", 100,
                    "maxLimit", 10_000,
                    "timeoutMs", 30_000));

    CalciteQueryEngineConfigAdapter adapter = new CalciteQueryEngineConfigAdapter();

    // When
    CalciteQueryEngineConfig config = adapter.adapt(engineConfigMap);

    // Then: basic structural assertions
    assertNotNull(config, "Config should not be null");
    assertEquals("chinook-calcite", config.name(), "Engine name should match template");
    assertNotNull(config.sources(), "Sources map should not be null");
    assertEquals(1, config.sources().size(), "Exactly one source should be configured");
    assertNotNull(config.config(), "Engine config map should not be null");

    // Verify Chinook source wiring
    Map<String, Object> sources = config.sources();
    @SuppressWarnings("unchecked")
    Map<String, Object> resolvedSourceConfig = (Map<String, Object>) sources.get("chinook-db");
    assertNotNull(resolvedSourceConfig, "chinook-db source should be present");
    assertEquals("jdbc", resolvedSourceConfig.get("type"), "Source type should be jdbc");

    // Verify we can pass the config into the factory and create an engine
    CalciteQueryEngineFactory factory = new CalciteQueryEngineFactory();
    CalciteQueryEngine engine = factory.create(config);
    assertNotNull(engine, "Engine created from Chinook-like config should not be null");
    assertEquals("chinook-calcite", engine.name(), "Engine name should propagate from config");

    log.info(
        "Created CalciteQueryEngine for Chinook with name='{}', sources={}, config={}",
        engine.name(),
        config.sources(),
        config.config());
  }

  @Test
  @Order(5)
  @DisplayName(
      "Phase 3 - Stage 1: PARSE stage parses a simple SQL into a SqlNode using planner + stage()")
  void parseStageParsesSqlUsingPlannerAndStage() throws Exception {
    // Given: an engine initialized with an in-memory H2 Chinook-style source (same as Phase 2)
    Map<String, Object> h2ConnectionConfig =
        Map.of(
            "url",
            "jdbc:h2:mem:chinook_parse;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "driver",
            "org.h2.Driver",
            "username",
            "sa",
            "password",
            "");

    Map<String, Object> h2InnerConfig =
        Map.of("type", "jdbc", "schema", "public", "connection", h2ConnectionConfig);

    Map<String, Object> sourceEntry =
        Map.of("name", "chinook-db", "type", "jdbc", "config", h2InnerConfig);

    Map<String, Object> sources = Map.of("chinook-db", sourceEntry);

    CalciteQueryEngineConfig engineConfig =
        new CalciteQueryEngineConfig("calcite-parse", sources, Map.of());

    CalciteQueryEngine engine = new CalciteQueryEngine(engineConfig);
    engine.open();

    // And a simple SQL query
    String sql = "SELECT 1 AS x";
    log.info("Phase 3.1: Starting PARSE stage with SQL: {}", sql);
    LogicalQuery logicalQuery = new SqlQuery(sql, Map.of());
    QueryEngineContext queryContext = QueryEngineContext.empty();

    // Build query-scoped planner (mirroring CalciteQueryEngine.execute())
    Planner planner = buildQueryScopedPlanner(engine, logicalQuery, queryContext);

    // When: invoke the private stage(ExecutionStage.PARSE, supplier) method via reflection
    Method stageMethod =
        CalciteQueryEngine.class.getDeclaredMethod(
            "stage", ExecutionStage.class, LambdaUtils.CheckedSupplier.class);
    stageMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    SqlNode parsed =
        (SqlNode)
            stageMethod.invoke(
                engine,
                ExecutionStage.PARSE,
                (LambdaUtils.CheckedSupplier<SqlNode>) () -> planner.parse(sql));

    // Then
    assertNotNull(parsed, "Parsed SqlNode should not be null");
    log.info("Phase 3.1: PARSE stage produced SqlNode:");
    log.info("  - Type: {}", parsed.getClass().getName());
    log.info("  - SqlNode: {}", parsed);
    log.info("  - toString: {}", parsed.toString());
    // Cleanup
    engine.close();
  }

  @Test
  @Order(6)
  @DisplayName(
      "Phase 3 - Stage 2: VALIDATE stage validates a parsed SqlNode using planner + stage()")
  void validateStageValidatesParsedSqlUsingPlannerAndStage() throws Exception {
    // Given: an engine initialized with an in-memory H2 Chinook-style source (same setup as PARSE)
    Map<String, Object> h2ConnectionConfig =
        Map.of(
            "url",
            "jdbc:h2:mem:chinook_validate;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "driver",
            "org.h2.Driver",
            "username",
            "sa",
            "password",
            "");

    Map<String, Object> h2InnerConfig =
        Map.of("type", "jdbc", "schema", "public", "connection", h2ConnectionConfig);

    Map<String, Object> sourceEntry =
        Map.of("name", "chinook-db", "type", "jdbc", "config", h2InnerConfig);

    Map<String, Object> sources = Map.of("chinook-db", sourceEntry);

    CalciteQueryEngineConfig engineConfig =
        new CalciteQueryEngineConfig("calcite-validate", sources, Map.of());

    CalciteQueryEngine engine = new CalciteQueryEngine(engineConfig);
    engine.open();

    String sql = "SELECT 1 AS x";
    log.info("Phase 3.2: Starting VALIDATE stage with SQL: {}", sql);
    LogicalQuery logicalQuery = new SqlQuery(sql, Map.of());
    QueryEngineContext queryContext = QueryEngineContext.empty();

    // Build query-scoped planner
    Planner planner = buildQueryScopedPlanner(engine, logicalQuery, queryContext);

    // First parse (we can use planner directly here)
    SqlNode parsed = planner.parse(sql);
    assertNotNull(parsed, "Parsed SqlNode should not be null before VALIDATE stage");
    log.info("  - Parsed SqlNode (input): {}", parsed);

    // Invoke private stage(ExecutionStage.VALIDATE, supplier) via reflection
    Method stageMethod =
        CalciteQueryEngine.class.getDeclaredMethod(
            "stage", ExecutionStage.class, LambdaUtils.CheckedSupplier.class);
    stageMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    SqlNode validated =
        (SqlNode)
            stageMethod.invoke(
                engine,
                ExecutionStage.VALIDATE,
                (LambdaUtils.CheckedSupplier<SqlNode>) () -> planner.validate(parsed));

    assertNotNull(validated, "Validated SqlNode should not be null");
    log.info("Phase 3.2: VALIDATE stage produced SqlNode:");
    log.info("  - Type: {}", validated.getClass().getName());
    log.info("  - SqlNode: {}", validated);
    log.info("  - toString: {}", validated.toString());

    engine.close();
  }

  @Test
  @Order(7)
  @DisplayName(
      "Phase 3 - Stage 3: CONVERT stage (now merged into OPTIMIZE) converts validated SqlNode to RelNode")
  void convertStageConvertsValidatedSqlToRelNodeUsingStage() throws Exception {
    // Note: CONVERT is now merged into OPTIMIZE via planner.rel(validated).rel
    // This test verifies the conversion happens as part of the OPTIMIZE stage
    // Given: an engine initialized with an in-memory H2 Chinook-style source
    Map<String, Object> h2ConnectionConfig =
        Map.of(
            "url",
            "jdbc:h2:mem:chinook_convert;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "driver",
            "org.h2.Driver",
            "username",
            "sa",
            "password",
            "");

    Map<String, Object> h2InnerConfig =
        Map.of("type", "jdbc", "schema", "public", "connection", h2ConnectionConfig);

    Map<String, Object> sourceEntry =
        Map.of("name", "chinook-db", "type", "jdbc", "config", h2InnerConfig);

    Map<String, Object> sources = Map.of("chinook-db", sourceEntry);

    CalciteQueryEngineConfig engineConfig =
        new CalciteQueryEngineConfig("calcite-convert", sources, Map.of());

    CalciteQueryEngine engine = new CalciteQueryEngine(engineConfig);
    engine.open();

    String sql = "SELECT 1 AS x";
    log.info("Phase 3.3: Starting CONVERT stage with SQL: {}", sql);
    LogicalQuery logicalQuery = new SqlQuery(sql, Map.of());
    QueryEngineContext queryContext = QueryEngineContext.empty();

    // Build query-scoped planner
    Planner planner = buildQueryScopedPlanner(engine, logicalQuery, queryContext);

    // PARSE and VALIDATE via planner (mirroring execute() flow)
    SqlNode parsed = planner.parse(sql);
    assertNotNull(parsed, "Parsed SqlNode should not be null before CONVERT stage");
    log.info("  - Parsed SqlNode (input): {}", parsed);

    SqlNode validated = planner.validate(parsed);
    assertNotNull(validated, "Validated SqlNode should not be null before CONVERT stage");
    log.info("  - Validated SqlNode (input): {}", validated);

    // Invoke private stage(ExecutionStage.CONVERT, supplier) via reflection.
    // Note: planner.rel(validated).rel performs both conversion (SqlNode -> RelNode) and
    // optimization
    Method stageMethod =
        CalciteQueryEngine.class.getDeclaredMethod(
            "stage", ExecutionStage.class, LambdaUtils.CheckedSupplier.class);
    stageMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    org.apache.calcite.rel.RelNode logicalPlan =
        (org.apache.calcite.rel.RelNode)
            stageMethod.invoke(
                engine,
                ExecutionStage.CONVERT,
                (LambdaUtils.CheckedSupplier<org.apache.calcite.rel.RelNode>)
                    () -> planner.rel(validated).rel);

    assertNotNull(logicalPlan, "Logical RelNode plan should not be null");
    log.info("Phase 3.3: CONVERT stage (via planner.rel) produced RelNode:");
    log.info("  - Type: {}", logicalPlan.getClass().getName());
    log.info("  - RelNode: {}", logicalPlan);
    log.info("  - toString: {}", logicalPlan.toString());
    log.info("  - Description: {}", logicalPlan.getDescription());
    if (logicalPlan.getRowType() != null) {
      log.info("  - RowType: {}", logicalPlan.getRowType());
    }

    engine.close();
  }

  @Test
  @Order(8)
  @DisplayName(
      "Phase 3 - Stage 4: OPTIMIZE stage optimizes validated SqlNode using planner.rel() + stage()")
  void optimizeStageOptimizesLogicalPlanUsingStage() throws Exception {
    // Given: engine initialized with an in-memory H2 source
    Map<String, Object> h2ConnectionConfig =
        Map.of(
            "url",
            "jdbc:h2:mem:chinook_optimize;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "driver",
            "org.h2.Driver",
            "username",
            "sa",
            "password",
            "");

    Map<String, Object> h2InnerConfig =
        Map.of("type", "jdbc", "schema", "public", "connection", h2ConnectionConfig);

    Map<String, Object> sourceEntry =
        Map.of("name", "chinook-db", "type", "jdbc", "config", h2InnerConfig);

    Map<String, Object> sources = Map.of("chinook-db", sourceEntry);

    CalciteQueryEngineConfig engineConfig =
        new CalciteQueryEngineConfig("calcite-optimize", sources, Map.of());

    CalciteQueryEngine engine = new CalciteQueryEngine(engineConfig);
    engine.open();

    String sql = "SELECT 1 AS x";
    LogicalQuery logicalQuery = new SqlQuery(sql, Map.of());
    QueryEngineContext queryContext = QueryEngineContext.empty();

    // Build query-scoped planner (includes optimization rules)
    Planner planner = buildQueryScopedPlanner(engine, logicalQuery, queryContext);

    // PARSE and VALIDATE
    SqlNode parsed = planner.parse(sql);
    SqlNode validated = planner.validate(parsed);

    // Invoke private stage(ExecutionStage.OPTIMIZE, supplier) via reflection.
    // Note: planner.rel(validated).rel performs both conversion (SqlNode -> RelNode) and
    // optimization
    Method stageMethod =
        CalciteQueryEngine.class.getDeclaredMethod(
            "stage", ExecutionStage.class, LambdaUtils.CheckedSupplier.class);
    stageMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    org.apache.calcite.rel.RelNode optimizedPlan =
        (org.apache.calcite.rel.RelNode)
            stageMethod.invoke(
                engine,
                ExecutionStage.OPTIMIZE,
                (LambdaUtils.CheckedSupplier<org.apache.calcite.rel.RelNode>)
                    () -> planner.rel(validated).rel);

    assertNotNull(optimizedPlan, "Optimized RelNode plan should not be null");
    log.info("Phase 3.4: OPTIMIZE stage produced optimized RelNode:");
    log.info("  - Type: {}", optimizedPlan.getClass().getName());
    log.info("  - RelNode: {}", optimizedPlan);
    log.info("  - toString: {}", optimizedPlan.toString());
    log.info("  - Description: {}", optimizedPlan.getDescription());
    if (optimizedPlan.getRowType() != null) {
      log.info("  - RowType: {}", optimizedPlan.getRowType());
    }
    if (optimizedPlan.getTraitSet() != null) {
      log.info("  - TraitSet: {}", optimizedPlan.getTraitSet());
    }

    engine.close();
  }

  @Test
  @Order(9)
  @DisplayName(
      "Phase 3 - Stage 5: EXECUTE stage executes optimized plan into a ResultSet using stage()")
  void executeStageExecutesOptimizedPlanUsingStage() throws Exception {
    // Given: engine + optimized plan
    Map<String, Object> h2ConnectionConfig =
        Map.of(
            "url",
            "jdbc:h2:mem:chinook_execute;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "driver",
            "org.h2.Driver",
            "username",
            "sa",
            "password",
            "");

    Map<String, Object> h2InnerConfig =
        Map.of("type", "jdbc", "schema", "public", "connection", h2ConnectionConfig);

    Map<String, Object> sourceEntry =
        Map.of("name", "chinook-db", "type", "jdbc", "config", h2InnerConfig);

    Map<String, Object> sources = Map.of("chinook-db", sourceEntry);

    CalciteQueryEngineConfig engineConfig =
        new CalciteQueryEngineConfig("calcite-execute", sources, Map.of());

    CalciteQueryEngine engine = new CalciteQueryEngine(engineConfig);
    engine.open();

    String sql = "SELECT 1 AS x";
    log.info("Phase 3.5: Starting EXECUTE stage with SQL: {}", sql);
    LogicalQuery logicalQuery = new SqlQuery(sql, Map.of());
    QueryEngineContext queryContext = QueryEngineContext.empty();

    // Build query-scoped planner and get optimized plan
    Planner planner = buildQueryScopedPlanner(engine, logicalQuery, queryContext);
    SqlNode parsed = planner.parse(sql);
    SqlNode validated = planner.validate(parsed);
    org.apache.calcite.rel.RelNode optimizedPlan = planner.rel(validated).rel;
    log.info("  - Optimized RelNode (input): {}", optimizedPlan);

    // Get executor
    io.cheshire.query.engine.calcite.executor.QueryExecutor executor =
        getPrivateField(
            engine, "executor", io.cheshire.query.engine.calcite.executor.QueryExecutor.class);

    Method stageMethod =
        CalciteQueryEngine.class.getDeclaredMethod(
            "stage", ExecutionStage.class, LambdaUtils.CheckedSupplier.class);
    stageMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    ResultSet resultSet =
        (ResultSet)
            stageMethod.invoke(
                engine,
                ExecutionStage.EXECUTE,
                (LambdaUtils.CheckedSupplier<ResultSet>) () -> executor.execute(optimizedPlan));

    assertNotNull(resultSet, "EXECUTE stage should produce a non-null ResultSet");
    log.info("Phase 3.5: EXECUTE stage produced ResultSet:");
    log.info("  - Type: {}", resultSet.getClass().getName());
    try {
      java.sql.ResultSetMetaData metaData = resultSet.getMetaData();
      if (metaData != null) {
        log.info("  - ColumnCount: {}", metaData.getColumnCount());
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
          log.info(
              "    Column {}: {} ({})",
              i,
              metaData.getColumnName(i),
              metaData.getColumnTypeName(i));
        }
      }
      if (resultSet.next()) {
        log.info("  - First row retrieved successfully");
        resultSet.beforeFirst(); // Reset for potential reuse
      }
    } catch (Exception e) {
      log.debug("Could not extract ResultSet metadata", e);
    }

    resultSet.close();
    engine.close();
  }

  @Test
  @Order(10)
  @DisplayName(
      "Phase 3 - Stage 6: TRANSFORM stage converts ResultSet into QueryEngineResult using stage()")
  void transformStageTransformsResultSetUsingStage() throws Exception {
    // Given: engine + ResultSet from executing an optimized plan
    Map<String, Object> h2ConnectionConfig =
        Map.of(
            "url",
            "jdbc:h2:mem:chinook_transform;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "driver",
            "org.h2.Driver",
            "username",
            "sa",
            "password",
            "");

    Map<String, Object> h2InnerConfig =
        Map.of("type", "jdbc", "schema", "public", "connection", h2ConnectionConfig);

    Map<String, Object> sourceEntry =
        Map.of("name", "chinook-db", "type", "jdbc", "config", h2InnerConfig);

    Map<String, Object> sources = Map.of("chinook-db", sourceEntry);

    CalciteQueryEngineConfig engineConfig =
        new CalciteQueryEngineConfig("calcite-transform", sources, Map.of());

    CalciteQueryEngine engine = new CalciteQueryEngine(engineConfig);
    engine.open();

    String sql = "SELECT 1 AS x";
    log.info("Phase 3.6: Starting TRANSFORM stage with SQL: {}", sql);
    LogicalQuery logicalQuery = new SqlQuery(sql, Map.of());
    QueryEngineContext queryContext = QueryEngineContext.empty();

    // Build query-scoped planner and get optimized plan
    Planner planner = buildQueryScopedPlanner(engine, logicalQuery, queryContext);
    SqlNode parsed = planner.parse(sql);
    SqlNode validated = planner.validate(parsed);
    org.apache.calcite.rel.RelNode optimizedPlan = planner.rel(validated).rel;
    log.info("  - Optimized RelNode (input): {}", optimizedPlan);

    // Execute to get ResultSet
    io.cheshire.query.engine.calcite.executor.QueryExecutor executor =
        getPrivateField(
            engine, "executor", io.cheshire.query.engine.calcite.executor.QueryExecutor.class);
    ResultSet resultSet = executor.execute(optimizedPlan);
    log.info("  - ResultSet (input): {}", resultSet);

    Method stageMethod =
        CalciteQueryEngine.class.getDeclaredMethod(
            "stage", ExecutionStage.class, LambdaUtils.CheckedSupplier.class);
    stageMethod.setAccessible(true);

    io.cheshire.query.engine.calcite.transformer.ResultTransformer transformer =
        getPrivateField(
            engine,
            "resultTransformer",
            io.cheshire.query.engine.calcite.transformer.ResultTransformer.class);

    @SuppressWarnings("unchecked")
    QueryEngineResult result =
        (QueryEngineResult)
            stageMethod.invoke(
                engine,
                ExecutionStage.TRANSFORM,
                (LambdaUtils.CheckedSupplier<QueryEngineResult>)
                    () -> transformer.transform(resultSet));

    assertNotNull(result, "TRANSFORM stage should produce a non-null QueryEngineResult");
    assertEquals(1, result.rowCount(), "Expected exactly one row from SELECT 1 AS x");
    log.info("Phase 3.6: TRANSFORM stage produced QueryEngineResult:");
    log.info("  - Type: {}", result.getClass().getName());
    log.info("  - RowCount: {}", result.rowCount());
    if (result.columns() != null) {
      log.info("  - Columns: {}", result.columns());
    }
    if (result.rows() != null && !result.rows().isEmpty()) {
      log.info("  - First row: {}", result.rows().get(0));
    }
    log.info("  - toString: {}", result.toString());

    resultSet.close();
    engine.close();
  }

  @Test
  @Order(4)
  @DisplayName(
      "Phase 2: open() initializes SchemaManager and FrameworkConfig with H2 Chinook source")
  void openInitializesSchemaManagerAndFrameworkConfig() throws Exception {
    // Given: a CalciteQueryEngineConfig whose sources map matches what SchemaManager and
    // CalciteSchemaAdapter expect.
    //
    // Structure expected by SchemaManager.registerSchemas():
    //   sources:
    //     chinook-db:
    //       name: chinook-db
    //       type: jdbc
    //       config:
    //         type: jdbc
    //         schema: public
    //         connection:
    //           url: jdbc:h2:mem:chinook;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    //           driver: org.h2.Driver
    //           username: sa
    //           password: ""
    //
    // JdbcAdapter will translate this into a Calcite JdbcSchema. H2 runs in-memory, so no external
    // DB is required.

    Map<String, Object> h2ConnectionConfig =
        Map.of(
            "url",
            "jdbc:h2:mem:chinook;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "driver",
            "org.h2.Driver",
            "username",
            "sa",
            "password",
            "");

    Map<String, Object> h2InnerConfig =
        Map.of("type", "jdbc", "schema", "public", "connection", h2ConnectionConfig);

    Map<String, Object> sourceEntry =
        Map.of("name", "chinook-db", "type", "jdbc", "config", h2InnerConfig);

    Map<String, Object> sources = Map.of("chinook-db", sourceEntry);

    CalciteQueryEngineConfig engineConfig =
        new CalciteQueryEngineConfig("calcite-init", sources, Map.of());

    CalciteQueryEngine engine = new CalciteQueryEngine(engineConfig);

    // When
    engine.open();

    // Then: engine should report open
    assertTrue(engine.isOpen(), "Engine should be open after calling open()");

    // Use reflection to inspect private fields for Phase 2 (we only care about initialization)
    SchemaManager schemaManager = getPrivateField(engine, "schemaManager", SchemaManager.class);
    assertNotNull(schemaManager, "SchemaManager should be initialized");
    assertNotNull(schemaManager.rootSchema(), "SchemaManager.rootSchema() should not be null");

    FrameworkConfig frameworkConfig =
        getPrivateField(engine, "frameworkConfig", FrameworkConfig.class);
    assertNotNull(frameworkConfig, "FrameworkConfig should be initialized");
    assertNotNull(
        frameworkConfig.getDefaultSchema(), "FrameworkConfig.defaultSchema should not be null");

    // The default schema should be the same root schema registered by SchemaManager
    assertEquals(
        schemaManager.rootSchema(),
        frameworkConfig.getDefaultSchema(),
        "Default schema in FrameworkConfig should be SchemaManager.rootSchema()");

    log.info(
        "Phase 2: open() initialized SchemaManager and FrameworkConfig for engine '{}'",
        engine.name());

    // Cleanup
    engine.close();
  }

  /**
   * Helper method to build a query-scoped planner, mirroring CalciteQueryEngine.execute(). This
   * creates a planner with query-specific rules and context.
   */
  private Planner buildQueryScopedPlanner(
      CalciteQueryEngine engine, LogicalQuery logicalQuery, QueryEngineContext queryContext)
      throws Exception {
    // Get base config and schema manager
    FrameworkConfig baseConfig = getPrivateField(engine, "frameworkConfig", FrameworkConfig.class);
    SchemaManager schemaManager = getPrivateField(engine, "schemaManager", SchemaManager.class);
    CalciteQueryEngineConfig calciteConfig =
        getPrivateField(engine, "calciteConfig", CalciteQueryEngineConfig.class);

    // Build QueryRuntimeContext
    QueryRuntimeContext runtimeContext =
        QueryRuntimeContext.fromQuery(logicalQuery, queryContext).build();

    // Build OptimizationContext (simplified for tests)
    OptimizationContext optimizationContext =
        OptimizationContext.builder()
            .withQueryType(QueryType.OLTP)
            .withSchemas(schemaManager.schemas())
            .withCharacteristics(
                QueryCharacteristics.builder()
                    .withJoins(false)
                    .withAggregations(false)
                    .withTableCount(1)
                    .build())
            .build();

    // Extract source names (similar to CalciteQueryEngine.extractSourceNames())
    List<String> sourceNames = new java.util.ArrayList<>();
    if (queryContext != null && queryContext.sources() != null) {
      for (io.cheshire.spi.source.SourceProvider<?> provider : queryContext.sources()) {
        try {
          String name = provider.name();
          if (name != null && !name.isBlank()) {
            sourceNames.add(name);
          }
        } catch (Exception e) {
          // Ignore
        }
      }
    }
    if (sourceNames.isEmpty() && calciteConfig.sources() != null) {
      sourceNames.addAll(calciteConfig.sources().keySet());
    }

    // Build RuleSetManager
    RuleSetManager ruleSet =
        RuleSetBuilder.forSources(sourceNames)
            .withSchemaManager(schemaManager)
            .withOptimizationContext(optimizationContext)
            .build();

    // Build query-scoped FrameworkConfig
    FrameworkConfig queryConfig =
        FrameworkInitializer.builder()
            .withSchemaManager(schemaManager)
            .buildQueryConfig(baseConfig, runtimeContext, ruleSet);

    // Create and return planner
    return Frameworks.getPlanner(queryConfig);
  }

  @SuppressWarnings("unchecked")
  private static <T> T getPrivateField(Object target, String fieldName, Class<T> type)
      throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    Object value = field.get(target);
    return (T) value;
  }
}
