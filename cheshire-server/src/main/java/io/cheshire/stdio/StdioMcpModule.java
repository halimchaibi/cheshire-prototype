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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cheshire.core.capability.Capability;
import io.cheshire.core.server.CheshireDispatcher;
import io.cheshire.jetty.mcp.McpManifestRegistrar;
import io.cheshire.jetty.mcp.McpProtocolAdapter;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class StdioMcpModule {

    private final Capability capability;
    private final CheshireDispatcher dispatcher;

    public StdioMcpModule(Capability capability, CheshireDispatcher dispatcher) {
        this.capability = capability;
        this.dispatcher = dispatcher;
    }

    public McpAsyncServer createServer() {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);

        StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(jsonMapper);

        McpAsyncServer mcpServer = McpServer.async(transportProvider).serverInfo("cheshire-mcp-jetty", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).resources(true, true).prompts(true)
                        .logging().build())
                .build();
        McpManifestRegistrar.of(capability, dispatcher, mcpServer, new McpProtocolAdapter()).register();
        log.info("Stdio MCP Module initialized for capability: {}", capability.name());
        return mcpServer;
    }
}
