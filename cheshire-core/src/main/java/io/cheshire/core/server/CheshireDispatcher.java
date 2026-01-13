/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.server;

import io.cheshire.core.CheshireSession;
import io.cheshire.core.capability.Capability;
import io.cheshire.core.server.protocol.RequestEnvelope;

/**
 * Central request dispatcher for routing and processing capability requests.
 * <p>
 * <strong>Architectural Role:</strong>
 * <p>
 * The dispatcher sits between the network transport layer and the business logic, acting as the orchestration point for
 * request processing. It:
 * <ol>
 * <li>Receives protocol-specific {@link RequestEnvelope} from servers</li>
 * <li>Routes requests to the appropriate capability and action</li>
 * <li>Orchestrates pipeline execution (preprocess → process → postprocess)</li>
 * <li>Returns standardized {@link ResponseEntity} to the transport</li>
 * </ol>
 * <p>
 * <strong>Sealed Interface Pattern:</strong>
 * <p>
 * This sealed interface restricts implementations to three protocol-specific dispatchers:
 * <ul>
 * <li><strong>{@link HttpDispatcher}:</strong> REST API and MCP JSON-RPC over HTTP</li>
 * <li><strong>{@link StreamingDispatcher}:</strong> MCP streamable HTTP</li>
 * <li><strong>{@link StdioDispatcher}:</strong> MCP over standard I/O</li>
 * </ul>
 * <p>
 * <strong>Factory Method:</strong>
 * <p>
 * The {@link #from(Capability, CheshireSession)} factory method selects the appropriate dispatcher implementation based
 * on the capability's exposure binding.
 * <p>
 * <strong>Request Flow:</strong>
 *
 * <pre>
 * Transport → Server → Dispatcher → Capability → Pipeline → Response
 * </pre>
 *
 * @see RequestEnvelope
 * @see ResponseEntity
 * @see Capability
 * @since 1.0.0
 */
public sealed interface CheshireDispatcher permits HttpDispatcher, StreamingDispatcher, StdioDispatcher {
    /**
     * Factory method to create the appropriate dispatcher for a capability.
     * <p>
     * Selects dispatcher implementation based on the capability's exposure binding:
     * <ul>
     * <li><strong>http-json, mcp-json-rpc:</strong> {@link HttpDispatcher}</li>
     * <li><strong>streaming:</strong> {@link StreamingDispatcher}</li>
     * <li><strong>mcp-stdio:</strong> {@link StdioDispatcher}</li>
     * </ul>
     *
     * @param capability
     *            the capability configuration
     * @param session
     *            the Cheshire session with access to registries
     * @return the appropriate dispatcher implementation
     * @throws IllegalArgumentException
     *             if binding type is unknown
     */
    static CheshireDispatcher from(Capability capability, CheshireSession session) {
        return switch (DispatcherBinding.from(capability.exposure().getBinding())) {
        case HTTP_JSON, MCP_JSON_RPC -> new HttpDispatcher(session);
        case STREAMING -> new StreamingDispatcher(session);
        case MCP_STDIO -> new StdioDispatcher(session);
        };
    }

    /**
     * Dispatches a request envelope through the capability's pipeline.
     * <p>
     * <strong>Processing Steps:</strong>
     * <ol>
     * <li>Extract capability and action from envelope</li>
     * <li>Resolve pipeline for the action</li>
     * <li>Execute pipeline stages (preprocess → process → postprocess)</li>
     * <li>Package result into {@link ResponseEntity}</li>
     * </ol>
     * <p>
     * <strong>Error Handling:</strong> Exceptions during processing are caught and returned as error responses with
     * appropriate status codes and messages.
     *
     * @param envelope
     *            the protocol-specific request envelope
     * @return response entity with status, headers, and body
     */
    ResponseEntity dispatch(RequestEnvelope envelope);

    /**
     * Enumeration of supported dispatcher binding types.
     * <p>
     * Maps exposure binding strings from configuration to dispatcher types.
     */
    enum DispatcherBinding {
        HTTP_JSON, MCP_JSON_RPC, MCP_STDIO, STREAMING;

        /**
         * Parses a binding string from configuration into an enum constant.
         * <p>
         * Performs case-insensitive matching for flexibility.
         *
         * @param value
         *            the binding string from configuration
         * @return the matching DispatcherBinding
         * @throws IllegalArgumentException
         *             if value doesn't match any binding
         */
        static DispatcherBinding from(String value) {
            return switch (value.toLowerCase()) {
            case "http-json" -> HTTP_JSON;
            case "mcp-json-rpc" -> MCP_JSON_RPC;
            case "streaming" -> STREAMING;
            case "mcp-stdio" -> MCP_STDIO;
            default -> throw new IllegalArgumentException("Unknown dispatcher binding: " + value);
            };
        }
    }
}
