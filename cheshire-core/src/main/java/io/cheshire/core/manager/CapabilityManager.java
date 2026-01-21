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

import io.cheshire.common.exception.CheshireException;
import io.cheshire.core.capability.Capability;
import io.cheshire.core.config.CheshireConfig;
import io.cheshire.core.pipeline.PipelineFactory;
import io.cheshire.core.registry.Registry;
import io.cheshire.core.registry.RegistryException;
import io.cheshire.spi.pipeline.*;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages the registration, initialization, and lifecycle of business capabilities.
 *
 * <p><strong>What is a Capability?</strong>
 *
 * <p>A capability is a self-contained business domain that federates data sources, actions
 * (tools/endpoints), and processing pipelines. Examples: "Authors", "Articles", "Comments".
 *
 * <p><strong>Initialization Process:</strong>
 *
 * <ol>
 *   <li>Loads capability definitions from {@link CheshireConfig}
 *   <li>Builds pipelines using {@link PipelineFactory}
 *   <li>Resolves exposure (REST/MCP) and transport (HTTP/stdio) configurations
 *   <li>Registers fully initialized {@link Capability} instances
 * </ol>
 *
 * <p><strong>Registry Pattern:</strong> This manager acts as a centralized registry allowing lookup
 * of capabilities by name. It's used throughout the framework to:
 *
 * <ul>
 *   <li>Route incoming requests to the correct capability
 *   <li>Execute actions within the capability's context
 *   <li>Manage capability lifecycle (startup/shutdown)
 * </ul>
 *
 * <p><strong>Singleton Behavior:</strong> Implemented as an atomic singleton to ensure consistent
 * global state across the application. First initialization requires {@link CheshireConfig}.
 *
 * <p><strong>Thread Safety:</strong> All operations are thread-safe via underlying {@link
 * Registry}.
 *
 * @see Capability
 * @see PipelineFactory
 * @see CheshireConfig
 * @since 1.0.0
 */
@Slf4j
public final class CapabilityManager implements Initializable {

  private static final AtomicReference<CapabilityManager> INSTANCE = new AtomicReference<>();
  private final CheshireConfig config;

  /**
   * Private constructor enforcing singleton pattern.
   *
   * @param config validated Cheshire configuration
   */
  private CapabilityManager(CheshireConfig config) {
    this.config = config;
  }

  /**
   * Returns the singleton instance, initializing it if necessary.
   *
   * <p>First call must provide a non-null {@link CheshireConfig}. Subsequent calls can pass {@code
   * null} and will receive the existing instance.
   *
   * @param config configuration for first-time initialization, or {@code null}
   * @return the singleton CapabilityManager instance
   * @throws IllegalArgumentException if first call provides null config
   */
  public static CapabilityManager instance(CheshireConfig config) {
    INSTANCE.updateAndGet(
        existing -> {
          if (existing != null) return existing;
          if (config == null) throw new IllegalArgumentException("First call must provide config");
          return new CapabilityManager(config);
        });
    return INSTANCE.get();
  }

  /**
   * Registers a capability.
   *
   * @param capability the capability to register, must not be null
   * @throws RegistryException if a capability with the same name is already registered
   * @throws IllegalArgumentException if capability is null
   */
  public void register(Capability capability) {
    if (capability == null) {
      throw new IllegalArgumentException("Capability cannot be null");
    }
    registry.register(capability.name(), capability);
  }

  private final Registry<Capability> registry =
      new Registry<>(
          "Capability",
          capability -> {
            try {
              clear();
              log.info("Shutdown {}", capability.name());
            } catch (Exception e) {
              log.error(
                  "Failed to shutdown capability '{}' during registry cleanup",
                  capability.name(),
                  e);
            }
          });

  /**
   * Retrieves a capability by name.
   *
   * @param name the capability name, must not be null or blank
   * @return the capability instance, never null
   * @throws RegistryException if no capability is registered with the given name
   * @throws IllegalArgumentException if name is null or blank
   */
  public Capability get(String name) {
    return registry.get(name);
  }

