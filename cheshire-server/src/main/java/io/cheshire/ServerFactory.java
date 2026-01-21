/*-
 * #%L
 * Cheshire :: Servers
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire;

import io.cheshire.core.capability.Capability;
import io.cheshire.core.server.CheshireDispatcher;
import io.cheshire.core.server.CheshireServer;
import io.cheshire.core.server.CheshireServerFactory;
import io.cheshire.core.server.CheshireTransport;
import io.cheshire.jetty.JettyServerHandle;
import io.cheshire.jetty.ServerRegistry;
import io.cheshire.stdio.StdioServerHandle;

/**
 * Default implementation of {@link CheshireServerFactory} for creating server handles.
 *
 * <p><strong>Server Creation Strategy:</strong>
 *
 * <ol>
 *   <li>Determines transport container via {@link ServerRegistry}
 *   <li>Routes to appropriate server handle based on binding type
 *   <li>Creates Jetty-based handles for HTTP/MCP-HTTP bindings
 *   <li>Creates stdio handles for MCP-STDIO bindings
 * </ol>
 *
 * <p><strong>Binding Types:</strong>
 *
 * <ul>
 *   <li><strong>HTTP_JSON:</strong> REST API over HTTP → {@link JettyServerHandle}
 *   <li><strong>MCP_JSON_RPC:</strong> MCP over HTTP → {@link JettyServerHandle}
 *   <li><strong>MCP_STDIO:</strong> MCP over stdio → {@link StdioServerHandle}
 * </ul>
 *
 * <p><strong>Transport Sharing:</strong>
 *
 * <p>Multiple server handles may share a single {@link CheshireTransport} container (e.g., multiple
 * REST endpoints on same Jetty server).
 *
 * @see CheshireServerFactory
 * @see JettyServerHandle
 * @see StdioServerHandle
 * @see ServerRegistry
 * @since 1.0.0
 */
public class ServerFactory implements CheshireServerFactory {
  @Override
  public CheshireServer create(
      Capability capability, String binding, CheshireDispatcher dispatcher) {
    CheshireTransport container = ServerRegistry.getOrCreate(capability);

    return switch (CheshireTransport.Binding.from(binding)) {
      case HTTP_JSON, MCP_JSON_RPC -> new JettyServerHandle(capability, container, dispatcher);
      case MCP_STDIO -> new StdioServerHandle(capability, container, dispatcher);
      case null -> throw new IllegalArgumentException("Dispatcher cannot be null");
    };
  }
}
