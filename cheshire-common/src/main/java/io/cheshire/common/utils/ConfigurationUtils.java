/*-
 * #%L
 * Cheshire :: Common Utils
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.cheshire.common.exception.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * The type Configuration utils.
 */
public final class ConfigurationUtils {

    /*
     * ========================= Jackson utilities =========================
     */

    private static final ObjectMapper COPY_MAPPER = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private ConfigurationUtils() {
        // utility class
    }

    /**
     * Deep copy a configuration object using Jackson. Used for: - Defensive copies (SpotBugs EI_EXPOSE_RE) - Test
     * isolation - Safe exposure of mutable config graphs
     *
     * @param <T>
     *            the type parameter
     * @param source
     *            the source
     * @param type
     *            the type
     * @return the t
     */
    public static <T> T deepCopy(T source, Class<T> type) {
        if (source == null) {
            return null;
        }
        try {
            byte[] bytes = COPY_MAPPER.writeValueAsBytes(source);
            return COPY_MAPPER.readValue(bytes, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deep copy configuration object of type " + type.getName(), e);
        }
    }

    /*
     * ========================= Properties utilities =========================
     */

    /**
     * Load properties from a classpath resource.
     *
     * @param resourcePath
     *            the resource path
     * @return the properties
     */
    public static Properties loadProperties(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath must not be null");

        try (InputStream input = ConfigurationUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {

            if (input == null) {
                throw new ConfigurationException("Resource not found: " + resourcePath);
            }

            Properties properties = new Properties();
            properties.load(input);
            return properties;

        } catch (IOException e) {
            throw new ConfigurationException("Failed to load properties from " + resourcePath, e);
        }
    }

    /**
     * Safe property access helper.
     *
     * @param properties
     *            the properties
     * @param key
     *            the key
     * @return the property
     */
    public static String getProperty(Properties properties, String key) {
        Objects.requireNonNull(properties, "properties must not be null");
        return properties.getProperty(key);
    }

    /**
     * Safe property access helper with default.
     *
     * @param properties
     *            the properties
     * @param key
     *            the key
     * @param defaultValue
     *            the default value
     * @return the property
     */
    public static String getProperty(Properties properties, String key, String defaultValue) {
        Objects.requireNonNull(properties, "properties must not be null");
        return properties.getProperty(key, defaultValue);
    }
}
