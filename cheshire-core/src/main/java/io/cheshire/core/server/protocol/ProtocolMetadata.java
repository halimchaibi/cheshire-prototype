/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.server.protocol;

/**
 * <h1>ProtocolMetadata</h1> *
 * <p>
 * An <b>Immutable Identity Card</b> for an inbound request. This record captures the technical fingerprint of the
 * source system, ensuring that the core engine can perform protocol-specific routing or logging without being tightly
 * coupled to transport implementations (like SSE or REST).
 * </p>
 * *
 * <h3>Architectural Intent</h3>
 * <p>
 * In a multi-protocol system (where requests might arrive via <b>MCP</b>, <b>REST</b>, or <b>GraphQL</b>),
 * {@code ProtocolMetadata} acts as the translation layer's memo. It preserves the "semantic verb" and "source version"
 * used during the initial ingress.
 * </p>
 * * *
 * <h3>Design Invariants</h3>
 * <ul>
 * <li><b>Immutability:</b> The {@code headers} map is defensively copied and wrapped in an unmodifiable collection
 * during construction.</li>
 * <li><b>Standardization:</b> Provides static factories for the two primary Cheshire ingress paths: <b>MCP (Model
 * Context Protocol)</b> and <b>Standard HTTP</b>.</li>
 * </ul>
 * *
 * <h3>Usage Patterns</h3>
 *
 * <pre>{@code
 * // 1. Standard MCP Tool Call Ingress
 * ProtocolMetadata mcpMeta = ProtocolMetadata.ofMcp("tools/call");
 * * // 2. Mapping a REST API call to an Action
 * ProtocolMetadata restMeta = ProtocolMetadata.ofHttpMapping("POST", "/api/v1/query");
 * }</pre>
 *
 * @param protocolType
 *            The identifier for the source system (e.g., "REST", "MCP", "CLI").
 * @param version
 *            The specific version of the protocol specification being used.
 * @param headers
 *            A read-only view of transport-level metadata (e.g., HTTP Headers).
 * @param path
 *            The raw URI, endpoint, or target routing string.
 * @param method
 *            The semantic operation or HTTP verb (e.g., "POST", "tools/call", "resources/read").
 * @author Cheshire Framework
 * @since 1.0.0
 */
public record ProtocolMetadata(String protocolType, String version, java.util.Map<String, String> headers, String path,
        String method) {
    /**
     * Canonical constructor with defensive copies for collection safety.
     */
    public ProtocolMetadata(String protocolType, String version, java.util.Map<String, String> headers, String path,
            String method) {
        this.protocolType = protocolType;
        this.version = version;
        this.headers = java.util.Collections
                .unmodifiableMap(headers != null ? new java.util.HashMap<>(headers) : java.util.Map.of());
        this.path = path;
        this.method = method;
    }

    /**
     * Factory method for manual creation of metadata instances.
     */
    public static ProtocolMetadata of(String protocolType, String version, java.util.Map<String, String> headers,
            String path, String method) {
        return new ProtocolMetadata(protocolType, version, headers, path, method);
    }

    /**
     * Specialized factory for <b>Model Context Protocol</b> implementations. Sets protocol to "MCP" and version to
     * "1.0.0".
     *
     * @param method
     *            The JSON-RPC method name (e.g., {@code tools/call}).
     * @return A pre-configured MCP metadata instance.
     */
    public static ProtocolMetadata ofMcp(String method) {
        return new ProtocolMetadata("MCP", "1.0.0", java.util.Map.of(), null, method);
    }

    /**
     * Specialized factory for standard <b>HTTP</b> mappings.
     *
     * @param method
     *            The HTTP verb (e.g., "GET", "POST").
     * @param path
     *            The target resource path.
     * @return A pre-configured HTTP metadata instance.
     */
    public static ProtocolMetadata ofHttpMapping(String method, String path) {
        return new ProtocolMetadata("HTTP", "1.0.0", java.util.Map.of(), path, method);
    }
}
