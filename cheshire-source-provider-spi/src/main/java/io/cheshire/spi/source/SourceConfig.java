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
