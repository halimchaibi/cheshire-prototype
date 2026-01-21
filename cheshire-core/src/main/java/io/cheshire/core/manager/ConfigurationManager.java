/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import io.cheshire.common.config.ConfigLoader;
import io.cheshire.common.config.ConfigSource;
import io.cheshire.common.exception.ConfigurationException;
import io.cheshire.common.utils.ConfigurationUtils;
import io.cheshire.core.config.ActionsConfig;
import io.cheshire.core.config.CheshireConfig;
import io.cheshire.core.config.PipelineConfig;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Central configuration management for the Cheshire framework.
 *
 * <p>ConfigurationManager is responsible for loading, resolving, validating, and safely exposing
 * Cheshire configuration from various sources (filesystem, classpath).
 *
 * <p><strong>Responsibilities:</strong>
 *
 * <ul>
 *   <li>Load main {@link CheshireConfig} from YAML files
 *   <li>Resolve capability references (actions, pipelines)
 *   <li>Validate configuration structure and references
 *   <li>Provide defensive copies to prevent external modification
 * </ul>
 *
 * <p><strong>Configuration Loading Process:</strong>
 *
 * <ol>
 *   <li>Load main config file (cheshire.yaml or from system property)
 *   <li>Resolve capability-specific files (actions-specification-file, pipelines-definition-file)
 *   <li>Validate all required fields and cross-references
 *   <li>Store validated configuration for session initialization
 * </ol>
 *
 * <p><strong>Configuration Sources:</strong>
 *
 * <pre>
 * ConfigSource
 *   ├── Classpath (embedded in JAR for tests/dev)
 *   └── Filesystem (external for production)
 * </pre>
 *
 * <p><strong>File Resolution:</strong> Main config file determined by (in order):
 *
 * <ol>
 *   <li>System property: {@code cheshire.config}
 *   <li>Environment variable: {@code CHESHIRE_CONFIG}
 *   <li>Default: {@code cheshire.yaml}
 * </ol>
 *
 * <p>Capability-specific files loaded relative to the main config source.
 *
 * <p><strong>Validation:</strong>
 *
 * <ul>
 *   <li>Required fields: name, exposure, transport, actions-specification-file
 *   <li>Source references must exist in sources configuration
 *   <li>Exposures must be defined
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> Immutable after initialization. {@link #getCheshireConfig()}
 * returns defensive copies.
 *
 * @see CheshireConfig
 * @see ConfigLoader
 * @see ConfigSource
 * @since 1.0.0
 */
@Slf4j
public final class ConfigurationManager {

  private final CheshireConfig cheshireConfig;
  private final ConfigLoader loader = new ConfigLoader(); // always default
  private Path baseConfigDir;
  private ConfigSource source;

  /**
   * Constructs a ConfigurationManager loading from a {@link ConfigSource}.
   *
   * <p>This is the primary constructor used by {@link io.cheshire.core.CheshireBootstrap}.
   *
   * <p><strong>Initialization sequence:</strong>
   *
   * <ol>
   *   <li>Determine config filename from system property/env/default
   *   <li>Load main {@link CheshireConfig} via {@link ConfigLoader}
   *   <li>Resolve capability references (actions, pipelines)
   *   <li>Validate complete configuration
   * </ol>
   *
   * @param source the configuration source (classpath or filesystem)
   * @throws ConfigurationException if loading or validation fails
   */
  public ConfigurationManager(ConfigSource source) {
    String configFile =
        System.getProperty(
            "cheshire.config", System.getenv().getOrDefault("CHESHIRE_CONFIG", "cheshire.yaml"));
    this.source = source;
    this.cheshireConfig = loader.load(source, configFile, CheshireConfig.class);
    initialize();
  }

  /**
   * Constructs a ConfigurationManager from a pre-built {@link CheshireConfig}.
   *
   * <p>This constructor is used for testing and programmatic configuration where YAML file loading
   * is bypassed.
   *
   * <p><strong>Note:</strong> No filesystem/classpath access. Capability references (actions,
   * pipelines) must be pre-populated in the config object.
   *
   * @param cheshireConfig fully configured CheshireConfig instance
   * @throws ConfigurationException if config is null or validation fails
   */
  public ConfigurationManager(CheshireConfig cheshireConfig) {
    if (cheshireConfig == null) {
      throw new ConfigurationException("CheshireConfig must not be null");
    }
    this.baseConfigDir = null; // no filesystem fallback
    this.cheshireConfig = cheshireConfig;
    initialize();
  }

  /**
   * Validates a required field and adds error if missing.
   *
   * @param value the field value to check
   * @param field the field name for error reporting
   * @param id the capability ID for error context
   * @param errors list to accumulate validation errors
   */
  private static void require(String value, String field, String id, List<String> errors) {
    if (value == null || value.isBlank()) {
      errors.add("Capability '" + id + "' missing required field: " + field);
    }
  }

  /**
   * Initializes the configuration by resolving references and validating.
   *
   * <p>Called automatically by constructors. Not intended for external use.
   */
  private void initialize() {
    resolveReferences();
    validate();
  }

  /**
   * Returns a defensive deep copy of the configuration.
   *
   * <p><strong>Why defensive copy?</strong> Prevents external modification of internal
   * configuration state. Required to avoid SpotBugs warning EI_EXPOSE_REP (Exposing Internal
   * Representation).
   *
   * <p><strong>Performance Note:</strong> This operation uses deep cloning via JSON
   * serialization/deserialization. Cache the result if called frequently.
   *
   * @return deep copy of CheshireConfig
   * @see ConfigurationUtils#deepCopy
   */
  public CheshireConfig getCheshireConfig() {
    return ConfigurationUtils.deepCopy(cheshireConfig, CheshireConfig.class);
  }

  /**
   * Resolves capability references to their specific configuration files.
   *
   * <p>For each capability, loads:
   *
   * <ul>
   *   <li>Actions specification (MCP tools, REST endpoints)
   *   <li>Pipeline definitions (PreProcessor, Executor, PostProcessor configs)
   * </ul>
   *
   * <p>Files are loaded relative to the main configuration source.
   */
  private void resolveReferences() {
    if (cheshireConfig.getCapabilities() == null) {
      return;
    }

    cheshireConfig
        .getCapabilities()
        .values()
        .forEach(
            capability -> {
              capability.setActions(
                  loadActionsSpecification(capability.getActionsSpecificationFile()));

              capability.setPipelines(
                  loadPipelinesDefinition(capability.getPipelinesDefinitionFile()));
            });
  }

  /**
   * Loads pipeline definitions for a capability.
   *
   * <p>Pipelines define the three-stage processing for each action: PreProcessor → Executor →
   * PostProcessor
   *
   * <p>Each pipeline specifies:
   *
   * <ul>
   *   <li>Implementation classes
   *   <li>DSL query templates
   *   <li>Validation rules
   * </ul>
   *
   * @param path relative path to pipelines YAML file
   * @return map of action name to pipeline configuration
   * @throws ConfigurationException if file cannot be loaded
   */
  private Map<String, PipelineConfig> loadPipelinesDefinition(String path) {
    if (source == null) {
      throw new ConfigurationException("Cannot load pipelines: ConfigLoader not initialized");
    }
    return loader.load(source, path, new TypeReference<Map<String, PipelineConfig>>() {});
  }

  /**
   * Loads actions specification for a capability.
   *
   * <p>Actions define the operations exposed by the capability:
   *
   * <ul>
   *   <li>MCP: tools with input schemas
   *   <li>REST: endpoints with HTTP methods
   *   <li>GraphQL: queries and mutations
   * </ul>
   *
   * @param path relative path to actions YAML file
   * @return actions configuration for the capability
   * @throws ConfigurationException if file cannot be loaded
   */
  private ActionsConfig loadActionsSpecification(String path) {
    if (source == null) {
      throw new ConfigurationException("Cannot load actions: ConfigLoader not initialized");
    }
    return loader.load(source, path, ActionsConfig.class);
  }

  /**
   * Validates the complete configuration structure.
   *
   * <p><strong>Validation checks:</strong>
   *
   * <ul>
   *   <li>Required capability fields (name, exposure, transport, etc.)
   *   <li>Source references exist
   *   <li>Exposures are defined
   *   <li>No dangling references
   * </ul>
   *
   * <p>Accumulates all errors before throwing to provide complete feedback.
   *
   * @throws ConfigurationException if any validation fails
   */
  private void validate() {
    List<String> errors = new ArrayList<>();

    validateCapabilities(errors);
    validateProtocols(errors);

    if (!errors.isEmpty()) {
      String message = "Cheshire configuration validation failed:\n" + String.join("\n", errors);
      log.error(message);
      throw new ConfigurationException(message);
    }

    log.info("Cheshire configuration validation passed");
  }

  /**
   * Validates all capability configurations.
   *
   * <p>Checks each capability for:
   *
   * <ul>
   *   <li>Required fields present
   *   <li>Source references valid
   *   <li>Actions and pipelines loadable
   * </ul>
   *
   * @param errors list to accumulate validation errors
   */
  private void validateCapabilities(List<String> errors) {
    Map<String, CheshireConfig.Capability> capabilities = cheshireConfig.getCapabilities();
    Map<String, CheshireConfig.Source> sources = cheshireConfig.getSources();

    if (capabilities == null) {
      return;
    }

    for (var entry : capabilities.entrySet()) {
      String id = entry.getKey();
      CheshireConfig.Capability cap = entry.getValue();

      require(cap.getName(), "name", id, errors);
      require(cap.getExposure(), "exposure", id, errors);
      require(cap.getTransport(), "protocol", id, errors);
      require(cap.getActionsSpecificationFile(), "actions-specification-file", id, errors);

      if (cap.getSources() != null) {
        for (String source : cap.getSources()) {
          if (sources == null || !sources.containsKey(source)) {
            errors.add("Capability '" + id + "' references unknown source: " + source);
          }
        }
      }
    }
  }

  /**
   * Validates protocol/exposure configurations.
   *
   * <p>Ensures at least one exposure is defined for the framework to be operational.
   *
   * @param errors list to accumulate validation errors
   */
  private void validateProtocols(List<String> errors) {
    if (cheshireConfig.getExposures() == null) {
      throw new ConfigurationException("CheshireConfig must have exposures");
    }
  }
}
