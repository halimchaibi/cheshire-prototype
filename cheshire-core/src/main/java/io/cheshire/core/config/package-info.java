/**
 * Configuration data structures representing the entire Cheshire framework configuration.
 * <p>
 * <strong>Package Overview:</strong>
 * <p>
 * This package contains immutable records that map to YAML configuration structure:
 * <ul>
 *   <li><strong>CheshireConfig</strong> - Root configuration with nested structures</li>
 *   <li><strong>ActionsConfig</strong> - MCP tools/actions specification</li>
 *   <li><strong>PipelineConfig</strong> - Three-stage pipeline definitions</li>
 * </ul>
 * <p>
 * <strong>Configuration Hierarchy:</strong>
 * <pre>
 * CheshireConfig (root)
 *   ├─ InfoConfig - Application metadata
 *   ├─ List&lt;Source&gt; - Data source definitions
 *   ├─ List&lt;QueryEngine&gt; - Query engine definitions
 *   ├─ List&lt;Capability&gt; - Capability definitions
 *   │    ├─ Exposure - Protocol configuration
 *   │    ├─ Transport - Network configuration
 *   │    ├─ ActionsConfig - MCP tools
 *   │    └─ PipelineConfig - Processing stages
 *   ├─ List&lt;Exposure&gt; - Global exposure configs
 *   └─ List&lt;Transport&gt; - Global transport configs
 * </pre>
 * <p>
 * <strong>YAML Mapping Example:</strong>
 * <pre>{@code
 * application:
 *   name: my-app
 *   version: 1.0.0
 *
 * sources:
 *   my-db:
 *     factory: io.cheshire.source.jdbc.JdbcDataSourceProviderFactory
 *     type: jdbc
 *     config:
 *       connection:
 *         driver: org.h2.Driver
 *         url: jdbc:h2:mem:mydb
 *
 * query-engines:
 *   jdbc-engine:
 *     engine: io.cheshire.query.engine.jdbc.JdbcQueryEngineFactory
 *     sources: [my-db]
 *
 * capabilities:
 *   my-capability:
 *     name: my-capability
 *     exposure:
 *       type: REST_HTTP
 *       binding: HTTP_JSON
 *     transport:
 *       port: 8080
 *     actions-specification-file: actions.yaml
 *     pipelines-definition-file: pipelines.yaml
 * }</pre>
 * <p>
 * <strong>Immutability:</strong>
 * <p>
 * All configuration classes are implemented as Java 21 records, ensuring:
 * <ul>
 *   <li>Thread-safe access without synchronization</li>
 *   <li>Automatic equals/hashCode/toString</li>
 *   <li>Pattern matching support</li>
 *   <li>Compact constructor for validation</li>
 * </ul>
 * <p>
 * <strong>Validation:</strong>
 * <p>
 * Configuration records use compact constructors for validation:
 * <pre>{@code
 * public record CheshireConfig.Source(String name, String factory, ...) {
 *     public Source {
 *         if (name == null || name.isBlank()) {
 *             throw new IllegalArgumentException("Source name required");
 *         }
 *         // Normalize collections to immutable
 *         config = config != null ? Map.copyOf(config) : Map.of();
 *     }
 * }
 * }</pre>
 * <p>
 * <strong>Loading Process:</strong>
 * <ol>
 *   <li>Load main config (cheshire.yaml)</li>
 *   <li>Parse into CheshireConfig record</li>
 *   <li>For each capability, load referenced files:
 *       <ul>
 *         <li>actions-specification-file → ActionsConfig</li>
 *         <li>pipelines-definition-file → PipelineConfig</li>
 *       </ul>
 *   </li>
 *   <li>Validate configuration consistency</li>
 *   <li>Resolve cross-references</li>
 * </ol>
 *
 * @see io.cheshire.core.config.CheshireConfig
 * @see io.cheshire.core.config.ActionsConfig
 * @see io.cheshire.core.config.PipelineConfig
 * @see io.cheshire.core.manager.ConfigurationManager
 * @since 1.0.0
 */
package io.cheshire.core.config;
