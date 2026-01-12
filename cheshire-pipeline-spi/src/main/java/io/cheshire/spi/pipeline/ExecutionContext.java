package io.cheshire.spi.pipeline;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <h1>ExecutionContext</h1>
 * <p>Represents a <b>long-lived connection context</b> (e.g., an MCP session, WebSocket, or persistent SSE stream).
 * Unlike {@link io.cheshire.spi.pipeline.ExecutionContext}, which is ephemeral and scoped to a single
 * request-response cycle, ExecutionContext persists across multiple distinct actions within the same session.</p>
 *
 * <h3>Core Responsibilities</h3>
 * <ul>
 * <li><b>Identity:</b> Tracks {@code sessionId} and {@code userId} for the lifetime of the connection.</li>
 * <li><b>Observability:</b> Maintains {@code traceId} for distributed tracing across session operations.</li>
 * <li><b>Governance:</b> Holds a {@code securityContext} for session-level Role-Based Access Control (RBAC).</li>
 * <li><b>Lifecycle Tracking:</b> Records {@code createdAt} (session start) and {@code arrivalTime} (current pulse).</li>
 * </ul>
 *
 * <h3>Mutability Model</h3>
 * <p>This record follows a <b>fully immutable</b> design pattern:</p>
 * <ul>
 * <li><b>Immutable Maps:</b> Both {@code securityContext} and {@code attributes} are exposed as
 * unmodifiable maps to prevent accidental modification.</li>
 * ExecutionContext instance with modified attributes (copy-on-write pattern).</li>
 * <li><b>Thread Safety:</b> Immutability guarantees thread-safe reads without synchronization.</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Create a new session
 * ExecutionContext session = ExecutionContext.empty();
 *
 * // Add attributes via functional mutation
 * session = session.mutateAttribute("client_version", "2.1.0");
 * session = session.mutateAttribute("capabilities", List.of("streaming", "tools"));
 *
 * // Retrieve attributes safely
 * session.getAttribute("client_version").ifPresent(version ->
 *     logger.info("Client version: {}", version)
 * );
 *
 * // Check session age
 * Duration sessionAge = Duration.between(session.createdAt(), Instant.now());
 * if (sessionAge.toHours() > 24) {
 *     logger.warn("Session {} has been active for over 24 hours", session.sessionId());
 * }
 * }</pre>
 *
 * <h3>ExecutionContext vs ExecutionContext</h3>
 * <table border="1">
 * <tr><th>Aspect</th><th>ExecutionContext</th><th>ExecutionContext</th></tr>
 * <tr><td>Lifetime</td><td>Long-lived (hours/days)</td><td>Ephemeral (milliseconds)</td></tr>
 * <tr><td>Mutability</td><td>Immutable (copy-on-write)</td><td>Hybrid (mutable attributes)</td></tr>
 * <tr><td>Scope</td><td>Connection-level</td><td>Request-level</td></tr>
 * <tr><td>Thread Safety</td><td>Immutable (inherently safe)</td><td>ConcurrentHashMap for attributes</td></tr>
 * </table>
 *
 * @param sessionId       Unique identifier for the client session (defaults to "anonymous" if null).
 * @param userId          The authenticated subject performing actions in this session.
 * @param traceId         Unique identifier for distributed tracing across session operations.
 * @param securityContext Immutable map containing session-level permissions, roles, or scopes.
 * @param attributes      Immutable map for session metadata and enrichment data.
 * @param arrivalTime     Timestamp of the current session activity (tracks latest "pulse").
 * @param deadline        The point in time after which the request should be aborted (optional).
 * @author Cheshire Framework
 * @since 1.0.0
 */