  /**
   * Checks if a capability is registered with the given name.
   *
   * @param name the capability name, must not be null or blank
   * @return true if a capability is registered, false otherwise
   * @throws IllegalArgumentException if name is null or blank
   */
  public boolean contains(String name) {
    return registry.contains(name);
  }

  /**
   * Returns an immutable collection of all registered capabilities.
   *
   * @return an immutable collection of capabilities
   */
  public Collection<Capability> all() {
    return registry.values();
  }

  /** Clears all registered capabilities. */
  public void clear() {
    registry.clear();
  }

  /**
   * Initializes all capabilities from configuration.
   *
   * <p><strong>Process:</strong>
   *
   * <ol>
   *   <li>Iterates over each capability definition in config
   *   <li>Builds pipeline processors for all actions
   *   <li>Resolves exposure/transport configurations
   *   <li>Creates and registers the {@link Capability}
   * </ol>
   *
   * <p><strong>Note:</strong> Contains a TODO for improving source and query engine reference
   * resolution.
   *
   * @throws IllegalStateException if pipeline construction fails
   */
  @Override
  public void initialize() {
    var capabilities = config.getCapabilities();

    capabilities.forEach(
        (capabilityName, def) -> {
          // TODO: Resolve source and query engine references properly.
          Capability capability =
              new Capability(
                  capabilityName,
                  def.getDescription(),
                  def.getDomain(),
                  resolveExposure(def),
                  resolveTransport(def),
                  def.getSources(),
                  def.getQueryEngine(),
                  resolvePipelines(def),
                  def.getActions());

          register(capability);
          log.info("Registered capability: {}", capability.name());
        });
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

  // Helper methods for resolving configurations

  private CheshireConfig.Transport resolveTransport(CheshireConfig.Capability def) {
    return resolveTo(
        config.getTransports(),
        def.getTransport(),
        CheshireConfig.Transport.class,
        () -> {
          log.warn("No transport configuration found for: {}. Using default.", def.getTransport());
          return new CheshireConfig.Transport();
        });
  }

  private Map<String, PipelineProcessor<CanonicalInput<?>, CanonicalOutput<?>>> resolvePipelines(
      CheshireConfig.Capability def) {
    try {
      return def.getPipelines().entrySet().stream()
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey,
                  e -> {
                    try {
                      return PipelineFactory.build(e.getKey(), e.getValue());
                    } catch (ClassNotFoundException ex) {
                      // We catch it inside the stream to provide context
                      log.error(
                          "Pipeline '{}' in capability '{}' refers to a missing class: {}",
                          e.getKey(),
                          def.getName(),
                          ex.getMessage());
                      throw new CheshireException(
                          "Critical configuration error: Class not found", ex);
                    }
                  }));
    } catch (IllegalArgumentException e) {
      log.error("Failed to resolve transport for capability '{}'", def.getName());
      throw new CheshireException("Failed to resolve transport for capability", e);
    }
  }

  private CheshireConfig.Exposure resolveExposure(CheshireConfig.Capability def) {
    return resolveTo(
        config.getExposures(),
        def.getExposure(),
        CheshireConfig.Exposure.class,
        () -> {
          throw new CheshireException(
              "Missing required exposure configuration for: " + def.getExposure());
        });
  }

  /**
   * Resolves a configuration value from a source map with type safety and defaults.
   *
   * <p>This utility method safely extracts typed values from configuration maps, providing clear
   * error messages when types don't match.
   *
   * @param source the source map containing configuration entries
   * @param key the configuration key to resolve
   * @param type the expected type of the value
   * @param fallbackHandler fallback supplier to create the value if key is absent
   * @param <T> the expected value type
   * @return the resolved value or default
   * @throws IllegalArgumentException if value exists but is wrong type
   */
  private static <T> T resolveTo(
      Map<String, ?> source, String key, Class<T> type, Supplier<T> fallbackHandler) {
    Object value = source.get(key);

    if (value == null) {
      return fallbackHandler != null ? fallbackHandler.get() : null;
    }

    if (!type.isInstance(value)) {
      throw new IllegalArgumentException(
          "Config key '%s' must be of type %s but was %s"
              .formatted(key, type.getSimpleName(), value.getClass().getSimpleName()));
    }
    return type.cast(value);
  }
}
