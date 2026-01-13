/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Generic thread-safe registry for managing named instances of a specific type.
 *
 * <p>
 * This class provides a consistent, thread-safe mechanism for registering, retrieving, and managing named instances. It
 * serves as a base implementation that can be used directly or extended for specific use cases.
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 * <li>Thread-safe operations using {@link ConcurrentHashMap}</li>
 * <li>Duplicate registration prevention</li>
 * <li>Optional resource cleanup on shutdown</li>
 * <li>Comprehensive logging</li>
 * <li>Null-safety with validation</li>
 * <li>Immutable snapshots of registry state</li>
 * </ul>
 *
 * <p>
 * <strong>Thread Safety:</strong> All operations are thread-safe and can be safely called from multiple threads
 * concurrently.
 * </p>
 *
 * <p>
 * <strong>Example Usage:</strong>
 * </p>
 *
 * <pre>{@code
 * // Create a registry for DataSource objects
 * Registry<DataSource> registry = new Registry<>("DataSource");
 *
 * // Register an instance
 * registry.register("primary-db", dataSource);
 *
 * // Retrieve an instance
 * DataSource ds = registry.get("primary-db");
 *
 * // Check existence
 * if (registry.contains("primary-db")) {
 *     // ...
 * }
 *
 * // Get all registered instances
 * Map<String, DataSource> all = registry.all();
 * }</pre>
 *
 * @param <T>
 *            the type of instances managed by this registry
 * @author Cheshire Framework
 * @since 1.0.0
 */
public class Registry<T> {

    private final Logger log;
    private final String registryType;
    private final Map<String, T> instances;
    private final Consumer<T> shutdownHandler;
    private volatile boolean shutdown = false;

    /**
     * Creates a new registry with the specified type name.
     *
     * @param registryType
     *            a descriptive name for this registry type (used in logging)
     * @throws IllegalArgumentException
     *             if registryType is null or blank
     */
    public Registry(String registryType) {
        this(registryType, null);
    }

    /**
     * Creates a new registry with the specified type name and shutdown handler.
     *
     * <p>
     * The shutdown handler is invoked for each registered instance when {@link #shutdown()} is called. This is useful
     * for registries managing resources that need cleanup (e.g., closing connections).
     * </p>
     *
     * @param registryType
     *            a descriptive name for this registry type (used in logging)
     * @param shutdownHandler
     *            optional handler for cleaning up instances on shutdown
     * @throws IllegalArgumentException
     *             if registryType is null or blank
     */
    public Registry(String registryType, Consumer<T> shutdownHandler) {
        if (registryType == null || registryType.isBlank()) {
            throw new IllegalArgumentException("Registry type cannot be null or blank");
        }
        this.registryType = registryType;
        this.log = LoggerFactory.getLogger(getClass().getName() + "." + registryType);
        this.instances = new ConcurrentHashMap<>();
        this.shutdownHandler = shutdownHandler;
    }

    /**
     * Registers an instance with the specified name.
     *
     * <p>
     * The name must be unique within this registry. If an instance with the same name is already registered, a
     * {@link RegistryException} is thrown.
     * </p>
     *
     * @param name
     *            the unique name for the instance, must not be null or blank
     * @param instance
     *            the instance to register, must not be null
     * @throws RegistryException
     *             if an instance with the same name is already registered or if the registry has been shutdown
     * @throws IllegalArgumentException
     *             if name is null/blank or instance is null
     */
    public void register(String name, T instance) {
        validateNotShutdown();
        validateName(name);
        Objects.requireNonNull(instance, "Instance cannot be null");

        T existing = instances.putIfAbsent(name, instance);
        if (existing != null) {
            String errorMsg = String.format("%s already registered with name '%s'. Existing instance: %s", registryType,
                    name, existing.getClass().getName());
            log.error(errorMsg);
            throw new RegistryException(errorMsg);
        }

        log.info("Registered {} '{}' (type: {})", registryType, name, instance.getClass().getSimpleName());
    }

    /**
     * Retrieves the instance registered with the given name.
     *
     * @param name
     *            the instance name, must not be null or blank
     * @return the registered instance, never null
     * @throws RegistryException
     *             if no instance is registered with the given name or if the registry has been shutdown
     * @throws IllegalArgumentException
     *             if name is null or blank
     */
    public T get(String name) {
        validateNotShutdown();
        validateName(name);

        T instance = instances.get(name);
        if (instance == null) {
            String errorMsg = String.format("No %s registered with name '%s'. Available: %s", registryType, name,
                    instances.keySet());
            log.error(errorMsg);
            throw new RegistryException(errorMsg);
        }

        log.trace("Retrieved {} '{}'", registryType, name);
        return instance;
    }

    /**
     * Retrieves the instance registered with the given name, or returns an empty Optional.
     *
     * <p>
     * This is a null-safe alternative to {@link #get(String)} that doesn't throw an exception when the instance is not
     * found.
     * </p>
     *
     * @param name
     *            the instance name, must not be null or blank
     * @return an Optional containing the instance if found, empty otherwise
     * @throws RegistryException
     *             if the registry has been shutdown
     * @throws IllegalArgumentException
     *             if name is null or blank
     */
    public Optional<T> find(String name) {
        validateNotShutdown();
        validateName(name);

        T instance = instances.get(name);
        if (instance != null) {
            log.trace("Found {} '{}'", registryType, name);
        } else {
            log.trace("No {} found with name '{}'", registryType, name);
        }
        return Optional.ofNullable(instance);
    }

