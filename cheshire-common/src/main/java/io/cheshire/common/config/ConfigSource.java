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

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Sealed abstraction for configuration sources (classpath or filesystem).
 *
 * <p><strong>Purpose:</strong>
 *
 * <p>Provides unified API for loading configuration files from different sources with security
 * validation and path resolution.
 *
 * <p><strong>Implementations:</strong>
 *
 * <ul>
 *   <li><strong>Classpath:</strong> Loads from classpath resources (e.g., jar files)
 *   <li><strong>Filesystem:</strong> Loads from filesystem with directory traversal protection
 * </ul>
 *
 * <p><strong>Factory Methods:</strong>
 *
 * <pre>{@code
 * // Classpath source (for packaged applications)
 * ConfigSource source = ConfigSource.classpath("config");
 *
 * // Filesystem source (for external configuration)
 * ConfigSource source = ConfigSource.filesystem(Path.of("/etc/cheshire"));
 * }</pre>
 *
 * <p><strong>Security:</strong>
 *
 * <p>Filesystem implementation validates paths to prevent directory traversal attacks. All resolved
 * paths must be within the specified root directory.
 *
 * <p><strong>Sealed Interface:</strong>
 *
 * <p>Java 21 sealed interface restricts implementations to Classpath and Filesystem, enabling
 * exhaustive pattern matching and preventing uncontrolled extension.
 *
 * @see ConfigLoader
 * @see ConfigurationException
 * @since 1.0.0
 */
public sealed interface ConfigSource permits ConfigSource.Classpath, ConfigSource.Filesystem {

  /**
   * Creates a classpath-based configuration source.
   *
   * @param root root package path (e.g., "config", "com/app/config")
   * @return classpath configuration source
   */
  static ConfigSource classpath(String root) {
    return new Classpath(root);
  }

  /**
   * Creates a filesystem-based configuration source with security validation.
   *
   * @param root root directory path
   * @return filesystem configuration source
   */
  static ConfigSource filesystem(Path root) {
    return new Filesystem(root);
  }

  /* ---------- factories ---------- */

  static String normalize(String p) {
    if (p == null || p.isBlank()) return "";
    return p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
  }

  /**
   * Opens an input stream for the specified configuration path.
   *
   * @param path relative configuration path
   * @return input stream for reading configuration
   * @throws ConfigurationException if path not found or access denied
   */
  InputStream open(String path);

  /* ---------- implementations ---------- */

  /**
   * Returns the base directory for this configuration source.
   *
   * @return base directory path (filesystem) or empty (classpath)
   */
  Optional<Path> baseDir();

  final class Classpath implements ConfigSource {
    private final String root;

    private Classpath(String root) {
      this.root = normalize(root);
    }

    @Override
    public InputStream open(String path) {
      String full = root.isEmpty() ? path : root + "/" + path;
      InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(full);

      if (in == null) {
        throw new io.cheshire.common.config.ConfigurationException(
            "Config not found on classpath: " + full);
      }
      return in;
    }

    @Override
    public Optional<Path> baseDir() {
      return Optional.empty(); // classpath has no filesystem base
    }
  }

  final class Filesystem implements ConfigSource {
    private final Path root;

    private Filesystem(Path root) {
      this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public InputStream open(String path) {
      Path p = root.resolve(path).normalize();
      if (!p.startsWith(root)) {
        throw new ConfigurationException("Access denied: " + path);
      }
      try {
        return java.nio.file.Files.newInputStream(p);
      } catch (Exception e) {
        throw new ConfigurationException("Config not found: " + p, e);
      }
    }

    @Override
    public Optional<Path> baseDir() {
      return Optional.of(root);
    }
  }
}