public record ExecutionContext(
        String sessionId,
        String userId,
        String traceId,
        Map<String, Object> securityContext,
        ConcurrentMap<String, Object> attributes,
        Instant arrivalTime,
        Instant deadline
) implements Context {

    /**
     * Canonical constructor providing defensive copies and immutability guarantees.
     * <p>
     * <b>Defensive Behaviors:</b>
     * <ul>
     * <li>{@code securityContext} and {@code transportHeaders} are wrapped as unmodifiable maps</li>
     * <li>{@code attributes} defaults to a new {@link ConcurrentHashMap} if null</li>
     * <li>{@code arrivalTime} defaults to {@link Instant#now()} if null</li>
     * </ul>
     *
     * @param sessionId       Session identifier (nullable)
     * @param userId          User identifier (nullable)
     * @param traceId         Trace identifier for distributed tracing (nullable)
     * @param securityContext Security metadata (nullable, will be unmodifiable)
     * @param attributes      Mutable attribute map (nullable, defaults to empty ConcurrentHashMap)
     * @param arrivalTime     Request arrival timestamp (nullable, defaults to now)
     * @param deadline        Request deadline (nullable)
     */
    public ExecutionContext(String sessionId, String userId, String traceId,
                            Map<String, Object> securityContext,
                            ConcurrentMap<String, Object> attributes,
                            Instant arrivalTime,
                            Instant deadline) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.traceId = traceId;

        // Ensure consistent immutability for security-sensitive maps
        this.securityContext = securityContext != null
                ? Collections.unmodifiableMap(securityContext)
                : Collections.emptyMap();

        // Attributes remain mutable for pipeline enrichment
        this.attributes = attributes != null
                ? attributes
                : new ConcurrentHashMap<>();

        this.arrivalTime = arrivalTime != null ? arrivalTime : Instant.now();
        this.deadline = deadline;
    }

    /**
     * Private constructor for the {@link #empty()} factory.
     */
    private ExecutionContext() {
        this(null, null, null, null, null, null, null);
    }

    /**
     * Factory method for creating an anonymous session with default values.
     * <p>
     * Creates a ExecutionContext with:
     * <p>
     * All fields are initialized as follows:
     * <ul>
     * <li>String fields: {@code null}</li>
     * <li>{@code securityContext}: empty unmodifiable map</li>
     * <li>{@code transportHeaders}: empty unmodifiable map</li>
     * <li>{@code attributes}: empty {@link ConcurrentHashMap}</li>
     * <li>{@code arrivalTime}: current timestamp via {@link Instant#now()}</li>
     * <li>{@code deadline}: {@code null}</li>
     * </ul>
     *
     * @return A blank context instance ready for attribute enrichment
     */
    public static ExecutionContext empty() {
        return new ExecutionContext();
    }

    /**
     * Conditionally adds an attribute if the key is not already present.
     * <p>
     * This operation is thread-safe and atomic. If the key already exists,
     * the existing value is preserved and returned.
     *
     * @param key   The attribute key (must not be null)
     * @param value The attribute value to set if absent
     * @return The previous value associated with the key, or {@code null} if absent
     * @throws IllegalArgumentException if key is null
     */
    @Override
    public Object putIfAbsent(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("Attribute key cannot be null");
        }
        return attributes.putIfAbsent(key, value);
    }

    /**
     * Returns a defensive copy of the attributes map.
     * <p>
     * <b>Important:</b> This method creates a new {@link ConcurrentHashMap} containing
     * all current attribute entries. Modifications to the returned map will <b>not</b>
     * affect the internal state of this ExecutionContext.
     * </p>
     * <p>
     * Use this method to obtain a snapshot of attributes for logging, serialization,
     * or passing to external systems. For incremental attribute access, use
     * {@link #putIfAbsent(String key, Object value)} to avoid unnecessary copying.
     * </p>
     *
     * @return A new ConcurrentHashMap containing a snapshot of all attributes
     */
    @Override
    public ConcurrentMap<String, Object> attributes() {
        //TODO: This approach is an attempt of POSA-style controlled mutation ... may need to revisit based on performance profiling.
        return new ConcurrentHashMap<>(attributes);
    }
}
