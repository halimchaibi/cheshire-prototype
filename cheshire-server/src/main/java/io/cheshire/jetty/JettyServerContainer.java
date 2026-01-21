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

import io.cheshire.core.config.CheshireConfig;
import io.cheshire.core.server.CheshireTransport;
import io.modelcontextprotocol.server.McpAsyncServer;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * Jetty-based HTTP transport container managing server lifecycle and servlet contexts.
 *
 * <p><strong>Purpose:</strong>
 *
 * <p>Represents the physical HTTP server infrastructure ("The Engine"). Owns the ServerSocket,
 * thread pool, and network port. Multiple server handles (REST, MCP) can share a single container
 * instance.
 *
 * <p><strong>Architecture:</strong>
 *
 * <pre>
 * JettyServerContainer (Port 8080)
 *   ├─ ServletContextHandler (/api/v1) - REST API
 *   ├─ ServletContextHandler (/mcp/v1) - MCP HTTP
 *   └─ ServletContextHandler (/metrics) - Monitoring
 * </pre>
 *
 * <p><strong>Reference Counting:</strong>
 *
 * <p>Tracks number of registered handles. Server starts when first handle registers, stops when
 * last handle unregisters. Thread-safe ref counting with atomic operations.
 *
 * <p><strong>Virtual Thread Integration:</strong>
 *
 * <p>Configures Jetty's thread pool to use Virtual Threads for request handling, enabling massive
 * concurrency with minimal resource overhead.
 *
 * <p><strong>CORS Handling:</strong>
 *
 * <p>TODO: **REVIEW CORS HANDLER** - Jetty 12 deprecated CORS as filter. Consider migrating to
 * Server → CORS → Contexts → Servlets pattern.
 *
 * <p><strong>Thread Pool:</strong>
 *
 * <ul>
 *   <li>Uses {@link QueuedThreadPool} with configurable min/max threads
 *   <li>Delegates to Virtual Thread executor for actual request processing
 *   <li>Default: 8-200 threads
 * </ul>
 *
 * <p><strong>TODO:</strong> Handle host configuration for testing purposes (currently 0.0.0.0).
 *
 * @see CheshireTransport
 * @see JettyServerHandle
 * @see ServerRegistry
 * @since 1.0.0
 */
@Slf4j
public final class JettyServerContainer implements CheshireTransport {

  private final Server server;
  private final CheshireConfig.Transport transportConfig;
  private final ContextHandlerCollection contexts;
  private final AtomicInteger refCount = new AtomicInteger(0);
  private final AtomicBoolean isStarted = new AtomicBoolean(false);
  private boolean running;

  public JettyServerContainer(CheshireConfig.Transport transportConfig) {
    this.transportConfig = transportConfig;

    int max = transportConfig.getThreadPool().getMaxThreads();
    int min = transportConfig.getThreadPool().getMinThreads();

    QueuedThreadPool threadPool = new QueuedThreadPool(max > 0 ? max : 200, min > 0 ? min : 8);
    threadPool.setName("cheshire-jetty-pool");

    threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());

    this.server = new Server(threadPool);
    this.contexts = new ContextHandlerCollection();

    // TODO: **REVIEW CORS HANDLER**
    // In jetty 12 CORS as filter had been deprecated, Server -> CORS -> Contexts -> Servlets
    // CrossOriginHandler corsHandler = new CrossOriginHandler();
    // corsHandler.setAllowedOriginPatterns(Set.of("*"));
    // corsHandler.setAllowedMethods(Set.of("GET", "POST", "HEAD", "OPTIONS"));
    // corsHandler.setAllowedHeaders(Set.of("Content-Type", "Accept", "Authorization",
    // "X-Requested-With"));
    // corsHandler.setAllowCredentials(true);
    //
    // corsHandler.setHandler(contexts);
    // server.setHandler(corsHandler);

    ServerConnector connector = new ServerConnector(server);
    // TODO: Handle host configuration for testing purposes
    connector.setHost("0.0.0.0");
    connector.setPort(transportConfig.getPort());
    server.addConnector(connector);

    server.setHandler(contexts);
  }

  /**
   * Registers a logical pre-configured ServletContextHandler (like REST or MCP) into this
   * transport.
   *
   * @param content ServletContextHandler
   */
  @Override
  public synchronized void register(Object content) {
    if (content instanceof ServletContextHandler context) {
      contexts.addHandler(context);
      if (isStarted.get()) {
        try {
          context.start();
          log.debug("Hot-deployed context {} to running container", context.getContextPath());
        } catch (Exception e) {
          String contextPath =
              context.getContextPath() != null ? context.getContextPath() : "unknown";
          log.error(
              "Failed to start hot-deployed context '{}' on container port {}",
              contextPath,
              transportConfig.getPort(),
              e);
          throw new RuntimeException(
              "Failed to start hot-deployed context '%s' on container port %d"
                  .formatted(contextPath, transportConfig.getPort()),
              e);
        }
      }
    } else if (content instanceof McpAsyncServer mcpServer) {
      log.debug("Registering MCP server is called bu tnot requred for StdIo transport");
    } else {
      throw new IllegalArgumentException(
          "Unsupported content type for registration: " + content.getClass());
    }
  }

  @Override
  public void start() throws Exception {
    synchronized (this) {
      if (isStarted.compareAndSet(false, true)) {
        server.start();
        log.info("Cheshire Jetty Container live on port {}", transportConfig.getPort());
      }
    }
  }

  @Override
  public void stop() throws Exception {
    if (refCount.decrementAndGet() <= 0 && isStarted.compareAndSet(true, false)) {
      log.info("Shutting down Jetty Container on port {}", transportConfig.getPort());
      server.stop();
    }
  }

  /** Returns true if the hardware socket is open and listening. */
  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public void attach() {
    refCount.incrementAndGet();
  }
}
