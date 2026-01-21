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

import io.cheshire.core.config.CheshireConfig;
import io.cheshire.core.registry.Registry;
import io.cheshire.core.registry.RegistryException;
import io.cheshire.spi.query.engine.QueryEngineFactory;
import io.cheshire.spi.source.SourceProvider;
import io.cheshire.spi.source.SourceProviderConfig;
import io.cheshire.spi.source.SourceProviderConfigAdapter;
import io.cheshire.spi.source.SourceProviderFactory;
import io.cheshire.spi.source.exception.SourceProviderException;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages source providers - the framework's data source abstraction layer.
 *
 * <p><strong>What is a SourceProvider?</strong>
 *
 * <p>A source provider abstracts access to external data sources (databases, APIs, file systems).
 * It handles connections, authentication, and resource lifecycle. Examples: JDBC connections, HTTP
 * APIs, S3 buckets.
 *
 * <p><strong>Initialization via SPI:</strong> This manager uses Java's {@link ServiceLoader} SPI
 * mechanism to discover {@link SourceProviderFactory} implementations at runtime. Factories create
 * providers from configuration.
 *
 * <p><strong>Process Flow:</strong>
 *
 * <ol>
 *   <li><strong>Discovery:</strong> ServiceLoader finds all SourceProviderFactory implementations
 *   <li><strong>Configuration:</strong> Each source definition in config is matched to a factory
 *   <li><strong>Adaptation:</strong> Factory adapts YAML config to typed {@link
 *       SourceProviderConfig}
 *   <li><strong>Creation:</strong> Factory creates and initializes the provider
 *   <li><strong>Registration:</strong> Provider registered by name for global access
 * </ol>
 *
 * <p><strong>Lifecycle Management:</strong> Providers implement {@link AutoCloseable}. The manager
 * ensures proper cleanup during shutdown, closing database connections and releasing resources.
 *
 * <p><strong>Singleton Pattern:</strong> Atomic singleton ensures consistent global registry state.
 *
 * <p><strong>Thread Safety:</strong> All operations are thread-safe via underlying {@link
 * Registry}.
 *
 * <p><strong>Example Usage:</strong>
 *
 * <pre>{@code
 * // Retrieve a registered provider
 * SourceProvider<?, ?> provider = sourceManager.get("blog-db");
 *
 * // Check availability
 * if (sourceManager.isRegistered("blog-db")) {
 *     // ...
 * }
 * }</pre>
 *
 * @see SourceProvider
 * @see SourceProviderFactory
 * @see SourceProviderConfig
 * @since 1.0.0
 */
@Slf4j
public final class SourceProviderManager implements Initializable {

  private static final AtomicReference<SourceProviderManager> INSTANCE = new AtomicReference<>();
  private final CheshireConfig config;

  private final Registry<SourceProvider<?>> registry =
      new Registry<>(
          "SourceProvider",
          provider -> {
            try {
              if (provider != null) {
                ((AutoCloseable) provider).close();
              }
            } catch (Exception e) {
              log.error("Error closing SourceProvider: {}", e.getMessage(), e);
            }
          });

  /**
   * Private constructor enforcing singleton pattern.
   *
   * @param config validated Cheshire configuration
   */
  private SourceProviderManager(CheshireConfig config) {
    this.config = config;
  }

  /**
   * Returns the singleton instance, initializing it if necessary.
   *
   * <p>First call must provide a non-null {@link CheshireConfig}. Subsequent calls can pass {@code
   * null} and will receive the existing instance.
   *
   * @param config configuration for first-time initialization, or {@code null}
   * @return the singleton SourceProviderManager instance
   * @throws IllegalArgumentException if first call provides null config
   */
  public static SourceProviderManager instance(CheshireConfig config) {
    INSTANCE.updateAndGet(
        existing -> {
          if (existing != null) return existing;
          if (config == null) throw new IllegalArgumentException("First call must provide config");
          return new SourceProviderManager(config);
        });
    return INSTANCE.get();
  }

  /**
   * Registers a source provider with the specified name.
   *
   * @param name the unique name for the provider, must not be null or blank
   * @param provider the source provider instance, must not be null
   * @throws RegistryException if a provider with the same name is already registered
   * @throws IllegalArgumentException if name is null/blank or provider is null
   */
  public void register(String name, SourceProvider<?> provider) {
    registry.register(name, provider);
  }

