/*-
 * #%L
 * Cheshire :: Servers
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.jetty.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cheshire.core.capability.Capability;
import io.cheshire.core.server.CheshireDispatcher;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

import java.time.Duration;

@Slf4j
public final class JettyMcpModule {

    private final Capability capability;
    private final CheshireDispatcher dispatcher;

    public JettyMcpModule(Capability capability, CheshireDispatcher dispatcher) {
        this.capability = capability;
        this.dispatcher = dispatcher;
    }

    public ServletContextHandler createHandler() {
        String baseUrl = capability.exposure().getConfig().get("path") instanceof String path ? path : "/mcp/v1";

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).enable(SerializationFeature.INDENT_OUTPUT);

        JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(mapper);

        HttpServletStreamableServerTransportProvider transport = HttpServletStreamableServerTransportProvider.builder()
                // TODO: Check if this is not causing issues and enforce trailing slash in urls
                .mcpEndpoint("/").jsonMapper(jsonMapper).keepAliveInterval(Duration.ofSeconds(30))
                .contextExtractor(request -> request::getHeader).build();

        McpAsyncServer mcpServer = McpServer.async(transport).serverInfo("cheshire-mcp-jetty", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).resources(true, true).prompts(true)
                        .logging().build())
                .build();

        McpManifestRegistrar.of(capability, dispatcher, mcpServer, new McpProtocolAdapter()).register();

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath(baseUrl); // Server scope not responsibility of client to provide it.
        // client of the api will use /mcp/v1/{capabilityName} as base path, capability is unique per server

        context.addServlet(new ServletHolder(transport), "/*");

        log.info("MCP Module context initialized at {}/*", context.getContextPath());
        return context;
    }
}
