/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Root configuration model for the entire Cheshire framework.
 *
 * <p>This class represents the complete, validated configuration loaded from YAML files (typically
 * {@code cheshire.yaml} or {@code blog-application.yaml}). It serves as the single source of truth
 * for all framework settings.
 *
 * <p><strong>Configuration Structure:</strong>
 *
 * <pre>
 * CheshireConfig
 *   ├── info           - Application metadata (name, version, domain)
 *   ├── capabilities   - Business capabilities (Authors, Articles, etc.)
 *   ├── sources        - Data sources (databases, APIs)
 *   ├── query-engines  - Query processors (JDBC, Calcite)
 *   ├── exposures      - Exposure configurations (REST, MCP)
 *   └── transports     - Network transports (HTTP, stdio)
 * </pre>
 *
 * <p><strong>Lifecycle:</strong>
 *
 * <ol>
 *   <li><strong>Load:</strong> {@link io.cheshire.core.manager.ConfigurationManager} loads YAML
 *   <li><strong>Resolve:</strong> References between files are resolved (actions, pipelines)
 *   <li><strong>Validate:</strong> Structure and required fields are validated
 *   <li><strong>Distribute:</strong> Passed to managers for component initialization
 * </ol>
 *
 * <p><strong>Mutability:</strong> This class uses Lombok's {@code @Data} for convenience. While
 * technically mutable, it should be treated as immutable after initialization. ConfigurationManager
 * returns defensive copies to prevent external modifications.
 *
 * <p><strong>Nested Configuration Classes:</strong> Contains nested static classes for each
 * configuration section: {@link Info}, {@link Capability}, {@link Source}, {@link QueryEngine},
 * {@link Exposure}, {@link Transport}.
 *
 * @see io.cheshire.core.manager.ConfigurationManager
 * @see ActionsConfig
 * @see PipelineConfig
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@Slf4j
public class CheshireConfig {

  @JsonProperty("info")
  private Info info;

  @JsonProperty("capabilities")
  private Map<String, Capability> capabilities;

  @JsonProperty("sources")
  private Map<String, Source> sources;

  @JsonProperty("query-engines")
  private Map<String, QueryEngine> queryEngines;

  @JsonProperty("exposures")
  private Map<String, Exposure> exposures;

  @JsonProperty("transports")
  private Map<String, Transport> transports;

  /**
   * Returns a pretty-printed JSON representation of this configuration.
   *
   * <p>Useful for debugging, logging, and configuration inspection.
   *
   * @return a formatted JSON string representation of the configuration
   */
  public String prettyPrint() {
    try {
      ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
      return mapper.writeValueAsString(this);
    } catch (Exception e) {
      log.error("Failed to pretty-print CheshireConfig", e);
      return "Failed to pretty-print CheshireConfig: " + e.getMessage();
    }
  }

  /**
   * Prints the configuration to standard output.
   *
   * <p>This is a convenience method that calls {@link #prettyPrint()} and log the result
   */
  public void print() {
    log.debug(prettyPrint());
  }

  /**
   * Application metadata and identification information.
   *
   * <p>Provides descriptive information about the Cheshire application instance, useful for
   * monitoring, logging, and documentation.
   */
  @Data
  public static class Info {
    private String name;
    private String version;
    private String description;
    private String domain;
    private String organization;
    private String contact;
    private String environment;
  }

  /**
   * Configuration for a single business capability.
   *
   * <p>Defines a self-contained business domain with its data sources, query engine, actions,
   * pipelines, and exposure/transport settings.
   *
   * <p><strong>Key Fields:</strong>
   *
   * <ul>
   *   <li><strong>name:</strong> Unique capability identifier
   *   <li><strong>domain:</strong> Business domain grouping
   *   <li><strong>sources:</strong> List of data source names to use
   *   <li><strong>query-engine:</strong> Query engine name for data operations
   *   <li><strong>exposure:</strong> How to expose (REST, MCP stdio, MCP HTTP)
   *   <li><strong>transport:</strong> Network transport (HTTP server, stdio)
   *   <li><strong>actions-specification-file:</strong> YAML file with action definitions
   *   <li><strong>pipelines-definition-file:</strong> YAML file with pipeline configs
   * </ul>
   *
   * <p><strong>Resolution:</strong> The {@code actions} and {@code pipelines} fields are populated
   * by {@link io.cheshire.core.manager.ConfigurationManager} after loading the referenced YAML
   * files.
   */
  @Data
  @NoArgsConstructor
  public static class Capability {
    private String name;
    private String description;
    private String domain;
    private String exposure;
    private String transport;
    private List<String> sources;

    @JsonProperty("query-engine")
    private String queryEngine;

    @JsonProperty("actions-specification-file")
    private String actionsSpecificationFile;

    @JsonProperty("pipelines-definition-file")
    private String pipelinesDefinitionFile;

    private Map<String, PipelineConfig> pipelines; // resolved from pipelinesDefinitionFile
    private ActionsConfig actions; // resolved from pipelinesDefinitionFile
  }

