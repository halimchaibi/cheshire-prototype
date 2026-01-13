/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core;

import io.cheshire.common.config.ConfigSource;
import io.cheshire.common.exception.CheshireException;
import io.cheshire.core.manager.ConfigurationManager;
import io.cheshire.core.manager.LifecycleManager;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;

/**
 * Entry point for initializing the Cheshire framework.
 * <p>
 * CheshireBootstrap orchestrates the framework's initialization sequence by:
 * <ol>
 * <li>Loading configuration from filesystem or classpath</li>
 * <li>Initializing the {@link ConfigurationManager} for YAML processing</li>
 * <li>Creating and initializing the {@link LifecycleManager}</li>
 * <li>Building a fully configured {@link CheshireSession}</li>
 * </ol>
 * <p>
 * <strong>Bootstrap Architecture:</strong>
 *
 * <pre>
 * CheshireBootstrap (Orchestrator)
 *   └── ConfigurationManager (Configuration Loading)
 *   └── LifecycleManager (Component Initialization)
 *       ├── Source Providers
 *       ├── Query Engines
 *       └── Capabilities with Pipelines
 *   └── CheshireSession (Runtime Container)
 * </pre>
 * <p>
 * <strong>Usage Examples:</strong>
 *
 * <pre>{@code
 * // From classpath resources
 * CheshireSession session = CheshireBootstrap.fromClasspath("config").build();
 *
 * // From filesystem directory
 * CheshireSession session = CheshireBootstrap.fromDirectory(Path.of("/etc/cheshire")).skipSessionAutoStart() // Manual
 *                                                                                                            // start
 *                                                                                                            // control
 *         .build();
 *
 * session.start(); // If auto-start was skipped
 * }</pre>
 * <p>
 * <strong>Configuration Resolution:</strong>
 * <ul>
 * <li>Main config file is resolved via system property {@code cheshire.config}</li>
 * <li>Capability-specific files (actions, pipelines) are loaded relative to base path</li>
 * <li>Classpath fallback ensures embedded configurations work in tests/JARs</li>
 * </ul>
 * <p>
 * <strong>Error Handling:</strong> Bootstrap failures throw {@link CheshireException} with detailed context about which
 * phase failed (config loading, lifecycle init, or session building).
 *
 * @see CheshireSession
 * @see ConfigurationManager
 * @see LifecycleManager
 * @since 1.0.0
 */
@Slf4j
public class CheshireBootstrap {

    private ConfigurationManager configManager;
    private boolean autoStartSession;
    private String configPath;

    private CheshireBootstrap() {
    }

    // private CheshireBootstrap(String configPath, boolean autoStartSession) {
    // this.configPath = configPath;
    // this.configManager = new ConfigurationManager(configPath);
    // this.autoStartSession = autoStartSession;
    // }
    private CheshireBootstrap(ConfigurationManager configManager, boolean autoStartSession) {
        this.configManager = configManager;
        this.autoStartSession = autoStartSession;
    }

    /**
     * Creates a bootstrap instance loading configuration from classpath resources.
     * <p>
     * This is the primary entry point for embedded applications, tests, and JAR deployments where configuration is
     * packaged within the application.
     * <p>
     * <strong>Resolution Order:</strong>
     * <ol>
     * <li>Looks for main config file at {@code classpath:resourceRoot/cheshire.config}</li>
     * <li>System property {@code cheshire.config} overrides the default filename</li>
     * <li>Capability-specific files are loaded relative to {@code resourceRoot}</li>
     * </ol>
     * <p>
     * <strong>Example structure:</strong>
     *
     * <pre>
     * src/main/resources/config/
     *   ├── blog-application.yaml  (main config)
     *   ├── blog-actions.yaml     (capability actions)
     *   └── blog-pipelines.yaml   (capability pipelines)
     * </pre>
     *
     * @param resourceRoot
     *            classpath root directory containing configuration files
     * @return configured bootstrap instance ready to build session
     * @throws CheshireException
     *             if configuration loading fails
     */
    public static CheshireBootstrap fromClasspath(String resourceRoot) {
        return new CheshireBootstrap(new ConfigurationManager(ConfigSource.classpath(resourceRoot)), true);
    }

