package io.cheshire.core.manager;

import io.cheshire.core.config.CheshireConfig;
import io.cheshire.core.registry.Registry;
import io.cheshire.core.registry.RegistryException;
import io.cheshire.spi.source.SourceConfig;
import io.cheshire.spi.source.SourceProvider;
import io.cheshire.spi.source.SourceProviderFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Manages source providers - the framework's data source abstraction layer.
 * <p>
 * <strong>What is a SourceProvider?</strong>
 * <p>
 * A source provider abstracts access to external data sources (databases, APIs,
 * file systems). It handles connections, authentication, and resource lifecycle.
 * Examples: JDBC connections, HTTP APIs, S3 buckets.
 * <p>
 * <strong>Initialization via SPI:</strong>
 * This manager uses Java's {@link ServiceLoader} SPI mechanism to discover
 * {@link SourceProviderFactory} implementations at runtime. Factories create
 * providers from configuration.
 * <p>
 * <strong>Process Flow:</strong>
 * <ol>
 *   <li><strong>Discovery:</strong> ServiceLoader finds all SourceProviderFactory implementations</li>
 *   <li><strong>Configuration:</strong> Each source definition in config is matched to a factory</li>
 *   <li><strong>Adaptation:</strong> Factory adapts YAML config to typed {@link SourceConfig}</li>
 *   <li><strong>Creation:</strong> Factory creates and initializes the provider</li>
 *   <li><strong>Registration:</strong> Provider registered by name for global access</li>
 * </ol>
 * <p>
 * <strong>Lifecycle Management:</strong>
 * Providers implement {@link AutoCloseable}. The manager ensures proper cleanup
 * during shutdown, closing database connections and releasing resources.
 * <p>
 * <strong>Singleton Pattern:</strong>
 * Atomic singleton ensures consistent global registry state.
 * <p>
 * <strong>Thread Safety:</strong>
 * All operations are thread-safe via underlying {@link Registry}.
 * <p>
 * <strong>Example Usage:</strong>
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
 * @see SourceConfig
 * @since 1.0.0
 */
@Slf4j
public final class SourceProviderManager implements Initializable {

    private static final AtomicReference<SourceProviderManager> INSTANCE = new AtomicReference<>();
    private final CheshireConfig config;

    private final Registry<SourceProvider<?, ?>> registry = new Registry<>(
            "SourceProvider",
            provider -> {
                try {
                    if (provider != null) {
                        ((AutoCloseable) provider).close();
                    }
                } catch (Exception e) {
                    log.error("Error closing SourceProvider: {}", e.getMessage(), e);
                }
            }
    );

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
     * <p>
     * First call must provide a non-null {@link CheshireConfig}. Subsequent
     * calls can pass {@code null} and will receive the existing instance.
     *
     * @param config configuration for first-time initialization, or {@code null}
     * @return the singleton SourceProviderManager instance
     * @throws IllegalArgumentException if first call provides null config
     */
    public static SourceProviderManager instance(CheshireConfig config) {
        INSTANCE.updateAndGet(existing -> {
            if (existing != null) return existing;
            if (config == null) throw new IllegalArgumentException("First call must provide config");
            return new SourceProviderManager(config);
        });
        return INSTANCE.get();

    }

    /**
     * Registers a source provider with the specified name.
     *
     * @param name     the unique name for the provider, must not be null or blank
     * @param provider the source provider instance, must not be null
     * @throws RegistryException        if a provider with the same name is already registered
     * @throws IllegalArgumentException if name is null/blank or provider is null
     */
    public void register(String name, SourceProvider<?, ?> provider) {
        registry.register(name, provider);
    }

    /**
     * Returns the provider registered with the given name.
     *
     * @param name the source name, must not be null or blank
     * @return the provider instance, never null
     * @throws RegistryException        if no provider is registered with the given name
     * @throws IllegalArgumentException if name is null or blank
     */
    public SourceProvider<?, ?> get(String name) {
        return registry.get(name);
    }

    /**
     * Returns an immutable snapshot of all registered providers.
     *
     * @return an immutable map of provider names to provider instances
     */
    public java.util.Map<String, SourceProvider<?, ?>> all() {
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
     * <p><strong>Warning:</strong> Use {@link #shutdown()} if proper cleanup is needed.</p>
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
     * <p>
     * <strong>SPI Discovery Process:</strong>
     * <ol>
     *   <li>Uses {@link ServiceLoader} to discover {@link SourceProviderFactory} implementations</li>
     *   <li>Caches factories by fully qualified class name</li>
     *   <li>For each source in config, resolves the appropriate factory</li>
     *   <li>Adapts YAML config to typed {@link SourceConfig}</li>
     *   <li>Creates provider via factory and registers it</li>
     * </ol>
     * <p>
     * <strong>Factory Resolution:</strong>
     * The config specifies the provider factory by class name. This approach
     * provides explicit control over which implementation handles each source.
     *
     * @throws IllegalStateException if no factory found for a configured source
     *                               or if provider creation fails
     */
    @Override
    public void initialize() {
        var sources = config.getSources();

        // Load factories via SPI
        ServiceLoader<SourceProviderFactory> loader =
                ServiceLoader.load(SourceProviderFactory.class, SourceProviderFactory.class.getClassLoader());

        // Cache factories by class name (stable, no guessing)
        Map<String, SourceProviderFactory> factories =
                loader.stream()
                        .map(ServiceLoader.Provider::get)
                        .peek(f -> log.info("Found SourceProvider: {}", f.getClass().getName()))
                        .collect(Collectors.toMap(
                                f -> f.getClass().getName(),
                                f -> f,
                                (a, b) -> a
                        ));

        // Instantiate engines from config
        sources.forEach((sourceName, sourceDef) -> {

            String factoryClass = config
                    .getSources()
                    .get(sourceName)
                    .getProvider();

            SourceProviderFactory factory = factories.get(factoryClass);

            if (factory == null) {
                throw new IllegalStateException(
                        "No SourceProviderFactory found for: " + factoryClass
                );
            }

            SourceConfig config =
                    factory.adapter().adapt(sourceName, sourceDef);

            try {
                SourceProvider<?, ?> source = factory.create(config);
                register(sourceName, source);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to initialize SourceProvider: " + sourceName, e
                );
            }
        });
    }

    /**
     * Shuts down all registered providers and clears the registry.
     *
     * <p>This method invokes cleanup on all providers (if they implement AutoCloseable)
     * and then removes them from the registry. After shutdown, the registry cannot be
     * used until the application is restarted.</p>
     */
    @Override
    public void shutdown() {
        registry.shutdown();
    }
}
