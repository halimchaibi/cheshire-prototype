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

import io.cheshire.common.utils.MapUtils;
import io.cheshire.common.utils.ObjectUtils;
import io.cheshire.common.utils.ServiceUtils;
import io.cheshire.core.config.CheshireConfig;
import io.cheshire.core.registry.Registry;
import io.cheshire.core.registry.RegistryException;
import io.cheshire.spi.query.engine.QueryEngine;
import io.cheshire.spi.query.engine.QueryEngineConfig;
import io.cheshire.spi.query.engine.QueryEngineConfigAdapter;
import io.cheshire.spi.query.engine.QueryEngineFactory;
import io.cheshire.spi.query.exception.QueryEngineException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages query engines - the framework's query execution abstraction layer.
 *
 * <p><strong>What is a QueryEngine?</strong>
 *
 * <p>A query engine processes structured queries against data sources. It abstracts the execution
 * model, allowing different implementations (JDBC, Calcite, custom DSLs) to be used
 * interchangeably.
 *
 * <p><strong>Query Engine Types:</strong>
 *
 * <ul>
 *   <li><strong>JDBC:</strong> Direct SQL execution against relational databases
 *   <li><strong>Calcite:</strong> Advanced query optimization and federation
 *   <li><strong>Custom:</strong> Domain-specific query processors
 * </ul>
 *
 * <p><strong>Initialization via SPI:</strong> Uses Java's {@link ServiceLoader} to discover {@link
 * QueryEngineFactory} implementations. Factories construct engines from configuration.
 *
 * <p><strong>Process Flow:</strong>
 *
 * <ol>
 *   <li><strong>Discovery:</strong> ServiceLoader finds all QueryEngineFactory implementations
 *   <li><strong>Configuration:</strong> Each engine definition in config matched to a factory
 *   <li><strong>Adaptation:</strong> Factory adapts YAML config to typed {@link QueryEngineConfig}
 *   <li><strong>Creation:</strong> Factory creates and initializes the engine
 *   <li><strong>Registration:</strong> Engine registered by name for lookup
 * </ol>
 *
 * <p><strong>Lifecycle Management:</strong> Engines implement {@link AutoCloseable}. Manager
 * ensures proper cleanup during shutdown, releasing connections and clearing caches.
 *
 * <p><strong>Singleton Pattern:</strong> Atomic singleton ensures consistent global registry state.
 *
 * <p><strong>Thread Safety:</strong> All operations thread-safe via underlying {@link Registry}.
 *
 * @see QueryEngine
 * @see QueryEngineFactory
 * @see QueryEngineConfig
 * @since 1.0.0
 */
@Slf4j
public final class QueryEngineManager implements Initializable {

  private static final AtomicReference<QueryEngineManager> INSTANCE = new AtomicReference<>();

  private final CheshireConfig config;

  private final Registry<QueryEngine<?>> registry =
      new Registry<>(
          "QueryEngine",
          engine -> {
            try {
              if (engine != null) {
                engine.close();
              }
            } catch (Exception e) {
              // Logged by Registry's shutdown handler
            }
          });

  /**
   * Private constructor enforcing singleton pattern.
   *
   * @param config validated Cheshire configuration
   */
  private QueryEngineManager(CheshireConfig config) {
    this.config = config;
  }

  /**
   * Returns the singleton instance, initializing it if necessary.
   *
   * <p>First call must provide a non-null {@link CheshireConfig}. Subsequent calls can pass {@code
   * null} and will receive the existing instance.
   *
   * @param config configuration for first-time initialization, or {@code null}
   * @return the singleton QueryEngineManager instance
   * @throws IllegalArgumentException if first call provides null config
   */
  public static QueryEngineManager instance(CheshireConfig config) {
    INSTANCE.updateAndGet(
        existing -> {
          if (existing != null) return existing;
          if (config == null) throw new IllegalArgumentException("First call must provide config");
          return new QueryEngineManager(config);
        });
    return INSTANCE.get();
  }

  /**
   * Registers a query engine under a logical name.
   *
   * @param name the engine name, must not be null or blank
   * @param engine the query engine instance, must not be null
   * @throws RegistryException if an engine with the same name is already registered
   * @throws IllegalArgumentException if name is null/blank or engine is null
   */
  public void register(String name, QueryEngine<?> engine) {
    registry.register(name, engine);
  }

