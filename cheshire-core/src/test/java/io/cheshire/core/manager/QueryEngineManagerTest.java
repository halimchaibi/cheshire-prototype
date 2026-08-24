/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cheshire.core.config.CheshireConfig;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QueryEngineManagerTest {

  @Test
  void initializeUsesFactoryFromEachEngineDefinition() throws Exception {
    CheshireConfig config = configWithSingleEngine();
    QueryEngineManager manager = newManager(config);

    manager.initialize();

    assertTrue(manager.contains("calcite"));
    assertEquals("calcite", manager.get("calcite").name());
    assertEquals(
        RecordingQueryEngineFactory.class.getName(),
        RecordingQueryEngineFactory.lastAdaptedConfig.get("factory"));
  }

  @Test
  void resolveSourcesCopiesDescriptionFromEngineDefinition() throws Exception {
    Map<String, Object> resolved = resolveSingleEngine();

    assertEquals("Calcite query engine", resolved.get("description"));
  }

  @Test
  void resolveSourcesKeepsResolvedSourceConfigByName() throws Exception {
    Map<String, Object> resolved = resolveSingleEngine();

    @SuppressWarnings("unchecked")
    Map<String, Object> sources = (Map<String, Object>) resolved.get("sources");

    assertTrue(sources.containsKey("chinook-db"));
    assertEquals(Map.of("schema", "PUBLIC"), sources.get("chinook-db"));
  }

  @Test
  void resolveSourcesFallsBackToSourceNamesInEngineConfig() throws Exception {
    CheshireConfig config = configWithNestedEngineSources();
    QueryEngineManager manager = newManager(config);

    Method resolveSources =
        QueryEngineManager.class.getDeclaredMethod("resolveSources", CheshireConfig.class);
    resolveSources.setAccessible(true);

    @SuppressWarnings("unchecked")
    Map<String, Object> all = (Map<String, Object>) resolveSources.invoke(manager, config);
    @SuppressWarnings("unchecked")
    Map<String, Object> resolved = (Map<String, Object>) all.get("calcite");
    @SuppressWarnings("unchecked")
    Map<String, Object> sources = (Map<String, Object>) resolved.get("sources");

    assertTrue(sources.containsKey("chinook-db"));
    assertEquals(Map.of("schema", "PUBLIC"), sources.get("chinook-db"));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> resolveSingleEngine() throws Exception {
    CheshireConfig config = configWithSingleEngine();
    QueryEngineManager manager = newManager(config);

    Method resolveSources =
        QueryEngineManager.class.getDeclaredMethod("resolveSources", CheshireConfig.class);
    resolveSources.setAccessible(true);
    Map<String, Object> all = (Map<String, Object>) resolveSources.invoke(manager, config);
    return (Map<String, Object>) all.get("calcite");
  }

  private static QueryEngineManager newManager(CheshireConfig config) throws Exception {
    Constructor<QueryEngineManager> constructor =
        QueryEngineManager.class.getDeclaredConstructor(CheshireConfig.class);
    constructor.setAccessible(true);
    return constructor.newInstance(config);
  }

  private static CheshireConfig configWithSingleEngine() {
    CheshireConfig.Source source = new CheshireConfig.Source();
    source.setName("chinook-db");
    source.setType("jdbc");
    source.setConfig(Map.of("schema", "PUBLIC"));

    CheshireConfig.QueryEngine engine = new CheshireConfig.QueryEngine();
    engine.setName("calcite");
    engine.setDescription("Calcite query engine");
    engine.setFactory(RecordingQueryEngineFactory.class.getName());
    engine.setSources(List.of("chinook-db"));
    engine.setConfig(Map.of("timeoutMs", 30_000));

    CheshireConfig config = new CheshireConfig();
    config.setSources(Map.of("chinook-db", source));
    config.setQueryEngines(Map.of("calcite", engine));
    return config;
  }

  private static CheshireConfig configWithNestedEngineSources() {
    CheshireConfig.Source source = new CheshireConfig.Source();
    source.setName("chinook-db");
    source.setType("jdbc");
    source.setConfig(Map.of("schema", "PUBLIC"));

    CheshireConfig.QueryEngine engine = new CheshireConfig.QueryEngine();
    engine.setName("calcite");
    engine.setDescription("Calcite query engine");
    engine.setFactory(RecordingQueryEngineFactory.class.getName());
    engine.setConfig(Map.of("sources", List.of("chinook-db"), "timeoutMs", 30_000));

    CheshireConfig config = new CheshireConfig();
    config.setSources(Map.of("chinook-db", source));
    config.setQueryEngines(Map.of("calcite", engine));
    return config;
  }
}
