/*-
 * #%L
 * Cheshire :: Common Utils
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

/**
 * Framework-wide constants and enumerations.
 *
 * <p><strong>Package Overview:</strong>
 *
 * <p>This package contains constant definitions used across the framework:
 *
 * <ul>
 *   <li>Protocol type constants (REST, MCP, WebSocket)
 *   <li>Binding type constants (HTTP-JSON, MCP-RPC)
 *   <li>Configuration keys
 *   <li>Default values
 * </ul>
 *
 * <p><strong>Constant Naming Conventions:</strong>
 *
 * <ul>
 *   <li>ALL_CAPS for static final constants
 *   <li>Grouped by functionality in separate classes
 *   <li>Enums for fixed sets of values
 * </ul>
 *
 * <p><strong>Example Constants:</strong>
 *
 * <pre>{@code
 * public final class CheshireConstants {
 *     // Protocol types
 *     public static final String REST_HTTP = "REST_HTTP";
 *     public static final String MCP_STDIO = "MCP_STDIO";
 *     public static final String MCP_HTTP = "MCP_STREAMABLE_HTTP";
 *
 *     // Default values
 *     public static final int DEFAULT_PORT = 8080;
 *     public static final int DEFAULT_TIMEOUT = 30000;
 *
 *     private CheshireConstants() {
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * // Use constants for configuration
 * if (exposure.getType().equals(CheshireConstants.REST_HTTP)) {
 *     // REST API configuration
 * }
 *
 * // Use enums for type safety
 * ExposureType type = ExposureType.from(config.getType());
 * switch (type) {
 * case REST_HTTP -> configureRest();
 * case MCP_STDIO -> configureStdio();
 * }
 * }</pre>
 *
 * @see io.cheshire.core.constant.Key
 * @since 1.0.0
 */
package io.cheshire.common.constants;
