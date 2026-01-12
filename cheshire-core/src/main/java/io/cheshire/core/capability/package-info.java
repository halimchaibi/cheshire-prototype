/**
 * Capability abstractions representing business-aligned, self-contained domains.
 * <p>
 * <strong>Package Overview:</strong>
 * <p>
 * Contains the core {@link io.cheshire.core.capability.Capability} record that encapsulates:
 * <ul>
 *   <li>Capability metadata (name, description, domain)</li>
 *   <li>Exposure configuration (REST, MCP stdio, MCP HTTP)</li>
 *   <li>Transport configuration (port, host, threading)</li>
 *   <li>Data sources and query engine references</li>
 *   <li>Pipeline definitions for each action</li>
 *   <li>Action specifications (MCP tools)</li>
 * </ul>
 * <p>
 * <strong>What is a Capability?</strong>
 * <p>
 * A capability is a business-aligned grouping of:
 * <ul>
 *   <li><strong>Actions:</strong> Invocable operations (exposed as MCP tools or REST endpoints)</li>
 *   <li><strong>Pipelines:</strong> Three-stage processing flow for each action</li>
 *   <li><strong>Data Access:</strong> Associated data sources and query engines</li>
 *   <li><strong>Protocol:</strong> How the capability is exposed (REST, MCP, etc.)</li>
 * </ul>
 * <p>
 * <strong>Example Capability:</strong>
 * <pre>{@code
 * Capability blogCapability = new Capability(
 *     "blog-capability",
 *     "Blog management capability",
 *     "blogging",
 *     exposure,      // REST API configuration
 *     transport,     // Port 8080, HTTP
 *     List.of("blog-db"),     // Data sources
 *     "jdbc-engine", // Query engine
 *     pipelines,     // Map of action → pipeline
 *     actions        // List of available actions
 * );
 * }</pre>
 * <p>
 * <strong>Capability Lifecycle:</strong>
 * <ol>
 *   <li>Load capability configuration from YAML</li>
 *   <li>Resolve data source and query engine references</li>
 *   <li>Load actions specification from external file</li>
 *   <li>Load pipeline definitions from external file</li>
 *   <li>Register capability in CapabilityManager</li>
 *   <li>Create server handle for capability</li>
 *   <li>Start server and expose actions</li>
 * </ol>
 * <p>
 * <strong>Multi-Protocol Support:</strong>
 * <p>
 * The same capability can be exposed through multiple protocols simultaneously:
 * <pre>{@code
 * // REST API
 * GET /api/v1/articles
 * POST /api/v1/articles
 *
 * // MCP HTTP
 * POST /mcp/v1
 * Body: {"tool": "list_articles", "arguments": {}}
 *
 * // MCP stdio
 * echo '{"tool": "list_articles"}' | java -jar app.jar --mcp-stdio
 * }</pre>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *   <li>Domain-Driven Design: Capabilities align with business domains</li>
 *   <li>Self-Contained: Each capability is independently deployable</li>
 *   <li>Protocol-Agnostic: Same logic, multiple protocols</li>
 *   <li>Composable: Capabilities can reference shared data sources</li>
 * </ul>
 *
 * @see io.cheshire.core.capability.Capability
 * @see io.cheshire.core.manager.CapabilityManager
 * @see io.cheshire.core.config.CheshireConfig.CapabilityConfig
 * @since 1.0.0
 */
package io.cheshire.core.capability;
