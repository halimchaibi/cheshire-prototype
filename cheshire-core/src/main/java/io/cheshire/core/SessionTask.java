/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core;

import io.cheshire.common.utils.MapUtils;
import java.util.function.UnaryOperator;

/**
 *
 *
 * <h1>SessionTask</h1>
 *
 * <p>The <b>Execution Context</b> for internal business logic. This record serves as a specialized
 * data carrier that simplifies how developers interact with raw inputs and operational metadata
 * during the execution phase of an action.
 *
 * <h3>Key Design Patterns</h3>
 *
 * <ul>
 *   <li><b>Immutable Transformations:</b> Uses the "Wither" pattern via {@link
 *       #withMetadata(UnaryOperator)} to return new instances rather than mutating state, ensuring
 *       safety in reactive pipelines.
 *   <li><b>Defensive Type Safety:</b> Provides "Safe Cast" utilities ({@code contextAs}, {@code
 *       valueAs}) to eliminate {@code ClassCastException} risks during runtime.
 *   <li><b>Required Input Enforcement:</b> Includes a "fail-fast" mechanism via {@link
 *       #require(String)} for mandatory parameters.
 * </ul>
 *
 * <h3>Usage Scenarios</h3>
 *
 * <pre>{@code
 * // 1. Enforcing a required parameter
 * String sql = task.require("query");
 *
 * // 2. Safe metadata extraction
 * Optional<User> user = task.contextAs("auth_user", User.class);
 *
 * // 3. Immutable state enrichment
 * SessionTask enrichedTask = task.withMetadata(meta -> {
 *     meta.put("processed_by", "worker-1");
 *     return meta;
 * });
 * }</pre>
 *
 * @param data The primary domain data (e.g., arguments for a tool call).
 * @param metadata Contextual operational data (e.g., security scopes, session IDs).
 * @author Cheshire Framework
 * @since 1.0.0
 */
public record SessionTask(
    java.util.Map<String, Object> data, java.util.Map<String, Object> metadata) {

  public SessionTask(java.util.Map<String, Object> data, java.util.Map<String, Object> metadata) {
    this.data = java.util.Collections.unmodifiableMap(new java.util.HashMap<>(data));
    this.metadata = java.util.Collections.unmodifiableMap(new java.util.HashMap<>(metadata));
  }

  /**
   * Extracts a value from the data map, throwing an exception if the key is missing.
   *
   * <p>Use this for mandatory inputs where the absence of data prevents execution.
   *
   * @param key The data key to retrieve.
   * @param <T> The expected type.
   * @return The value associated with the key.
   * @throws IllegalArgumentException if the key is not present in the data map.
   */
  @SuppressWarnings("unchecked")
  public <T> T require(String key) {
    return (T)
        java.util.Optional.ofNullable(data.get(key))
            .orElseThrow(() -> new IllegalArgumentException("Missing required input: " + key));
  }

  /**
   * Extracts a value from the data map with strict type validation, throwing a descriptive
   * exception if the key is missing or the type is mismatched. *
   *
   * <p>This ensures "fail-fast" behavior in the pipeline: if a Pre-Processor stashed the wrong
   * type, the Executor will catch it immediately.
   *
   * @param key The attribute identifier to retrieve.
   * @param type The class representing the expected type {@code T}.
   * @param <T> The expected return type.
   * @return The value associated with the key, safely cast to {@code T}.
   * @throws IllegalArgumentException if the key is missing.
   * @throws ClassCastException if the value exists but is not assignable to {@code type}.
   */
  public <T> T requireAs(String key, Class<T> type) {
    Object value =
        java.util.Optional.ofNullable(data.get(key))
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Execution Boundary Violation: Missing required attribute '%s'"
                            .formatted(key)));

    if (!type.isInstance(value)) {
      throw new ClassCastException(
          "Attribute Type Mismatch: Key '%s' expected %s but found %s"
              .formatted(key, type.getSimpleName(), value.getClass().getSimpleName()));
    }

    return type.cast(value);
  }

  public <T> T requireMetaAs(String key, Class<T> type) {
    Object value =
        java.util.Optional.ofNullable(metadata.get(key))
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Execution Boundary Violation: Missing required attribute '%s'"
                            .formatted(key)));

    if (!type.isInstance(value)) {
      throw new ClassCastException(
          "Attribute Type Mismatch: Key '%s' expected %s but found %s"
              .formatted(key, type.getSimpleName(), value.getClass().getSimpleName()));
    }

    return type.cast(value);
  }

  /**
   * Returns a <b>new</b> SessionTask instance with transformed metadata.
   *
   * <p>This follows the immutable update pattern, preserving the original task state.
   *
   * @param transform A function that accepts the current metadata and returns an updated map.
   * @return A new SessionTask with the updated metadata.
   */
  public SessionTask withMetadata(
      java.util.function.UnaryOperator<java.util.Map<String, Object>> transform) {
    java.util.Map<String, Object> mutableMeta = new java.util.HashMap<>(this.metadata);
    java.util.Map<String, Object> updatedMeta = transform.apply(mutableMeta);
    return new SessionTask(this.data, updatedMeta);
  }

  /**
   * Safely retrieves and casts a value from the <b>metadata</b> map.
   *
   * @param key The metadata key.
   * @param clazz The target class for type checking.
   * @param <T> The expected type.
   * @return An Optional containing the value if it exists and matches the class type.
   */
  public <T> java.util.Optional<T> metadataAs(String key, Class<T> clazz) {
    return java.util.Optional.ofNullable(metadata.get(key))
        .filter(clazz::isInstance)
        .map(clazz::cast);
  }

  /**
   * Attempts to find a value in <b>data</b>, falling back to <b>metadata</b> if not found.
   *
   * <p>Useful for generic lookups where a value might be either a parameter or an attribute.
   *
   * @param key The search key.
   * @param clazz The target class for type checking.
   * @param <T> The expected type.
   * @return An Optional containing the value if found in either map and matches the type.
   */
  public <T> java.util.Optional<T> valueAs(String key, Class<T> clazz) {
    Object val = data.get(key);
    if (val == null) {
      val = metadata.get(key);
    }

    return java.util.Optional.ofNullable(val).filter(clazz::isInstance).map(clazz::cast);
  }

  /**
   * Casts the primary payload to a structured type.
   *
   * <p>If a specific "payload" key exists in the data map, it is cast; otherwise, the entire data
   * map is cast to the target type.
   *
   * @param clazz The target class.
   * @param <T> The expected type.
   * @return The casted payload or data map.
   */
  public <T> T dataAs(Class<T> clazz) {
    Object payload = data.get("payload");
    return clazz.cast(payload != null ? payload : data);
  }

  public SessionTask withDebugInfo(String key, Object value) {
    return withMetadata(
        meta -> {
          // Use your MapUtils here
          MapUtils.putNested(meta, "DEBUG", key, value);
          return meta;
        });
  }
}
