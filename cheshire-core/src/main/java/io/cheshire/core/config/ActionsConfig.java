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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Configuration model for MCP (Model Context Protocol) actions specification.
 * <p>
 * This class represents the structure of an MCP actions file (e.g., {@code blog-actions.yaml}), which defines the
 * tools, resources, prompts, and schemas exposed by a capability.
 * <p>
 * <strong>MCP Concepts:</strong>
 * <ul>
 * <li><strong>Tools:</strong> Invocable operations (e.g., createAuthor, listArticles)</li>
 * <li><strong>Resources:</strong> Static data endpoints (e.g., configuration, metadata)</li>
 * <li><strong>Resource Templates:</strong> Parameterized resource URIs</li>
 * <li><strong>Prompts:</strong> Pre-defined prompt templates for LLM interactions</li>
 * <li><strong>Schemas:</strong> Reusable JSON schemas for input validation</li>
 * </ul>
 * <p>
 * <strong>Lifecycle:</strong>
 * <ol>
 * <li>Loaded by {@link io.cheshire.core.manager.ConfigurationManager}</li>
 * <li>Referenced from {@link CheshireConfig.Capability} via {@code actions-specification-file}</li>
 * <li>Merged into capability configuration</li>
 * <li>Used by protocol adapters (REST, MCP) to expose operations</li>
 * </ol>
 * <p>
 * <strong>Structure:</strong>
 *
 * <pre>
 * ActionsConfig
 *   ├── info           - Metadata (name, version, authors)
 *   ├── tools          - Invocable operations
 *   ├── resources      - Static data endpoints
 *   ├── resourceTemplates - Parameterized resources
 *   ├── prompts        - LLM prompt templates
 *   ├── schemas        - Reusable JSON schemas
 *   └── configuration  - Server/auth/rate-limit settings
 * </pre>
 *
 * @see CheshireConfig.Capability
 * @see PipelineConfig
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class ActionsConfig {
    @JsonProperty("mcp")
    public String mcp;

    @JsonProperty("info")
    public Info info;

    @JsonProperty("tools")
    public List<Tool> tools;

    @JsonProperty("resources")
    public List<Resource> resources;

    @JsonProperty("resourceTemplates")
    public List<ResourceTemplate> resourceTemplates;

    @JsonProperty("prompts")
    public List<Prompt> prompts;

    @JsonProperty("configuration")
    public Configuration configuration;

    @JsonProperty("schemas")
    public Map<String, Object> schemas;

    // --- Inner Classes ---

    /**
     * Metadata about the actions specification.
     */
    public static class Info {
        @JsonProperty("name")
        public String name;

        @JsonProperty("version")
        public String version;

        @JsonProperty("description")
        public String description;

        @JsonProperty("authors")
        public List<String> authors;

        @JsonProperty("repository")
        public String repository;
    }

    /**
     * Represents an MCP tool - an invocable operation exposed to clients.
     * <p>
     * Tools map to business operations (CRUD, search, etc.) and are exposed as REST endpoints or MCP tools depending on
     * the exposure type.
     * <p>
     * <strong>Example:</strong>
     *
     * <pre>{@code
     * uri: /authors/create
     * name: createAuthor
     * description: Creates a new author
     * inputSchema:
     *   type: object
     *   properties:
     *     name: { type: string }
     *     email: { type: string }
     * }</pre>
     */
    public static class Tool {
        @JsonProperty("uri")
        public String uri;
        @JsonProperty("name")
        public String name;

        @JsonProperty("description")
        public String description;

        @JsonProperty("inputSchema")
        public Map<String, Object> inputSchema;

        @JsonProperty("metadata")
        public Map<String, Object> metadata;
    }

    /**
     * Represents an MCP resource - a static data endpoint.
     * <p>
     * Resources provide read-only access to configuration, metadata, or other static information.
     */
    public static class Resource {
        @JsonProperty("uri")
        public String uri;

        @JsonProperty("name")
        public String name;

        @JsonProperty("description")
        public String description;

        @JsonProperty("mimeType")
        public String mimeType;

        @JsonProperty("metadata")
        public Map<String, Object> metadata;
    }

    /**
     * Represents a parameterized resource URI template.
     * <p>
     * Allows dynamic resource paths with placeholders.
     * <p>
     * <strong>Example:</strong>
     *
     * <pre>{@code
     * uriTemplate: /articles/{id}
     * }</pre>
     */
    public static class ResourceTemplate {
        @JsonProperty("uriTemplate")
        public String uriTemplate;

        @JsonProperty("name")
        public String name;

        @JsonProperty("description")
        public String description;

        @JsonProperty("mimeType")
        public String mimeType;

        @JsonProperty("metadata")
        public Map<String, Object> metadata;
    }

    /**
     * Represents an MCP prompt template for LLM interactions.
     * <p>
     * Prompts provide pre-defined templates that can be filled with arguments for consistent LLM interactions.
     */
    public static class Prompt {
        @JsonProperty("name")
        public String name;

        @JsonProperty("description")
        public String description;

        @JsonProperty("arguments")
        public List<Argument> arguments;

        @JsonProperty("metadata")
        public Map<String, Object> metadata;

        /**
         * Represents a prompt argument with validation rules.
         */
        public static class Argument {
            @JsonProperty("name")
            public String name;

            @JsonProperty("description")
            public String description;

            @JsonProperty("required")
            public boolean required;

            @JsonProperty("type")
            public String type;

            @JsonProperty("enum") // Maps the reserved 'enum' keyword
            public List<String> enumValues;

            @JsonProperty("default") // Maps the reserved 'default' keyword
            public String defaultValue;
        }
    }

    /**
     * Server-level configuration for the MCP capability.
     * <p>
     * Defines authentication, rate limiting, and caching policies.
     */
    public static class Configuration {
        @JsonProperty("server")
        public Server server;

        @JsonProperty("authentication")
        public Authentication authentication;

        @JsonProperty("rateLimiting")
        public RateLimiting rateLimiting;

        @JsonProperty("caching")
        public Caching caching;

        /**
         * Server connection settings.
         */
        public static class Server {
            @JsonProperty("baseUrl")
            public String baseUrl;

            @JsonProperty("timeout")
            public int timeout;

            @JsonProperty("maxRetries")
            public int maxRetries;
        }

        /**
         * Authentication configuration.
         */
        public static class Authentication {
            @JsonProperty("type")
            public String type;

            @JsonProperty("envVar")
            public String envVar;

            @JsonProperty("description")
            public String description;
        }

        /**
         * Rate limiting configuration.
         */
        public static class RateLimiting {
            @JsonProperty("requestsPerMinute")
            public int requestsPerMinute;

            @JsonProperty("burstSize")
            public int burstSize;
        }

        /**
         * Caching configuration.
         */
        public static class Caching {
            @JsonProperty("enabled")
            public boolean enabled;

            @JsonProperty("ttl")
            public long ttl;
        }
    }
}
