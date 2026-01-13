/*-
 * #%L
 * Cheshire :: Source Provider :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.source;

import java.util.Map;

public interface SourceConfig {

    String type();

    String name();

    String description();

    Map<String, Object> asMap();

    String get(String key);

    default String require(String key) throws SourceProviderException {
        String value = get(key);
        if (value == null || value.isBlank()) {
            throw new SourceProviderException("Required configuration key missing: " + key);
        }
        return value;
    }
}
