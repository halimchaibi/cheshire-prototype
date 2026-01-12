package io.cheshire.stdio;

import io.cheshire.core.capability.Capability;
import io.cheshire.core.server.CheshireDispatcher;
import io.cheshire.core.server.CheshireServer;
import io.cheshire.core.server.CheshireTransport;
import io.modelcontextprotocol.server.McpAsyncServer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class StdioServerHandle implements CheshireServer {

    private final Capability capability;
    private final CheshireDispatcher dispatcher;
    private final CheshireTransport container;

    public StdioServerHandle(
            Capability capability,
            CheshireTransport container,
            CheshireDispatcher dispatcher
    ) {
        this.capability = capability;
        this.container = container;
        this.dispatcher = dispatcher;
    }

    /**
     * Prepares the module configuration (no ports opened yet).
     */
    @Override
    public void init(
    ) throws Exception {

    }

    @Override
    public void start() throws Exception {
        log.info("Starting STDIO server handle for capability {}", capability.name());
        McpAsyncServer server = new StdioMcpModule(capability, dispatcher).createServer();
        container.register(server);
        container.start();
    }

    @Override
    public void stop() throws Exception {
        log.info("Stopping STDIO server handle for capability {}", capability.name());
        container.stop();
    }

    /**
     * Returns the exposure type (e.g., "REST-API", "STREAMABLE-MCP").
     */
    @Override
    public String type() {
        return "MCP-STDIO";
    }

    @Override
    public boolean isRunning() {
        return container.isRunning();
    }
}
