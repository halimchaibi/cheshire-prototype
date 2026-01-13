/*-
 * #%L
 * Cheshire :: Source Provider :: JDBC
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.source.jdbc;

import io.cheshire.core.config.CheshireConfig;
import io.cheshire.spi.source.SourceConfig;
import io.cheshire.spi.source.SourceProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Generic JDBC SourceConfigT implementation. Holds all necessary information to create a JDBC connection.
 */
public record JdbcDataSourceConfig(String name, String description, String driverClassName, String jdbcUrl,
        String username, String password, Map<String, Object> additionalProperties) implements SourceConfig {

    private static final Logger log = LoggerFactory.getLogger(JdbcDataSourceConfig.class);

    /**
     * Creates a JdbcDataSourceConfig from a CheshireConfig SourceDefinition.
     *
     * @param name
     *            the source name
     * @param sourceDef
     *            the source definition from CheshireConfig
     * @return a new JdbcDataSourceConfig instance
     * @throws IllegalArgumentException
     *             if the source type is not "jdbc"
     */
    public static JdbcDataSourceConfig from(String name, CheshireConfig.Source sourceDef) {
        if (!"jdbc".equals(sourceDef.getType())) {
            throw new IllegalArgumentException("Source type must be jdbc, but was: " + sourceDef.getType());
        }

        log.debug("Creating JdbcDataSourceConfig from source definition: {}", sourceDef);

        Map<String, Object> config = sourceDef.getConfig();

        @SuppressWarnings("unchecked")
        Map<String, Object> connection = (Map<String, Object>) config.get("connection");

        if (connection == null) {
            throw new IllegalArgumentException("Missing 'connection' section in JDBC source config");
        }

        String driverClassName = (String) connection.get("driver");
        String jdbcUrl = (String) connection.get("url");
        String username = (String) connection.get("username");
        String password = (String) connection.get("password");

        if (driverClassName == null || jdbcUrl == null) {
            throw new IllegalArgumentException("JDBC 'driver' and 'url' must be defined in connection config");
        }

        // --- Everything else is additional properties ---
        Map<String, Object> additionalProperties = new HashMap<>(config);
        additionalProperties.remove("connection");

        return new JdbcDataSourceConfig(name, sourceDef.getDescription(), driverClassName, jdbcUrl, username, password,
                additionalProperties);
    }

    @Override
    public String type() {
        return "jdbc";
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>(additionalProperties);
        map.put("driverClassName", driverClassName);
        map.put("jdbcUrl", jdbcUrl);
        map.put("username", username);
        map.put("password", password);
        return map;
    }

    @Override
    public String get(String key) {
        switch (key) {
        case "driverClassName":
            return driverClassName;
        case "jdbcUrl":
            return jdbcUrl;
        case "username":
            return username;
        case "password":
            return password;
        default:
            return additionalProperties.get(key) != null ? additionalProperties.get(key).toString() : null;
        }
    }

    @Override
    public String require(String key) throws SourceProviderException {
        String value = get(key);
        if (value == null || value.isBlank()) {
            throw new SourceProviderException("Required configuration key missing: " + key);
        }
        return value;
    }

    public Properties toProperties() {
        Properties props = new Properties();
        props.putAll(additionalProperties);
        props.put("user", username);
        props.put("password", password);
        return props;
    }
}
