package io.cheshire.core.server;

import io.cheshire.core.capability.Capability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory interface for creating {@link CheshireServer} instances.
 * <p>
 * <strong>SPI Pattern:</strong>
 * <p>
 * Implementations of this interface are discovered via Java's Service Provider Interface (SPI).
 * The factory class name is specified in the transport configuration, and the framework
 * dynamically loads and instantiates it.
 * <p>
 * <strong>Implementation Requirements:</strong>
 * <ul>
 *   <li>Must have a public no-argument constructor</li>
 *   <li>Must be registered in {@code META-INF/services/io.cheshire.core.server.CheshireServerFactory}</li>
 *   <li>Should be stateless (factory methods only)</li>
 * </ul>
 * <p>
 * <strong>Example Implementations:</strong>
 * <ul>
 *   <li><strong>JettyServerFactory:</strong> Creates Jetty-based HTTP servers</li>
 *   <li><strong>StdioServerFactory:</strong> Creates stdio-based MCP servers</li>
 * </ul>
 * <p>
 * <strong>Configuration Example:</strong>
 * <pre>{@code
 * transports:
 *   http-transport:
 *     factory: io.cheshire.server.jetty.JettyServerFactory
 *     port: 8080
 *     host: 0.0.0.0
 * }</pre>
 * <p>
 * <strong>Dynamic Loading:</strong>
 * <p>
 * The {@link #load(String)} static method provides reflection-based loading with
 * clear error messages for common failures (class not found, wrong type, instantiation errors).
 *
 * @see CheshireServer
 * @see io.cheshire.runtime.CheshireRuntime
 * @since 1.0.0
 */
public interface CheshireServerFactory {

    Logger log = LoggerFactory.getLogger(CheshireServerFactory.class);

    /**
     * Dynamically loads a CheshireServerFactory implementation by class name.
     * <p>
     * <strong>Loading Process:</strong>
     * <ol>
     *   <li>Load class via {@link Class#forName(String)}</li>
     *   <li>Verify it implements {@link CheshireServerFactory}</li>
     *   <li>Instantiate via no-arg constructor</li>
     *   <li>Return factory instance</li>
     * </ol>
     * <p>
     * <strong>Error Handling:</strong>
     * Provides clear error messages for common failures:
     * <ul>
     *   <li><strong>ClassNotFoundException:</strong> Factory not in classpath (missing dependency)</li>
     *   <li><strong>IllegalArgumentException:</strong> Class doesn't implement interface</li>
     *   <li><strong>Exception:</strong> Instantiation failed (no public no-arg constructor)</li>
     * </ul>
     *
     * @param className fully qualified class name of the factory implementation
     * @return instantiated factory instance
     * @throws RuntimeException if loading or instantiation fails
     */
    static CheshireServerFactory load(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!CheshireServerFactory.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException(
                        "Class " + className + " does not implement CheshireServerFactory");
            }
            return (CheshireServerFactory) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            log.error("Factory class not found in classpath: '{}'. Check your YAML config or dependencies.", className, e);
            throw new RuntimeException("Factory missing: " + className, e);
        } catch (Exception e) {
            log.error("Failed to instantiate factory '{}'. Ensure a public no-args constructor exists.", className, e);
            throw new RuntimeException("Instantiation failed: " + className, e);
        }
    }

    /**
     * Creates a CheshireServer instance for the given capability.
     * <p>
     * <strong>Factory Responsibilities:</strong>
     * <ul>
     *   <li>Extract transport configuration from capability</li>
     *   <li>Create and configure the underlying server (Jetty, Netty, etc.)</li>
     *   <li>Wire the dispatcher for request handling</li>
     *   <li>Return initialized (but not started) server</li>
     * </ul>
     * <p>
     * <strong>Server State:</strong>
     * The returned server should be in the NEW state, ready for {@link CheshireServer#init()}
     * and {@link CheshireServer#start()} calls.
     *
     * @param capability capability configuration with transport settings
     * @param binding    exposure binding string (e.g., "/api/v1", "mcp-stdio")
     * @param dispatcher dispatcher for routing and processing requests
     * @return initialized CheshireServer instance (not started)
     */
    CheshireServer create(Capability capability, String binding, CheshireDispatcher dispatcher);
}
