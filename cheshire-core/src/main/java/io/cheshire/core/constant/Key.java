/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.constant;

/**
 * Standard metadata keys used throughout the Cheshire framework.
 *
 * <p>This enum defines a canonical set of keys for accessing metadata in contexts, requests, and
 * internal data structures. It ensures type-safe, consistent key usage across the framework.
 *
 * <p><strong>Key Categories:</strong>
 *
 * <ul>
 *   <li><strong>Capability Context:</strong> CAPABILITY, ACTION, USER_ID, SESSION_ID, TRACE_ID
 *   <li><strong>Security:</strong> SECURITY_CONTEXT
 *   <li><strong>Transport:</strong> TRANSPORT_HEADERS
 *   <li><strong>Timing:</strong> ARRIVAL_TIME, DEADLINE
 *   <li><strong>Resources:</strong> SOURCES, ENGINE
 *   <li><strong>Payload:</strong> PAYLOAD_DATA, PAYLOAD_PARAMETERS
 *   <li><strong>Debug:</strong> DEBUG, DEBUG_CTX_TASK, DEBUG_REQUEST
 * </ul>
 *
 * <p><strong>Usage Pattern:</strong>
 *
 * <pre>{@code
 * // Accessing metadata
 * String capability = context.get(Key.CAPABILITY.key());
 * String traceId = request.metadata().get(Key.TRACE_ID.key());
 *
 * // Parsing from string
 * Key key = Key.from("capability");
 * }</pre>
 *
 * <p><strong>Case Insensitivity:</strong> The {@link #from(String)} method performs
 * case-insensitive lookup, allowing flexible parsing from configuration or external sources.
 *
 * @see io.cheshire.core.task.SessionTask
 * @see io.cheshire.core.capability.Capability
 * @since 1.0.0
 */
public enum Key {
  // Capability
  CAPABILITY("capability"),
  ACTION("action"),
  USER_ID("user-id"),
  SESSION_ID("sessionId"),
  TRACE_ID("traceId"),
  SECURITY_CONTEXT("securityContext"),
  TRANSPORT_HEADERS("transportHeaders"),
  ATTRIBUTES("attributes"),
  ARRIVAL_TIME("arrivalTime"),
  DEADLINE("deadline"),

  // Source Providers
  SOURCES("sources"),
  // Query engine
  ENGINE("engine"),

  // Task Building
  PAYLOAD_DATA("payload.data"),
  PAYLOAD_PARAMETERS("payload.parameters"),
  // Debug keys
  DEBUG("debug"),
  DEBUG_CTX_TASK("task.info"),
  DEBUG_REQUEST("request.info");

  private final String keyName;

  /**
   * Constructs a Key enum constant with its string representation.
   *
   * @param key the string key name used in metadata maps
   */
  Key(String key) {
    this.keyName = key;
  }

  /**
   * Parses a string into a Key enum constant (case-insensitive).
   *
   * <p>This method allows flexible parsing from configuration files, HTTP headers, or other
   * external sources where case may vary.
   *
   * @param raw the string to parse
   * @return the matching Key enum constant
   * @throws IllegalArgumentException if no matching key found
   */
  public static Key from(String raw) {
    String normalized = raw.trim();
    for (Key k : values()) {
      if (k.keyName.equalsIgnoreCase(normalized)) {
        return k;
      }
    }
    throw new IllegalArgumentException("Unknown metadata Key: " + raw);
  }

  /**
   * Returns the string representation of this key.
   *
   * <p>This is the actual key name used in metadata maps and contexts.
   *
   * @return the key name as a string
   */
  public String key() {
    return keyName;
  }
}
