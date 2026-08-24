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

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 *
 * <h1>RequestContext</h1>
 *
 * <p>The <b>Stateful Context</b> for an individual request manager. This record carries
 * cross-cutting concerns such as identity, security, observability, and transient attributes
 * through the Cheshire pipeline.
 *
 * <h3>Core Responsibilities</h3>
 *
 * <ul>
 *   <li><b>Identity:</b> Tracks the {@code sessionId} and {@code userId} across asynchronous
 *       boundaries.
 *   <li><b>Observability:</b> Stores {@code transportHeaders} for distributed tracing (e.g., W3C
 *       Trace Context, Zipkin).
 *   <li><b>Governance:</b> Maintains a {@code securityContext} for Role-Based Access Control
 *       (RBAC).
 *   <li><b>Resilience:</b> Holds an optional {@code deadline} (TTL) to enforce timeouts for
 *       long-running AI operations.
 * </ul>
 *
 * <h3>Lifecycle & Mutability</h3>
 *
 * <p>This record employs a hybrid mutability model to balance safety with flexibility:
 *
 * <ul>
 *   <li><b>Immutable Context:</b> {@code securityContext} and {@code transportHeaders} are exposed
 *       as unmodifiable maps to prevent accidental modification of security-sensitive data.
 *   <li><b>Mutable Attributes:</b> The {@code attributes} map uses {@link ConcurrentHashMap}
 *       internally to allow thread-safe enrichment as the request passes through Interceptors and
 *       Filters.
 *   <li><b>Defensive Copying:</b> The {@link #attributes()} accessor returns a defensive copy to
 *       prevent external code from holding references to the internal mutable state.
 * </ul>
 *
 * <h3>Usage Example</h3>
 *
 * <pre>{@code
 * RequestContext context = RequestContext.empty();
 *
 * // Add attributes during request processing
 * context.putIfAbsent("tenant_id", "EU-1");
 * context.putIfAbsent("request_source", "mobile_app");
 *
 * // Retrieve attributes safely
 * context.getAttribute("tenant_id").ifPresent(id -> logger.info("Processing request for tenant: {}", id));
 *
 * // Check deadline for timeout enforcement
 * if (context.deadline() != null && context.deadline().isBefore(Instant.now())) {
 *     throw new TimeoutException("Request deadline exceeded");
 * }
 *
 * // Get a snapshot of all attributes
 * ConcurrentMap<String, Object> snapshot = context.attributes();
 * }</pre>
 *
 * <h3>Thread Safety</h3>
 *
 * <p>The {@code attributes} map is backed by {@link ConcurrentHashMap}, making it safe for
 * concurrent access across multiple threads. However, individual attribute values are not
 * synchronized—callers are responsible for thread-safety of the values themselves.
 *
 * @param sessionId Unique identifier for the client session (SSE stream or Stdio pipe).
 * @param userId The authenticated subject performing the action.
 * @param traceId Unique identifier for distributed tracing across services.
 * @param securityContext Immutable map containing permissions, roles, or scopes.
 * @param transportHeaders Immutable propagation headers for observability (e.g., spanId, traceId).
 * @param attributes Thread-safe mutable map for transient key-value pairs (internal use).
 * @param arrivalTime Timestamp when the request was received by the system.
 * @param deadline The point in time after which the request should be aborted (optional).
 * @author Cheshire Framework
 * @since 1.0.0
 */
public record RequestContext(
    String sessionId,
    String userId,
    String traceId,
    Map<String, Object> securityContext,
    Map<String, String> transportHeaders,
    ConcurrentMap<String, Object> attributes,
    Instant arrivalTime,
    Instant deadline) {

  /**
   * Canonical constructor providing defensive copies and immutability guarantees.
   *
   * <p><b>Defensive Behaviors:</b>
   *
   * <ul>
   *   <li>{@code securityContext} and {@code transportHeaders} are wrapped as unmodifiable maps
   *   <li>{@code attributes} defaults to a new {@link ConcurrentHashMap} if null
   *   <li>{@code arrivalTime} defaults to {@link Instant#now()} if null
   * </ul>
   *
   * @param sessionId Session identifier (nullable)
   * @param userId User identifier (nullable)
   * @param traceId Trace identifier for distributed tracing (nullable)
   * @param securityContext Security metadata (nullable, will be unmodifiable)
   * @param transportHeaders Transport headers for observability (nullable, will be unmodifiable)
   * @param attributes Mutable attribute map (nullable, defaults to empty ConcurrentHashMap)
   * @param arrivalTime Request arrival timestamp (nullable, defaults to now)
   * @param deadline Request deadline (nullable)
   */
  public RequestContext(
      String sessionId,
      String userId,
      String traceId,
      Map<String, Object> securityContext,
      Map<String, String> transportHeaders,
      ConcurrentMap<String, Object> attributes,
      Instant arrivalTime,
      Instant deadline) {
    this.sessionId = sessionId;
    this.userId = userId;
    this.traceId = traceId;

    // Ensure consistent immutability for security-sensitive maps
    this.securityContext =
        securityContext != null
            ? Collections.unmodifiableMap(securityContext)
            : Collections.emptyMap();

    this.transportHeaders =
        transportHeaders != null
            ? Collections.unmodifiableMap(transportHeaders)
            : Collections.emptyMap();

    // Attributes remain mutable for pipeline enrichment
    this.attributes = attributes != null ? attributes : new ConcurrentHashMap<>();

    this.arrivalTime = arrivalTime != null ? arrivalTime : Instant.now();
    this.deadline = deadline;
  }

  /** Private constructor for the {@link #empty()} factory. */
  private RequestContext() {
    this(null, null, null, null, null, null, null, null);
  }

  /**
   * Creates a new, uninitialized RequestContext with default values.
   *
   * <p>All fields are initialized as follows:
   *
   * <ul>
   *   <li>String fields: {@code null}
   *   <li>{@code securityContext}: empty unmodifiable map
   *   <li>{@code transportHeaders}: empty unmodifiable map
   *   <li>{@code attributes}: empty {@link ConcurrentHashMap}
   *   <li>{@code arrivalTime}: current timestamp via {@link Instant#now()}
   *   <li>{@code deadline}: {@code null}
   * </ul>
   *
   * @return A blank context instance ready for attribute enrichment
   */
  public static RequestContext empty() {
    return new RequestContext();
  }

  /**
   * Conditionally adds an attribute if the key is not already present.
   *
   * <p>This operation is thread-safe and atomic. If the key already exists, the existing value is
   * preserved and returned.
   *
   * @param key The attribute key (must not be null)
   * @param value The attribute value to set if absent
   * @return The previous value associated with the key, or {@code null} if absent
   * @throws IllegalArgumentException if key is null
   */
  public Object putIfAbsent(String key, Object value) {
    if (key == null) {
      throw new IllegalArgumentException("Attribute key cannot be null");
    }
    return attributes.putIfAbsent(key, value);
  }

  /**
   * Returns a defensive copy of the attributes map.
   *
   * <p><b>Important:</b> This method creates a new {@link ConcurrentHashMap} containing all current
   * attribute entries. Modifications to the returned map will <b>not</b> affect the internal state
   * of this RequestContext.
   *
   * <p>Use this method to obtain a snapshot of attributes for logging, serialization, or passing to
   * external systems. For incremental attribute access, use {@link #putIfAbsent(String key, Object
   * value)} to avoid unnecessary copying.
   *
   * @return A new ConcurrentHashMap containing a snapshot of all attributes
   */
  @Override
  public ConcurrentMap<String, Object> attributes() {
    // TODO: This approach is an attempt of POSA-style controlled mutation ... may need to revisit
    // based on
    // performance profiling.
    return new ConcurrentHashMap<>(attributes);
  }
}
