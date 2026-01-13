/*-
 * #%L
 * Cheshire :: Servers
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.stdio;

import io.cheshire.core.server.CheshireTransport;
import io.modelcontextprotocol.server.McpAsyncServer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Standard input/output transport container for MCP stdio protocol.
 * <p>
 * <strong>Purpose:</strong>
 * <p>
 * Manages MCP server instances communicating via standard input/output streams. Unlike HTTP transports, stdio transport
 * supports only a single registered module.
 * <p>
 * <strong>Protocol:</strong>
 * <p>
 * Implements Model Context Protocol (MCP) over stdio:
 * <ul>
 * <li>Reads JSON-RPC messages from stdin</li>
 * <li>Writes JSON-RPC responses to stdout</li>
 * <li>No network ports or HTTP overhead</li>
 * <li>Suitable for CLI tools, subprocess communication</li>
 * </ul>
 * <p>
 * <strong>Single Module Constraint:</strong>
 * <p>
 * Only one {@link McpAsyncServer} can be registered per stdio transport since there's only one stdin/stdout pair.
 * Attempting to register multiple modules throws {@link IllegalStateException}.
 * <p>
 * <strong>Lifecycle:</strong>
 * <ul>
 * <li>{@link #register(Object)} - Register MCP server (once)</li>
 * <li>{@link #start()} - Start stdio communication</li>
 * <li>{@link #stop()} - Stop stdio communication</li>
 * </ul>
 * <p>
 * <strong>Use Case:</strong>
 *
 * <pre>{@code
 * # CLI invocation:
 * $ blog-app --mcp-stdio
 * # MCP client communicates via stdin/stdout
 * }</pre>
 *
 * @see CheshireTransport
 * @see StdioServerHandle
 * @see McpAsyncServer
 * @since 1.0.0
 */
@Slf4j
public final class StdioServerContainer implements CheshireTransport {

    private final AtomicBoolean started = new AtomicBoolean(false);
    private McpAsyncServer server;

    @Override
    public synchronized void register(Object server) {
        if (!(server instanceof McpAsyncServer m)) {
            throw new IllegalArgumentException(
                    "STDIO transport only supports StdioMcpModule, got: " + server.getClass());
        }
        if (this.server != null) {
            throw new IllegalStateException("STDIO transport can only register one module");
        }

        this.server = m;
    }

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return; // already started
        }
        if (server == null) {
            log.warn("STDIO transport started without a registered module");
            return;
        }
        log.info("Starting STDIO for {}", server.getServerInfo());

    }

    @Override
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            return; // already stopped
        }

        if (server != null) {
            log.info("Stopping STDIO transport for module {}", server.getServerInfo());
        }
    }

    @Override
    public boolean isRunning() {
        return started.get();
    }

    @Override
    public void attach() {
        // No-op for STDIO
    }
}