    /**
     * Checks if an instance is registered with the given name.
     *
     * @param name
     *            the instance name, must not be null or blank
     * @return true if an instance is registered with the given name, false otherwise
     * @throws IllegalArgumentException
     *             if name is null or blank
     */
    public boolean contains(String name) {
        validateName(name);
        return instances.containsKey(name);
    }

    /**
     * Returns an immutable snapshot of all registered instances.
     *
     * <p>
     * The returned map is a snapshot of the registry at the time of the call. Changes to the registry after this call
     * will not be reflected in the returned map.
     * </p>
     *
     * @return an immutable map of names to instances
     * @throws RegistryException
     *             if the registry has been shutdown
     */
    public Map<String, T> all() {
        validateNotShutdown();
        Map<String, T> snapshot = Map.copyOf(instances);
        log.trace("Retrieved snapshot of all {} instances (count: {})", registryType, snapshot.size());
        return snapshot;
    }

    /**
     * Returns an immutable collection of all registered instance names.
     *
     * @return an immutable collection of registered names
     */
    public Collection<String> names() {
        return Map.copyOf(instances).keySet();
    }

    /**
     * Returns an immutable collection of all registered instances.
     *
     * @return an immutable collection of registered instances
     * @throws RegistryException
     *             if the registry has been shutdown
     */
    public Collection<T> values() {
        validateNotShutdown();
        return Map.copyOf(instances).values();
    }

    /**
     * Unregisters an instance by name.
     *
     * <p>
     * <strong>Note:</strong> This method does not invoke the shutdown handler on the removed instance. If cleanup is
     * needed, the caller must handle it.
     * </p>
     *
     * @param name
     *            the instance name, must not be null or blank
     * @return true if an instance was removed, false if no instance was registered
     * @throws RegistryException
     *             if the registry has been shutdown
     * @throws IllegalArgumentException
     *             if name is null or blank
     */
    public boolean unregister(String name) {
        validateNotShutdown();
        validateName(name);

        T removed = instances.remove(name);
        if (removed != null) {
            log.info("Unregistered {} '{}'", registryType, name);
            return true;
        } else {
            log.debug("No {} registered with name '{}' to unregister", registryType, name);
            return false;
        }
    }

    /**
     * Removes and returns the instance registered with the given name.
     *
     * <p>
     * This is useful when you need both to remove an instance and obtain it for cleanup purposes.
     * </p>
     *
     * @param name
     *            the instance name, must not be null or blank
     * @return an Optional containing the removed instance if found, empty otherwise
     * @throws RegistryException
     *             if the registry has been shutdown
     * @throws IllegalArgumentException
     *             if name is null or blank
     */
    public Optional<T> remove(String name) {
        validateNotShutdown();
        validateName(name);

        T removed = instances.remove(name);
        if (removed != null) {
            log.info("Removed {} '{}'", registryType, name);
        }
        return Optional.ofNullable(removed);
    }

    /**
     * Clears all registered instances.
     *
     * <p>
     * <strong>Warning:</strong> This operation removes all registered instances from the registry but does NOT invoke
     * the shutdown handler. Use {@link #shutdown()} if proper cleanup is needed.
     * </p>
     *
     * <p>
     * This is primarily useful for testing or application reload scenarios. Use with caution in production code.
     * </p>
     *
     * @throws RegistryException
     *             if the registry has been shutdown
     */
    public void clear() {
        validateNotShutdown();
        int count = instances.size();
        instances.clear();
        log.info("Cleared {} registry (removed {} instances)", registryType, count);
    }

    /**
     * Returns the number of registered instances.
     *
     * @return the number of registered instances
     */
    public int size() {
        return instances.size();
    }

    /**
     * Checks if the registry is empty.
     *
     * @return true if no instances are registered, false otherwise
     */
    public boolean isEmpty() {
        return instances.isEmpty();
    }

    /**
     * Checks if the registry has been shutdown.
     *
     * @return true if shutdown has been called, false otherwise
     */
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * Shuts down this registry and cleans up all registered instances.
     *
     * <p>
     * If a shutdown handler was provided during construction, it will be invoked for each registered instance. The
     * shutdown process is best-effort: if an exception occurs while processing an instance, it is logged but does not
     * prevent processing of other instances.
     * </p>
     *
     * <p>
     * After shutdown, the registry cannot be used for any operations except {@link #isShutdown()}.
     * </p>
     *
     * <p>
     * This method is idempotent: calling it multiple times has no additional effect.
     * </p>
     */
    public void shutdown() {
        if (shutdown) {
            log.debug("{} registry already shutdown", registryType);
            return;
        }

        log.info("Shutting down {} registry (instances: {})", registryType, instances.size());

        if (shutdownHandler != null) {
            instances.forEach((name, instance) -> {
                try {
                    log.debug("Shutting down {} '{}'", registryType, name);
                    shutdownHandler.accept(instance);
                } catch (Exception e) {
                    log.error("Error shutting down {} '{}': {}", registryType, name, e.getMessage(), e);
                }
            });
        }

        instances.clear();
        shutdown = true;
        log.info("{} registry shutdown complete", registryType);
    }

    /**
     * Returns a string representation of this registry.
     *
     * @return a string containing the registry type and registered names
     */
    @Override
    public String toString() {
        return String.format("%s{type='%s', instances=%s, shutdown=%s}", getClass().getSimpleName(), registryType,
                instances.keySet(), shutdown);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Instance name cannot be null or blank");
        }
    }

    private void validateNotShutdown() {
        if (shutdown) {
            throw new RegistryException(registryType + " registry has been shutdown");
        }
    }
}
