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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public sealed interface Canonical<S extends Canonical<S>> extends Iterable<Map.Entry<String, Object>>
        permits CanonicalInput, CanonicalOutput {
    /**
     * Access the data as a Map. In streaming implementations, this returns a Lazy-view Map.
     */
    Map<String, Object> data();

    /**
     * Access processing metadata.
     */
    Map<String, Object> metadata();

    /**
     * The abstract factory method that implementations must provide.
     */
    S copy(Map<String, Object> data, Map<String, Object> metadata);

    /**
     * By default, iterating over the object iterates over its data entries.
     */
    @Override
    default Iterator<Map.Entry<String, Object>> iterator() {
        return data().entrySet().iterator();
    }

    /**
     * Expose a stream for functional processing.
     */
    default Stream<Map.Entry<String, Object>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Shared requirement check.
     */
    @SuppressWarnings("unchecked")
    default <T> T require(String key, Class<T> type) {
        Object value = data().get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required key: " + key);
        }
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Key [%s] is %s, but expected %s".formatted(key,
                    value.getClass().getSimpleName(), type.getSimpleName()));
        }
        return (T) value;
    }

    /**
     * Shared metadata evolution logic.
     */
    default S mutateMetadata(UnaryOperator<Map<String, Object>> transform) {
        // We preserve ordering by using LinkedHashMap for the transformation
        Map<String, Object> updatedMeta = transform.apply(new LinkedHashMap<>(metadata()));
        return copy(data(), updatedMeta);
    }

    default void validate(String... requiredKeys) {
        for (String key : requiredKeys) {
            if (!data().containsKey(key)) {
                throw new IllegalArgumentException("Validation failed: Missing " + key);
            }
        }
    }
}