  /**
   * Retrieves a query engine by name.
   *
   * @param name the engine name, must not be null or blank
   * @return the query engine instance, never null
   * @throws RegistryException if no engine is registered with the given name
   * @throws IllegalArgumentException if name is null or blank
   */
  public QueryEngine<?> get(String name) {
    return registry.get(name);
  }

  /**
   * Checks if an engine is registered with the given name.
   *
   * @param name the engine name, must not be null or blank
   * @return true if an engine is registered, false otherwise
   * @throws IllegalArgumentException if name is null or blank
   */
  public boolean contains(String name) {
    return registry.contains(name);
  }

  /**
   * Returns an immutable map of all registered engines.
   *
   * @return an immutable map of engine names to engines
   */
  public Map<String, QueryEngine<?>> all() {
    return registry.all();
  }

  /**
   * Clears all engines without closing them.
   *
   * <p><strong>Warning:</strong> Use {@link #shutdown()} for proper cleanup.
   */
  public void clear() {
    registry.clear();
  }

  /**
   * Initializes all query engines from configuration.
   *
   * <p><strong>SPI Discovery Process:</strong>
   *
   * <ol>
   *   <li>Uses {@link ServiceLoader} to discover {@link QueryEngineFactory} implementations
   *   <li>Caches factories by fully qualified class name
   *   <li>For each engine in config, resolves the appropriate factory
   *   <li>Adapts YAML config to typed {@link QueryEngineConfig}
   *   <li>Creates engine via factory and registers it
   * </ol>
   *
   * <p><strong>Factory Resolution:</strong> Config specifies the engine factory by class name,
   * providing explicit control over which implementation processes queries.
   *
   * @throws IllegalStateException if no factory found for a configured engine or if engine creation
   *     fails
   */
  @Override
  public void initialize() {

    var engines = resolveSources(config);
    var factories = ServiceUtils.loadAll(QueryEngineFactory.class);

    engines.forEach(
        (name, engineDef) -> {
          try {
            String factoryClass =
                MapUtils.someValueFromMapAs(engines, "factory", String.class)
                    .orElseThrow(
                        () ->
                            new IllegalStateException(
                                "No factory setting specified for engine: " + name));

            QueryEngineFactory<?> factory =
                Optional.ofNullable(factories.get(factoryClass))
                    .orElseThrow(
                        () ->
                            new IllegalStateException(
                                "No QueryEngineFactory found for: " + factoryClass));

            @SuppressWarnings("unchecked")
            Map<String, Object> engineConfig =
                ObjectUtils.someObjectAs(engineDef, Map.class)
                    .orElseThrow(() -> new IllegalStateException("Engine config must be a map"));

            QueryEngine<?> engine = createAndValidate(factory, engineConfig);
            register(engine.name(), engine);

          } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize QueryEngine: " + name, e);
          }
        });
  }

  private <C extends QueryEngineConfig> QueryEngine<?> createAndValidate(
      QueryEngineFactory<C> factory, Map<String, Object> engine) throws QueryEngineException {

    QueryEngineConfigAdapter<C> adapter = factory.adapter();

    C config = adapter.adapt(engine);

    if (!factory.configType().isInstance(config)) {
      throw new IllegalStateException(
          "Adapter produced "
              + config.getClass().getName()
              + " but factory expected "
              + factory.configType().getName());
    }

    factory.validate(config);
    return factory.create(config);
  }

  private Map<String, Object> resolveSources(CheshireConfig config) {
    var engines = config.getQueryEngines();
    var sources = config.getSources();
    Map<String, Object> result = new HashMap<>();

    engines.forEach(
        (engineName, engine) -> {
          Map<String, Object> engineMap = new HashMap<>(engine.getConfig());

          engineMap.put("name", engine.getName());
          engineMap.put("factory", engine.getFactory());
          engineMap.put("description", engine.getFactory());

          Map<String, Object> resolvedSources =
              engine.getSources().stream()
                  .filter(sources::containsKey)
                  .collect(
                      Collectors.toMap(
                          sourceName -> sourceName,
                          sourceName -> sources.get(sourceName).getConfig()));

          engineMap.put("sources", resolvedSources);
          engineMap.put("config", engine.getConfig());

          result.put(engineName, engineMap);
        });

    return Collections.unmodifiableMap(result);
  }

  /**
   * Shuts down all registered engines and clears the registry.
   *
   * <p>This method closes all engines (best-effort) and then removes them from the registry.
   * Exceptions during shutdown are logged but do not prevent other engines from being closed.
   */
  @Override
  public void shutdown() {
    registry.shutdown();
  }
}