    /**
     * Creates a bootstrap instance loading configuration from filesystem directory.
     * <p>
     * This is the primary entry point for production deployments where configuration is externalized and managed
     * separately from the application binary.
     * <p>
     * <strong>Benefits of filesystem configuration:</strong>
     * <ul>
     * <li>Configuration updates without redeployment</li>
     * <li>Environment-specific configs (dev/staging/prod)</li>
     * <li>Security: sensitive configs outside application package</li>
     * <li>Operational flexibility: config management via ops tools</li>
     * </ul>
     * <p>
     * <strong>Path traversal security:</strong> Configuration loading enforces that all referenced files remain within
     * the specified base directory to prevent path traversal attacks.
     *
     * @param dir
     *            filesystem directory containing configuration files
     * @return configured bootstrap instance ready to build session
     * @throws CheshireException
     *             if directory doesn't exist or configuration loading fails
     * @see ConfigurationManager
     */
    public static CheshireBootstrap fromDirectory(Path dir) {
        return new CheshireBootstrap(new ConfigurationManager(ConfigSource.filesystem(dir)), true);
    }

    /**
     * Disables automatic session start during {@link #build()}.
     * <p>
     * By default, {@link #build()} calls {@link CheshireSession#start()} automatically. Use this method when you need
     * manual control over session lifecycle, such as:
     * <ul>
     * <li>Adding custom init/shutdown hooks before starting</li>
     * <li>Performing validation or warmup before activation</li>
     * <li>Coordinating startup with external systems</li>
     * </ul>
     * <p>
     * <strong>Example usage:</strong>
     *
     * <pre>{@code
     * CheshireSession session = CheshireBootstrap.fromClasspath("config").skipSessionAutoStart().build();
     *
     * // Custom initialization
     * session.addInitHook(() -> warmupCache());
     * session.start();
     * }</pre>
     *
     * @return this bootstrap instance for method chaining
     */
    public CheshireBootstrap skipSessionAutoStart() {
        this.autoStartSession = false;
        return this;
    }

    /**
     * Provides access to the underlying configuration manager.
     * <p>
     * Useful for advanced scenarios requiring direct access to configuration details before session construction.
     *
     * @return the configuration manager instance
     */
    public ConfigurationManager configManager() {
        return configManager;
    }

    /**
     * Builds and optionally starts the {@link CheshireSession}.
     * <p>
     * <strong>Build Sequence:</strong>
     * <ol>
     * <li>Load and validate {@link io.cheshire.core.config.CheshireConfig}</li>
     * <li>Initialize {@link LifecycleManager}</li>
     * <li>Initialize source providers from configuration</li>
     * <li>Initialize query engines</li>
     * <li>Initialize capabilities with their pipelines</li>
     * <li>Build {@link CheshireSession} with all components</li>
     * <li>Start session if auto-start enabled (default)</li>
     * </ol>
     * <p>
     * <strong>Component Initialization Order:</strong>
     *
     * <pre>
     * 1. Sources (Database connections, APIs)
     * 2. Query Engines (JDBC, Calcite)
     * 3. Capabilities (Business domains)
     *    └── Actions (Operations)
     *    └── Pipelines (Processing logic)
     * </pre>
     * <p>
     * The session includes shutdown hooks for graceful cleanup of all initialized components in reverse order.
     *
     * @return fully initialized and optionally started CheshireSession
     * @throws CheshireException
     *             if configuration loading, lifecycle initialization, or session build fails
     */
    public CheshireSession build() {
        try {
            var config = configManager.getCheshireConfig();
            var lifecycle = new LifecycleManager(config);
            lifecycle.initialize();

            var session = CheshireSession.builder().config(configManager.getCheshireConfig())
                    .sourceProviders(lifecycle.sources()).capabilities(lifecycle.capabilities())
                    .queryEngines(lifecycle.engines()).shutdownHooks(List.of(lifecycle::shutdown)).build();

            if (autoStartSession)
                session.start();
            return session;

        } catch (CheshireException e) {
            log.error("Cheshire bootstrap failed during initialization phase: config={}", configPath, e);
            throw e;
        } catch (Exception e) {
            log.error("Cheshire bootstrap failed unexpectedly: config={}", configPath, e);
            throw new CheshireException(
                    "Failed to bootstrap Cheshire Core - error during configuration loading, lifecycle initialization, or session building",
                    e);
        }
    }

}