  /**
   * Returns the provider registered with the given name.
   *
   * @param name the source name, must not be null or blank
   * @return the provider instance, never null
   * @throws RegistryException if no provider is registered with the given name
   * @throws IllegalArgumentException if name is null or blank
   */
  public SourceProvider<?> get(String name) {
    return registry.get(name);
  }

  /**
   * Returns an immutable snapshot of all registered providers.
   *
   * @return an immutable map of provider names to provider instances
   */
  public java.util.Map<String, SourceProvider<?>> all() {
    return registry.all();
  }

  /**
   * Checks if a provider is registered with the given name.
   *
   * @param name the source name, must not be null or blank
   * @return true if a provider is registered with the given name, false otherwise
   * @throws IllegalArgumentException if name is null or blank
   */
  public boolean isRegistered(String name) {
    return registry.contains(name);
  }

  /**
   * Unregisters a provider by name.
   *
   * @param name the source name, must not be null or blank
   * @return true if a provider was removed, false if no provider was registered
   * @throws IllegalArgumentException if name is null or blank
   */
  public boolean unregister(String name) {
    return registry.unregister(name);
  }

  /**
   * Clears all registered providers without closing them.
   *
   * <p><strong>Warning:</strong> Use {@link #shutdown()} if proper cleanup is needed.
   */
  public void clear() {
    registry.clear();
  }

  /**
   * Returns the number of registered providers.
   *
   * @return the number of registered providers
   */
  public int size() {
    return registry.size();
  }

  /**
   * Initializes all source providers from configuration.
   *
   * <p><strong>SPI Discovery Process:</strong>
   *
   * <ol>
   *   <li>Uses {@link ServiceLoader} to discover {@link SourceProviderFactory} implementations
   *   <li>Caches factories by fully qualified class name
   *   <li>For each source in config, resolves the appropriate factory
   *   <li>Adapts YAML config to typed {@link SourceProviderConfig}
   *   <li>Creates provider via factory and registers it
   * </ol>
   *
   * <p><strong>Factory Resolution:</strong> The config specifies the provider factory by class
   * name. This approach provides explicit control over which implementation handles each source.
   *
   * @throws IllegalStateException if no factory found for a configured source or if provider
   *     creation fails
   */
  @Override
  public void initialize() {

    ServiceLoader<SourceProviderFactory> loader =
        ServiceLoader.load(SourceProviderFactory.class, QueryEngineFactory.class.getClassLoader());

    var factories =
        loader.stream()
            .map(ServiceLoader.Provider::get)
            .peek(f -> log.info("Found SourceProviderFactory: {}", f.getClass().getName()))
            .collect(Collectors.toMap(factory -> factory.getClass().getName(), factory -> factory));

    var sourceConfigs = config.getSources();
    sourceConfigs.forEach(
        (sourceName, sourceDef) -> {
          String factoryClass = sourceDef.getFactory();
          SourceProviderFactory<?> factory = factories.get(factoryClass);

          if (factory == null) {
            throw new IllegalStateException(
                "Factory "
                    + factoryClass
                    + " not found in classpath. "
                    + "Check META-INF/services or if the jar is included.");
          }

          try {
            SourceProvider<?> source = createAndValidate(factory, sourceDef);
            register(sourceName, source);
          } catch (SourceProviderException e) {
            throw new IllegalStateException(
                String.format(
                    "Failed to initialize SourceProvider '%s' using factory %s",
                    sourceName, factoryClass),
                e);
          }
        });
  }

  private <C extends SourceProviderConfig> SourceProvider<?> createAndValidate(
      SourceProviderFactory<C> factory, CheshireConfig.Source sourceDef)
      throws SourceProviderException {

    SourceProviderConfigAdapter<C> adapter = factory.adapter();

    C config = adapter.adapt(sourceDef.getConfig());

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

  /**
   * Shuts down all registered providers and clears the registry.
   *
   * <p>This method invokes cleanup on all providers (if they implement AutoCloseable) and then
   * removes them from the registry. After shutdown, the registry cannot be used until the
   * application is restarted.
   */
  @Override
  public void shutdown() {
    registry.shutdown();
  }
}
