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
 * <h1>RequestEnvelope</h1> *
 * <p>
 * The <b>Root Aggregate</b> for all Cheshire protocol operations. This record acts as the "Shipping Container" for
 * every inbound request, encapsulating identity, domain routing, payload data, and stateful context.
 * </p>
 * *
 * <h3>Traceability and Routing</h3>
 * <ul>
 * <li>{@code requestId}: A mandatory UUID string used for cross-system correlation and distributed tracing.</li>
 * <li>{@code capability}: The domain-level namespace (e.g., "tools", "prompts") used to locate the correct
 * Registry.</li>
 * <li>{@code action}: The specific operation to execute within the designated capability.</li>
 * </ul>
 * *
 * <h3>Design Invariants</h3>
 * <p>
 * The <b>Compact Constructor</b> enforces strict validation rules:
 * </p>
 * <ul>
 * <li>Structural components ({@code requestId}, {@code capability}, {@code action}) cannot be null.</li>
 * <li>Temporal integrity: If {@code receivedAt} is not provided, it is automatically initialized to
 * {@link java.time.Instant#now()}.</li>
 * </ul>
 * *
 * <h3>Factory Patterns</h3>
 * <p>
 * Use the provided {@code of(...)} static factories for simplified instantiation when full metadata or context is not
 * immediately available during protocol ingestion.
 * </p>
 * *
 *
 * <pre>{@code
 * RequestEnvelope envelope = RequestEnvelope.of("req-123", "tools", "query-db", myPayload);
 * }</pre>
 *
 * @param requestId
 *            Unique identifier for correlation.
 * @param capability
 *            Target namespace for routing.
 * @param action
 *            Specific method or logic to invoke.
 * @param protocolMetadata
 *            Key specific to the ingress protocol (e.g., SSE, MCP, REST).
 * @param payload
 *            The data package containing arguments and parameters.
 * @param context
 *            The mutable manager and session state for this request.
 * @param receivedAt
 *            The exact timestamp when the request entered the Cheshire system. * @author Cheshire Framework
 * @since 1.0.0
 */
public record RequestEnvelope(String requestId, String capability, String action, ProtocolMetadata protocolMetadata,
        RequestPayload payload, RequestContext context, java.time.Instant receivedAt) {
    /**
     * Compact constructor for field validation and normalization.
     *
     * @throws NullPointerException
     *             if mandatory routing fields are missing.
     */
    public RequestEnvelope {
        java.util.Objects.requireNonNull(requestId, "requestId is required");
        java.util.Objects.requireNonNull(capability, "capability is required");
        java.util.Objects.requireNonNull(action, "action is required");

        if (receivedAt == null) {
            receivedAt = java.time.Instant.now();
        }
    }

    /**
     * Factory for creating a minimal RequestEnvelope with default context and current timestamp.
     */
    public static RequestEnvelope of(String id, String cap, String act, RequestPayload payload) {
        return new RequestEnvelope(id, cap, act, null, payload, RequestContext.empty(), java.time.Instant.now());
    }

    /**
     * Factory for creating a RequestEnvelope including protocol-specific metadata.
     */
    public static RequestEnvelope of(String id, String cap, String act, RequestPayload payload,
            ProtocolMetadata protocolMetadata) {
        return new RequestEnvelope(id, cap, act, protocolMetadata, payload, RequestContext.empty(),
                java.time.Instant.now());
    }
}
