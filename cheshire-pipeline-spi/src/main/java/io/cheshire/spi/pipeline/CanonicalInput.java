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

import java.util.Map;

public non-sealed interface CanonicalInput<S extends CanonicalInput<S>> extends Canonical<S> {
    static <T extends CanonicalInput<T>> T create(Class<T> implementationClass, Map<String, Object> data,
            Map<String, Object> metadata) {

        try {
            return implementationClass.getConstructor(Map.class, Map.class).newInstance(data, metadata);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Implementation " + implementationClass.getName() + " must provide a (Map, Map) constructor.", e);
        }
    }
}
