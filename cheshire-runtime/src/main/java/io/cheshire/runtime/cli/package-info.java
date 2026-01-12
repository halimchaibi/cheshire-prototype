/**
 * Command-line interface components for runtime control.
 * <p>
 * <strong>Package Overview:</strong>
 * <p>
 * This package provides CLI utilities for interacting with the Cheshire runtime,
 * including argument parsing, command execution, and interactive shells.
 * <p>
 * <strong>Typical Usage:</strong>
 * <pre>{@code
 * // Command-line application
 * java -jar app.jar --rest                  // Start with REST API
 * java -jar app.jar --mcp-stdio             // Start with MCP stdio
 * java -jar app.jar --mcp-http              // Start with MCP HTTP
 * java -jar app.jar --config /path/to/config.yaml
 * }</pre>
 * <p>
 * <strong>Supported Commands:</strong>
 * <ul>
 *   <li><strong>--rest</strong> - Start with REST API exposure</li>
 *   <li><strong>--mcp-stdio</strong> - Start with MCP stdio exposure</li>
 *   <li><strong>--mcp-http</strong> - Start with MCP HTTP exposure</li>
 *   <li><strong>--config</strong> - Specify configuration file</li>
 *   <li><strong>--help</strong> - Display help message</li>
 *   <li><strong>--version</strong> - Display version information</li>
 * </ul>
 *
 * @see io.cheshire.runtime.CheshireRuntime
 * @since 1.0.0
 */
package io.cheshire.runtime.cli;
