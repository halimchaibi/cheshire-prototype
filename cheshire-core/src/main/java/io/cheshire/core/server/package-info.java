/**
 * Server infrastructure interfaces and protocol abstractions.
 * <p>
 * <strong>Package Overview:</strong>
 * <p>
 * This package defines the core abstractions for server infrastructure:
 * <ul>
 *   <li><strong>CheshireServer</strong> - Server lifecycle interface</li>
 *   <li><strong>CheshireServerFactory</strong> - SPI factory for server creation</li>
 *   <li><strong>CheshireDispatcher</strong> - Sealed dispatcher interface (HTTP, MCP)</li>
 *   <li><strong>CheshireTransport</strong> - Transport abstraction (Jetty, stdio)</li>
 *   <li><strong>RequestHandler</strong> - Functional request processing interface</li>
 *   <li><strong>ResponseEntity</strong> - Sealed response ADT (Success, Failure)</li>
 *   <li><strong>ProtocolAdapter</strong> - Protocol translation interface</li>
 * </ul>
 * <p>
 * <strong>Server Architecture:</strong>
 * <pre>
 * CheshireServer (interface)
 *    ├─ JettyServerHandle (HTTP/WebSocket)
 *    └─ StdioServerHandle (stdio)
 *         ↓
 * CheshireTransport (interface)
 *    ├─ JettyServerContainer (physical server)
 *    └─ StdioServerContainer (stdio handler)
 *         ↓
 * ProtocolAdapter (interface)
 *    ├─ RestProtocolAdapter
 *    └─ McpProtocolAdapter
 * </pre>
 * <p>
 * <strong>Sealed Dispatcher Pattern:</strong>
 * <p>
 * The {@link io.cheshire.core.server.CheshireDispatcher} is a sealed interface with
 * restricted implementations enabling exhaustive pattern matching:
 * <pre>{@code
 * CheshireDispatcher dispatcher = getDispatcher();
 *
 * switch (dispatcher) {
 *     case HttpDispatcher http -> handleHttp(http);
 *     case McpDispatcher mcp -> handleMcp(mcp);
 *     // Compiler ensures all cases covered
 * }
 * }</pre>
 * <p>
 * <strong>Lifecycle Management:</strong>
 * <p>
 * Server lifecycle follows a consistent pattern:
 * <ol>
 *   <li><strong>init()</strong> - Load configuration, prepare resources (no network)</li>
 *   <li><strong>start()</strong> - Bind to port, start accepting requests</li>
 *   <li><strong>stop()</strong> - Gracefully shutdown, close connections</li>
 *   <li><strong>isRunning()</strong> - Check server status</li>
 * </ol>
 * <p>
 * <strong>Request Processing Flow:</strong>
 * <pre>
 * External Request
 *    ↓
 * CheshireServer.handle()
 *    ↓
 * ProtocolAdapter.toRequestEnvelope()
 *    ↓
 * CheshireDispatcher.dispatch()
 *    ↓
 * CheshireSession.execute()
 *    ↓
 * Pipeline Processing
 *    ↓
 * TaskResult → ResponseEntity
 *    ↓
 * ProtocolAdapter.fromResponseEntity()
 *    ↓
 * External Response
 * </pre>
 * <p>
 * <strong>Transport Sharing:</strong>
 * <p>
 * Multiple server handles (capabilities) can share a single transport container:
 * <pre>{@code
 * // Two capabilities sharing port 8080
 * JettyServerContainer container = new JettyServerContainer(transportConfig);
 * container.register(restCapability1Handler);  // /api/v1/blog
 * container.register(restCapability2Handler);  // /api/v1/users
 * container.start();  // Single Jetty server, multiple contexts
 * }</pre>
 * <p>
 * <strong>Virtual Thread Integration:</strong>
 * <p>
 * Server implementations leverage Java 21 Virtual Threads for massive concurrency:
 * <ul>
 *   <li>Jetty configured with Virtual Thread executor</li>
 *   <li>Request handling in Virtual Threads (cheap, lightweight)</li>
 *   <li>10,000+ concurrent requests with minimal resources</li>
 * </ul>
 * <p>
 * <strong>Design Patterns:</strong>
 * <ul>
 *   <li><strong>Facade:</strong> CheshireServer simplifies complex subsystems</li>
 *   <li><strong>Adapter:</strong> ProtocolAdapter for protocol translation</li>
 *   <li><strong>Factory:</strong> CheshireServerFactory for SPI-based creation</li>
 *   <li><strong>Sealed Interface:</strong> CheshireDispatcher for type safety</li>
 *   <li><strong>Strategy:</strong> Different server strategies (Jetty, stdio)</li>
 * </ul>
 *
 * @see io.cheshire.core.server.CheshireServer
 * @see io.cheshire.core.server.CheshireDispatcher
 * @see io.cheshire.core.server.ResponseEntity
 * @since 1.0.0
 */
package io.cheshire.core.server;