  /**
   * Configuration for a data source.
   *
   * <p>Defines connection and access details for external data sources such as databases, APIs, or
   * file systems.
   *
   * <p><strong>Key Fields:</strong>
   *
   * <ul>
   *   <li><strong>type:</strong> Source type (jdbc, api, file)
   *   <li><strong>provider:</strong> Fully qualified factory class name
   *   <li><strong>schema:</strong> Optional database schema name
   *   <li><strong>config:</strong> Provider-specific configuration map
   * </ul>
   *
   * <p><strong>Example (JDBC):</strong>
   *
   * <pre>{@code
   * type: jdbc
   * provider: io.cheshire.source.jdbc.JdbcDataSourceProviderFactory
   * config:
   *   url: jdbc:h2:mem:blog
   *   driver: org.h2.Driver
   *   username: sa
   * }</pre>
   */
  @Data
  @NoArgsConstructor
  public static class Source {
    private String name;
    private String description;
    private String factory;
    private Map<String, Object> config;
  }

  /**
   * Configuration for a query engine.
   *
   * <p>Defines the query processor used to execute queries against data sources.
   *
   * <p><strong>Key Fields:</strong>
   *
   * <ul>
   *   <li><strong>engine:</strong> Fully qualified factory class name
   *   <li><strong>sources:</strong> List of source names this engine can query
   *   <li><strong>config:</strong> Engine-specific configuration map
   * </ul>
   *
   * <p><strong>Example (JDBC):</strong>
   *
   * <pre>{@code
   * engine: io.cheshire.query.engine.jdbc.JdbcQueryEngineFactory
   * sources: [blog-db]
   * config:
   *   fetchSize: 100
   *   queryTimeout: 30
   * }</pre>
   */
  @Data
  @NoArgsConstructor
  public static class QueryEngine {
    String name;
    String description;
    String factory;
    Map<String, Object> config;
  }

  /**
   * Configuration for how a capability is exposed to clients.
   *
   * <p>Defines the protocol and handler for client interactions.
   *
   * <p><strong>Exposure Types:</strong>
   *
   * <ul>
   *   <li><strong>rest:</strong> RESTful HTTP API
   *   <li><strong>mcp-stdio:</strong> Model Context Protocol over standard I/O
   *   <li><strong>mcp-http:</strong> Model Context Protocol over HTTP (streamable)
   * </ul>
   *
   * <p><strong>Key Fields:</strong>
   *
   * <ul>
   *   <li><strong>type:</strong> Exposure type (rest, mcp-stdio, mcp-http)
   *   <li><strong>enabled:</strong> Whether this exposure is active
   *   <li><strong>binding:</strong> URL path or endpoint (e.g., "/api/v1")
   *   <li><strong>execution:</strong> Execution mode (sync, async)
   *   <li><strong>config:</strong> Exposure-specific settings (CORS, auth, etc.)
   * </ul>
   */
  @Data
  @NoArgsConstructor
  public static class Exposure {
    private String type;
    private Boolean enabled;
    private String binding;
    private String execution;
    private Map<String, Object> config;
  }

  /**
   * Configuration for network transport layer.
   *
   * <p>Defines the server infrastructure for handling network connections.
   *
   * <p><strong>Key Fields:</strong>
   *
   * <ul>
   *   <li><strong>factory:</strong> Server factory class (e.g., Jetty, Netty)
   *   <li><strong>host:</strong> Bind address (e.g., "0.0.0.0", "localhost")
   *   <li><strong>port:</strong> Listen port (e.g., 8080)
   *   <li><strong>threadPool:</strong> Thread pool configuration
   *   <li><strong>http:</strong> HTTP-specific settings
   *   <li><strong>ssl:</strong> TLS/SSL configuration
   *   <li><strong>security:</strong> Network security policies
   * </ul>
   *
   * <p><strong>Special Case - stdio:</strong> For MCP stdio transport, most fields are unused as
   * communication happens over standard input/output streams.
   */
  @Data
  @NoArgsConstructor
  public static class Transport {
    private Integer port;
    private String host;
    private String factory;
    private ThreadPool threadPool;
    private HttpConfig http;
    private SslConfig ssl;

    @JsonProperty("security")
    private NetworkSecurity security;

    /**
     * Thread pool configuration for request handling.
     *
     * <p>Controls the server's thread pool for processing concurrent requests.
     */
    @Data
    @NoArgsConstructor
    public static class ThreadPool {
      private Integer minThreads;
      private Integer maxThreads;
      private Integer idleTimeout;
    }

    /**
     * HTTP protocol-specific configuration.
     *
     * <p>Fine-tunes HTTP request/response handling behavior.
     */
    @Data
    @NoArgsConstructor
    public static class HttpConfig {
      private Integer requestHeaderSize;
      private Integer responseHeaderSize;
      private Integer outputBufferSize;
      private Integer requestTimeout;
    }

    /**
     * SSL/TLS configuration for secure connections.
     *
     * <p>Configures HTTPS with certificates, cipher suites, and client authentication.
     */
    @Data
    @NoArgsConstructor
    public static class SslConfig {
      private Boolean enabled;
      private String protocol;
      private String keystorePath;
      private String keystorePassword;
      private String truststorePath;
      private List<String> includeCipherSuites;
      private List<String> excludeCipherSuites;
      private Boolean needClientAuth;
      private Boolean wantClientAuth;
    }

    /**
     * Network security policies for connection filtering and rate limiting.
     *
     * <p>Controls which hosts can connect and enforces connection limits.
     */
    @Data
    @NoArgsConstructor
    public static class NetworkSecurity {
      private List<String> allowedHosts;
      private List<String> blockedHosts;
      private Integer maxConnections;
      @JsonIgnore private Integer maxConnectionsPerIP; // Maps from maxConnectionsPerIP
    }
  }
}
