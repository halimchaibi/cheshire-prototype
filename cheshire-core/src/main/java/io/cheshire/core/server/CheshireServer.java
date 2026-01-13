/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.server;

/**
 * Lifecycle contract for network servers exposing Cheshire capabilities.
 * <p>
 * <strong>Architectural Role:</strong>
 * <p>
 * A CheshireServer represents a running network service (HTTP server, stdio handler) that exposes one or more
 * capabilities to external clients. It manages the server lifecycle and coordinates with the {@link CheshireDispatcher}
 * for request processing.
 * <p>
 * <strong>Lifecycle States:</strong>
 *
 * <pre>
 * NEW → init() → INITIALIZED → start() → RUNNING → stop() → STOPPED
 * </pre>
 * <p>
 * <strong>Implementation Examples:</strong>
 * <ul>
 * <li><strong>JettyCheshireServer:</strong> HTTP/HTTPS server using Jetty</li>
 * <li><strong>StdioCheshireServer:</strong> Standard I/O handler for MCP stdio</li>
 * </ul>
 * <p>
 * <strong>Factory Pattern:</strong>
 * <p>
 * Servers are created via {@link CheshireServerFactory} implementations loaded through Java's SPI mechanism. The
 * factory class name is specified in the transport configuration.
 * <p>
 * <strong>Thread Safety:</strong>
 * <p>
 * Implementations must be thread-safe as lifecycle methods may be called from different threads during
 * startup/shutdown.
 *
 * @see CheshireServerFactory
 * @see CheshireDispatcher
 * @see io.cheshire.runtime.CheshireRuntime
 * @since 1.0.0
 */
public interface CheshireServer {

    /**
     * Initializes the server configuration without starting network listeners.
     * <p>
     * <strong>Responsibilities:</strong>
     * <ul>
     * <li>Validate configuration</li>
     * <li>Allocate resources (thread pools, buffers)</li>
     * <li>Prepare server components (handlers, filters)</li>
     * <li>Do NOT bind to ports or start accepting connections</li>
     * </ul>
     * <p>
     * <strong>Idempotency:</strong> Multiple calls should be safe (no-op if already initialized).
     *
     * @throws Exception
     *             if initialization fails (invalid config, resource allocation failure)
     */
    void init() throws Exception;

    /**
     * Starts the server and begins accepting client connections.
     * <p>
     * <strong>Responsibilities:</strong>
     * <ul>
     * <li>Bind to configured host/port (for network servers)</li>
     * <li>Mount capability routes/handlers</li>
     * <li>Start accepting connections</li>
     * <li>Transition to RUNNING state</li>
     * </ul>
     * <p>
     * <strong>Blocking Behavior:</strong> This method should return quickly. Long-running accept loops should run in
     * background threads.
     * <p>
     * <strong>Prerequisites:</strong> {@link #init()} must be called first.
     *
     * @throws Exception
     *             if server cannot start (port in use, permission denied)
     */
    void start() throws Exception;

    /**
     * Stops the server and releases all resources.
     * <p>
     * <strong>Responsibilities:</strong>
     * <ul>
     * <li>Stop accepting new connections</li>
     * <li>Complete or abort in-flight requests (graceful shutdown)</li>
     * <li>Unmount capability routes</li>
     * <li>Release network resources (close sockets, unbind ports)</li>
     * <li>Shutdown thread pools</li>
     * </ul>
     * <p>
     * <strong>Graceful Shutdown:</strong> Implementations should attempt graceful shutdown with a reasonable timeout,
     * then force termination if needed.
     * <p>
     * <strong>Idempotency:</strong> Multiple calls should be safe (no-op if already stopped).
     *
     * @throws Exception
     *             if shutdown encounters critical errors
     */
    void stop() throws Exception;

    /**
     * Returns the exposure type identifier for this server.
     * <p>
     * Used for logging, monitoring, and debugging to identify which protocol/exposure this server implements.
     * <p>
     * <strong>Examples:</strong> "REST-API", "MCP-STDIO", "MCP-HTTP"
     *
     * @return exposure type string
     */
    String type();

    /**
     * Checks if the server is currently running and accepting connections.
     * <p>
     * Returns {@code true} only after {@link #start()} succeeds and before {@link #stop()} is called.
     *
     * @return {@code true} if server is running, {@code false} otherwise
     */
    boolean isRunning();
}
