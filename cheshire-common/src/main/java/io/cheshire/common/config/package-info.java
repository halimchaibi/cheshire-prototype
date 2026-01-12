/**
 * Configuration utilities for loading and parsing YAML configuration files.
 * <p>
 * <strong>Package Overview:</strong>
 * <p>
 * This package provides robust configuration loading with support for:
 * <ul>
 *   <li><strong>ConfigLoader</strong> - YAML file loader with Jackson integration</li>
 *   <li><strong>ConfigSource</strong> - Sealed abstraction for classpath/filesystem sources</li>
 *   <li><strong>ConfigurationException</strong> - Configuration-specific exceptions</li>
 * </ul>
 * <p>
 * <strong>Loading Strategies:</strong>
 * <p>
 * The {@link io.cheshire.common.config.ConfigSource} sealed interface supports two loading strategies:
 * <pre>
 * ConfigSource (sealed)
 *   ├─ Classpath - Load from classpath resources (packaged in JAR)
 *   └─ Filesystem - Load from filesystem with security validation
 * </pre>
 * <p>
 * <strong>Example Usage:</strong>
 * <pre>{@code
 * // Load from classpath
 * ConfigSource source = ConfigSource.classpath("config");
 * ConfigLoader loader = new ConfigLoader();
 * CheshireConfig config = loader.load(source, "application.yaml", CheshireConfig.class);
 *
 * // Load from filesystem
 * ConfigSource source = ConfigSource.filesystem(Path.of("/etc/myapp"));
 * CheshireConfig config = loader.load(source, "application.yaml", CheshireConfig.class);
 *
 * // Load with TypeReference for generic types
 * Map<String, Object> config = loader.load(
 *     source,
 *     "dynamic.yaml",
 *     new TypeReference<Map<String, Object>>() {}
 * );
 * }</pre>
 * <p>
 * <strong>Security:</strong>
 * <p>
 * The filesystem loader prevents directory traversal attacks:
 * <pre>{@code
 * ConfigSource source = ConfigSource.filesystem(Path.of("/app/config"));
 *
 * // Valid: resolves to /app/config/settings.yaml
 * loader.load(source, "settings.yaml", ...);
 *
 * // Invalid: throws ConfigurationException (escapes base directory)
 * loader.load(source, "../../../etc/passwd", ...);
 * }</pre>
 * <p>
 * <strong>Fallback Strategy:</strong>
 * <p>
 * ConfigLoader attempts filesystem first, then falls back to classpath:
 * <ol>
 *   <li>Check if file exists on filesystem relative to base directory</li>
 *   <li>If found, load from filesystem</li>
 *   <li>If not found, attempt to load from classpath</li>
 *   <li>If still not found, throw ConfigurationException</li>
 * </ol>
 * <p>
 * <strong>Supported Formats:</strong>
 * <ul>
 *   <li>YAML (.yaml, .yml)</li>
 *   <li>JSON (.json) - via Jackson YAML parser</li>
 * </ul>
 * <p>
 * <strong>Error Handling:</strong>
 * <p>
 * All configuration errors are wrapped in {@link io.cheshire.common.config.ConfigurationException}:
 * <pre>{@code
 * try {
 *     config = loader.load(source, "config.yaml", CheshireConfig.class);
 * } catch (ConfigurationException e) {
 *     log.error("Failed to load configuration: {}", e.getMessage());
 *     // Provide fallback or exit
 * }
 * }</pre>
 * <p>
 * <strong>Type Safety:</strong>
 * <p>
 * ConfigLoader supports both class-based and generic type loading:
 * <pre>{@code
 * // Class-based (compile-time type safety)
 * CheshireConfig config = loader.load(..., CheshireConfig.class);
 *
 * // Generic types (runtime type safety via TypeReference)
 * List<String> values = loader.load(
 *     ...,
 *     new TypeReference<List<String>>() {}
 * );
 * }</pre>
 * <p>
 * <strong>Integration with CheshireBootstrap:</strong>
 * <pre>{@code
 * // Bootstrap uses ConfigLoader internally
 * CheshireSession session = CheshireBootstrap
 *     .fromClasspath("config")        // Uses ConfigSource.classpath()
 *     .build();
 *
 * // Or from filesystem
 * CheshireSession session = CheshireBootstrap
 *     .fromFilesystem(Path.of("/etc/cheshire"))  // Uses ConfigSource.filesystem()
 *     .build();
 * }</pre>
 *
 * @see io.cheshire.common.config.ConfigLoader
 * @see io.cheshire.common.config.ConfigSource
 * @see io.cheshire.common.config.ConfigurationException
 * @see io.cheshire.core.manager.ConfigurationManager
 * @since 1.0.0
 */
package io.cheshire.common.config;
