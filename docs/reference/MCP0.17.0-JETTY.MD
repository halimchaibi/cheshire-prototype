# MCP 0.17.0 with Jetty - Complete Wiring Guide

## Key Changes in MCP 0.17.0

According to the release notes, version 0.17.0 introduced several important changes:

1. **Module Restructuring**: The `mcp` module is now an umbrella module that brings together `mcp-core` and
   `mcp-json-jackson2`
2. **McpTransportContext Enhancements**: Re-purposed for both server and client packages, now immutable
3. **Package Changes**: Utility classes moved from `io.modelcontextprotocol.utils` to `io.modelcontextprotocol.util`

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│              MCP Client (External)              │
│         Connects via HTTP/SSE to /sse           │
└────────────────────┬────────────────────────────┘
                     │ HTTP/SSE
                     ↓
┌─────────────────────────────────────────────────┐
│            Jetty Server (Port 8080)             │
│  ┌───────────────────────────────────────────┐  │
│  │    ServletContextHandler ("/")            │  │
│  │  ┌─────────────────────────────────────┐  │  │
│  │  │ HttpServletSseServerTransportProvider│  │  │
│  │  │    (Registered at "/sse")            │  │  │
│  │  └──────────────┬──────────────────────┘  │  │
│  └─────────────────┼─────────────────────────┘  │
└────────────────────┼────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────┐
│              McpSyncServer                      │
│  ┌───────────────────────────────────────────┐  │
│  │  Server Info + Capabilities               │  │
│  ├───────────────────────────────────────────┤  │
│  │  Tool Registry                            │  │
│  │  - weather-forecast                       │  │
│  │  - calculator                             │  │
│  ├───────────────────────────────────────────┤  │
│  │  Resource Registry                        │  │
│  │  - data://config                          │  │
│  ├───────────────────────────────────────────┤  │
│  │  Prompt Registry                          │  │
│  │  - greeting                               │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

## Component Wiring Explained

### 1. Transport Provider (HttpServletSseServerTransportProvider)

This is the bridge between the HTTP servlet world and the MCP protocol:

```java
HttpServletSseServerTransportProvider transportProvider =
    HttpServletSseServerTransportProvider.builder()
        .objectMapper(new ObjectMapper())  // JSON serialization
        .contextPath("/")                  // Base path
        .endpoint("/sse")                  // SSE endpoint path
        .keepAliveInterval(Duration.ofSeconds(30))  // Keep connections alive
        .build();
```

**Key Points:**

- It implements the MCP SSE transport specification using the traditional Servlet API
- Handles asynchronous message processing using Servlet 6.0 async support
- Manages multiple client sessions
- The transport provider IS a servlet (extends HttpServlet)

### 2. MCP Server Creation

The server is created with the transport provider and configured with capabilities:

```java
McpSyncServer mcpServer = McpServer.sync(transportProvider)
    .serverInfo("my-jetty", "1.0.0")  // Server metadata
    .capabilities(...)                  // What the jetty can do
    .build();
```

**Sync vs Async:**

- `McpServer.sync()` - Blocking operations, simpler to implement
- `McpServer.async()` - Returns `Mono<T>`, better for reactive applications

### 3. Capabilities Registration

Define what features your server supports:

```java
ServerCapabilities.builder()
    .tools(true)           // Can execute tools
    .resources(false, true) // Can serve resources, with list changes
    .prompts(true)         // Can provide prompts
    .logging()             // Supports logging
    .completions()         // Supports completions
    .build()
```

### 4. Tool Registration

Tools are functions that the MCP client can invoke:

```java
McpServerFeatures.SyncToolSpecification tool =
    new McpServerFeatures.SyncToolSpecification(
        new McpSchema.Tool(
            "tool-name",           // Unique identifier
            "Description",         // What it does
            "{ JSON Schema }"      // Input parameter schema
        ),
        (exchange, arguments) -> {
            // Your implementation
            return new McpSchema.CallToolResult(...);
        }
    );

mcpServer.addTool(tool);
```

### 5. Jetty Server Setup

Wire everything into Jetty:

```java
// 1. Create Jetty jetty
Server jettyServer = new Server(8080);

// 2. Create servlet context
ServletContextHandler context = new ServletContextHandler();
context.setContextPath("/");

// 3. Register the transport provider as a servlet
ServletHolder holder = new ServletHolder(transportProvider);
context.addServlet(holder, "/sse");  // Maps to the endpoint

// 4. Attach context to jetty
jettyServer.setHandler(context);

// 5. Start
jettyServer.start();
```

## Complete Workflow

1. **Client connects** to `http://localhost:8080/sse`
2. **Jetty** routes request to `HttpServletSseServerTransportProvider`
3. **Transport Provider** establishes SSE connection and creates session
4. **MCP Server** negotiates capabilities with client
5. **Client** discovers available tools/resources/prompts
6. **Client** invokes tool → MCP Server → Your handler function
7. **Response** flows back through the chain

## Testing Your Server

### Using curl:

```bash
# Connect to SSE endpoint
curl -N http://localhost:8080/sse

# Send MCP message
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"method": "tools/list"}'
```

### Using MCP Client:

```java
McpClient client = McpClient.sync(
    HttpClientSseClientTransport.builder()
        .url("http://localhost:8080/sse")
        .build()
)
.clientInfo("test-client", "1.0.0")
.build();

// List available tools
var tools = client.listTools().block();
```

## Important Notes

- **Thread Safety**: The sync server uses blocking operations; ensure proper thread pool sizing
- **Error Handling**: Always return `CallToolResult` with `isError=true` for errors
- **Schema Validation**: JSON schemas are validated automatically
- **Session Management**: Each client connection gets its own session
- **Graceful Shutdown**: Always call `mcpServer.closeGracefully()` before stopping Jetty

## Migration from Earlier Versions

If upgrading from pre-0.17.0:

1. Update imports: `io.modelcontextprotocol.utils.*` → `io.modelcontextprotocol.util.*`
2. Use `mcp` umbrella module instead of separate `mcp-core` + `mcp-jackson2`
3. Update `McpTransportContext` usage - it's now immutable
4. Consider using the new `HttpServletStreamableServerTransportProvider` for better performance

## Resources

- Official Documentation: https://modelcontextprotocol.io/sdk/java/mcp-server
- Java SDK GitHub: https://github.com/modelcontextprotocol/java-sdk
- Example Projects: Look for examples in the SDK repository
