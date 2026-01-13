/*-
 * #%L
 * Cheshire :: Common Utils
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.common.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.cheshire.common.exception.ConfigurationException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * YAML configuration loader with filesystem and classpath fallback support.
 * <p>
 * <strong>Purpose:</strong>
 * <p>
 * Provides a unified API for loading YAML configuration files from either:
 * <ol>
 * <li>Filesystem (absolute or relative paths)</li>
 * <li>Classpath resources (fallback)</li>
 * </ol>
 * <p>
 * <strong>Loading Strategy:</strong>
 * <ol>
 * <li>Check if file exists at resolved filesystem path</li>
 * <li>If exists, load from filesystem</li>
 * <li>Otherwise, attempt to load from classpath</li>
 * <li>Throw {@link ConfigurationException} if not found in either location</li>
 * </ol>
 * <p>
 * <strong>Path Resolution:</strong>
 * <p>
 * Resolves paths relative to a base directory with security validation:
 * <ul>
 * <li>Normalizes paths to prevent directory traversal</li>
 * <li>Validates resolved path is within base directory</li>
 * <li>Throws exception for paths outside base directory</li>
 * </ul>
 * <p>
 * <strong>Jackson Integration:</strong>
 * <p>
 * Uses Jackson's YAMLFactory for parsing. Supports:
 * <ul>
 * <li><strong>Class deserialization:</strong> {@code load(path, MyConfig.class)}</li>
 * <li><strong>Generic types:</strong> {@code load(path, new TypeReference<Map<String, Object>>() {})}</li>
 * </ul>
 * <p>
 * <strong>Example Usage:</strong>
 *
 * <pre>{@code
 * ConfigLoader loader = new ConfigLoader();
 * Path configDir = Path.of("/etc/cheshire/config");
 *
 * // Load specific config class
 * CheshireConfig config = loader.load(configDir, "cheshire.yaml", CheshireConfig.class);
 *
 * // Load generic map
 * Map<String, Object> rawConfig = loader.load(configDir, "settings.yaml", new TypeReference<Map<String, Object>>() {
 * });
 * }</pre>
 * <p>
 * <strong>ConfigSource Integration:</strong>
 * <p>
 * Also supports {@link ConfigSource} abstraction for custom configuration sources.
 * <p>
 * <strong>TODO:</strong> Review config path handling and complete class implementation.
 *
 * @see ConfigSource
 * @see ConfigurationException
 * @since 1.0.0
 */
// TODO: REVIEW THE CONFIG PATH HANDLING AND THE COMPLETE CLASS
@Slf4j
public class ConfigLoader {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * Load a configuration file from the given base directory.
     *
     * @param baseDir
     *            Base folder provided by the implementation
     * @param resourcePath
     *            Path relative to the baseDir
     * @param configClass
     *            Class to deserialize
     */
    public <T> T load(Path baseDir, String resourcePath, Class<T> configClass) {
        Path path = resolvePath(baseDir, resourcePath);
        return loadFromPathOrClasspath(path, resourcePath, configClass);
    }

    public <T> T load(Path baseDir, String resourcePath, TypeReference<T> typeRef) {
        Path path = resolvePath(baseDir, resourcePath);
        return loadFromPathOrClasspath(path, resourcePath, typeRef);
    }

    public <T> T load(ConfigSource source, String path, Class<T> type) {
        try (InputStream in = source.open(path)) {
            return yamlMapper.readValue(in, type);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to parse: " + path, e);
        }
    }

    public <T> T load(ConfigSource source, String path, TypeReference<T> ref) {
        try (InputStream in = source.open(path)) {
            return yamlMapper.readValue(in, ref);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to parse: " + path, e);
        }
    }

    private Path resolvePath(Path baseDir, String resourcePath) {
        Path path = baseDir.resolve(resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath)
                .toAbsolutePath().normalize();
        if (!path.startsWith(baseDir.toAbsolutePath().normalize())) {
            throw new ConfigurationException("Access denied for configuration path: " + resourcePath);
        }
        return path;
    }

    private <T> T loadFromPathOrClasspath(Path path, String resourcePath, Class<T> clazz) {
        if (Files.exists(path) && Files.isRegularFile(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                return yamlMapper.readValue(in, clazz);
            } catch (IOException e) {
                throw new ConfigurationException("Failed to parse configuration: " + path, e);
            }
        }

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null)
                throw new ConfigurationException("Config not found on classpath: " + resourcePath);
            return yamlMapper.readValue(in, clazz);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to parse config from classpath: " + resourcePath, e);
        }
    }

    private <T> T loadFromPathOrClasspath(Path path, String resourcePath, TypeReference<T> typeRef) {
        if (Files.exists(path) && Files.isRegularFile(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                return yamlMapper.readValue(in, typeRef);
            } catch (IOException e) {
                throw new ConfigurationException("Failed to parse configuration: " + path, e);
            }
        }

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null)
                throw new ConfigurationException("Config not found on classpath: " + resourcePath);
            return yamlMapper.readValue(in, typeRef);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to parse config from classpath: " + resourcePath, e);
        }
    }
}
