/*-
 * #%L
 * Cheshire :: Pipeline :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.pipeline;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Marker interface representing the execution context for a pipeline processing operation. */
public interface Context {
  ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();

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
  default Object putIfAbsent(String key, Object value) {
    if (key == null) {
      throw new IllegalArgumentException("Attribute key cannot be null");
    }
    return attributes.putIfAbsent(key, value);
  }
}
