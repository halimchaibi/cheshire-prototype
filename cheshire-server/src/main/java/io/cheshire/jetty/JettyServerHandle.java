package io.cheshire.jetty;

import io.cheshire.core.capability.Capability;
import io.cheshire.core.constant.ExposureType;
import io.cheshire.core.server.CheshireDispatcher;
import io.cheshire.core.server.CheshireServer;
import io.cheshire.core.server.CheshireTransport;
import io.cheshire.jetty.http.JettyRestModule;
import io.cheshire.jetty.mcp.JettyMcpModule;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;

/**
 * Jetty-based server handle representing a single capability's HTTP endpoint.
 * <p>
 * <strong>Proxy Pattern:</strong>
 * <p>
 * Represents the logical service layer ("The Tenant"). Owns the capability-specific
 * dispatcher and API paths. Delegates physical server operations to the shared
 * {@link JettyServerContainer}.
 * <p>
 * <strong>Architecture:</strong>
 * <pre>
 * JettyServerHandle (Capability: "blog")
 *   ├─ Capability: business logic definition
 *   ├─ Dispatcher: request routing
 *   ├─ API Path: /api/v1/blog or /mcp/v1/blog
 *   └─ Container: shared Jetty server
 * </pre>
 * <p>
 * <strong>Lifecycle:</strong>
 * <ol>
 *   <li>{@link #init()} - Initialize configuration (no network)</li>
 *   <li>{@link #start()} - Attach to container, register context, start server</li>
 *   <li>{@link #stop()} - Reference-counted shutdown via container</li>
 * </ol>
 * <p>
 * <strong>Exposure Type Routing:</strong>
 * <ul>
 *   <li><strong>REST_HTTP:</strong> → {@link JettyRestModule}</li>
 *   <li><strong>MCP_STREAMABLE_HTTP:</strong> → {@link JettyMcpModule}</li>
 * </ul>
 * <p>
 * <strong>Multi-Tenancy:</strong>
 * <p>
 * Multiple handles can share a single container (port), each serving different
 * capabilities at different paths.
 *
 * @see CheshireServer
 * @see JettyServerContainer
 * @see JettyRestModule
 * @see JettyMcpModule
 * @since 1.0.0
 */
@Slf4j
public final class JettyServerHandle implements CheshireServer {

    private final Capability capability;
    private final CheshireTransport container;
    private final CheshireDispatcher dispatcher;

    public JettyServerHandle(Capability capability, CheshireTransport container, CheshireDispatcher dispatcher) {
        this.capability = capability;
        this.container = container;
        this.dispatcher = dispatcher;
    }

    /**
     * Prepares the jetty for execution.
     * <p>This phase should include loading the {@code CheshireConfig}, initializing the
     * {@code ProtocolAdapter}, and setting up internal handlers. No network ports should
     * be opened during this phase.</p>
     *
     * @throws Exception if configuration is invalid or required resources are unavailable.
     */
    @Override
    public void init() throws Exception {
        log.debug("Initializing logical service for {}", capability.name());
    }

    @Override
    public void start() throws Exception {
        container.attach();
        ServletContextHandler handler = switch (ExposureType.from(capability.exposure().getType())) {
            case REST_HTTP -> new JettyRestModule(capability.exposure(), dispatcher).createHandler();

            case MCP_STREAMABLE_HTTP -> new JettyMcpModule(capability, dispatcher).createHandler();
        };
        container.register(handler);
        container.start();
    }

    @Override
    public void stop() throws Exception {
        container.stop(); // Reference counted shutdown
    }

    @Override
    public String type() {
        return capability.exposure().getType();
    }

    @Override
    public boolean isRunning() {
        return container.isRunning();
    }
}
