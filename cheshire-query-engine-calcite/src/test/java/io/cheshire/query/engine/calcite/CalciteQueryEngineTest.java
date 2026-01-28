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
import io.cheshire.query.engine.calcite.schema.SchemaManager;
import io.cheshire.spi.query.exception.QueryEngineException;
import java.lang.reflect.Field;
import java.util.Map;
import org.apache.calcite.tools.FrameworkConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
class CalciteQueryEngineTest {

  private static final Logger log = LoggerFactory.getLogger(CalciteQueryEngineTest.class);

  @Test
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

  @SuppressWarnings("unchecked")
  private static <T> T getPrivateField(Object target, String fieldName, Class<T> type)
      throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    Object value = field.get(target);
    return (T) value;
  }
}
