/*-
 * #%L
 * Cheshire :: Servers
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.jetty;

import io.cheshire.core.capability.Capability;
import io.cheshire.core.server.CheshireTransport;
import io.cheshire.stdio.StdioServerContainer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing shared transport containers across capabilities.
 * <p>
 * <strong>Purpose:</strong>
 * <p>
 * Provides singleton transport instances keyed by port number, enabling multiple capabilities to share a single HTTP
 * server (Jetty instance) when configured with the same port.
 * <p>
 * <strong>Transport Sharing Strategy:</strong>
 * <ul>
 * <li><strong>HTTP/Jetty:</strong> One container per unique port number (shareable)</li>
 * <li><strong>Stdio:</strong> Dedicated instance per capability (non-shareable)</li>
 * </ul>
 * <p>
 * <strong>Example Sharing:</strong>
 *
 * <pre>{@code
 * # Configuration:
 * capability-1: port 8080, path /api/v1/blog
 * capability-2: port 8080, path /api/v1/users
 * capability-3: port 8081, path /api/v1/admin
 *
 * # Result:
 * - JettyContainer(8080) serves both capability-1 and capability-2
 * - JettyContainer(8081) serves capability-3
 * }</pre>
 * <p>
 * <strong>Thread Safety:</strong>
 * <p>
 * Uses {@link ConcurrentHashMap#computeIfAbsent} for atomic get-or-create operations. Safe for concurrent capability
 * initialization.
 * <p>
 * <strong>Lifecycle:</strong>
 * <p>
 * Containers are created lazily on first access and shared across capabilities. Reference counting in
 * {@link JettyServerContainer} manages actual shutdown.
 *
 * @see JettyServerContainer
 * @see StdioServerContainer
 * @see CheshireTransport
 * @since 1.0.0
 */
@Slf4j
public class ServerRegistry {

    private static final Map<Integer, CheshireTransport> ENGINES = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation.
     */
    private ServerRegistry() {
        // Hidden constructor
    }

    /**
     * Returns the transport for a given capability, creating it if necessary.
     * <p>
     * Creates {@link JettyServerContainer} for network bindings (HTTP) keyed by port, or dedicated
     * {@link StdioServerContainer} for stdio bindings.
     *
     * @param capability
     *            capability defining transport configuration
     * @return shared or dedicated transport instance
     */
    public static CheshireTransport getOrCreate(Capability capability) {
        int port = capability.transport().getPort();
        return ENGINES.computeIfAbsent(port, p -> {
            CheshireTransport.Binding binding = CheshireTransport.Binding.from(capability.exposure().getBinding());

            if (!CheshireTransport.Binding.requiresNetwork(binding)) {
                // For transports like STDIO, create a dedicated instance per server
                log.info("Creating dedicated STDIO transport for capability {}", capability.name());
                return new StdioServerContainer();
            }

            log.info("Creating dedicated Jetty transport for binding {} on port {}", binding, port);
            return new JettyServerContainer(capability.transport());
        });
    }
}
