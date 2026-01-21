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

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration model for a three-stage processing pipeline.
 *
 * <p><strong>Pipeline Architecture:</strong>
 *
 * <p>Cheshire uses a three-stage pipeline pattern for all action processing:
 *
 * <pre>
 * Request → [Preprocess] → [Process] → [Postprocess] → Response
 * </pre>
 *
 * <p><strong>Pipeline Stages:</strong>
 *
 * <ol>
 *   <li><strong>Preprocess:</strong> Input validation, transformation, enrichment (multiple steps)
 *   <li><strong>Process:</strong> Core business logic, query execution (single step)
 *   <li><strong>Postprocess:</strong> Output formatting, filtering, pagination (multiple steps)
 * </ol>
 *
 * <p><strong>Example Pipeline (Create Author):</strong>
 *
 * <pre>{@code
 * preprocess:
 *   - name: validateInput
 *     implementation: io.blog.pipeline.BlogInputProcessor
 *
 * process:
 *   name: executeQuery
 *   implementation: io.blog.pipeline.BlogExecutor
 *   template: DSL_QUERY
 *   description: |
 *     INSERT INTO Authors (id, name, email, bio, created_at)
 *     VALUES ({{id}}, {{name}}, {{email}}, {{bio}}, NOW())
 *
 * postprocess:
 *   - name: formatOutput
 *     implementation: io.blog.pipeline.BlogOutputProcessor
 * }</pre>
 *
 * <p><strong>Lifecycle:</strong>
 *
 * <ol>
 *   <li>Loaded by {@link io.cheshire.core.manager.ConfigurationManager}
 *   <li>Referenced from {@link CheshireConfig.Capability} via {@code pipelines-definition-file}
 *   <li>Built into {@link io.cheshire.spi.pipeline.PipelineProcessor} by {@link
 *       io.cheshire.core.pipeline.PipelineFactory}
 *   <li>Executed for each action invocation
 * </ol>
 *
 * @see ActionsConfig
 * @see io.cheshire.core.pipeline.PipelineFactory
 * @see io.cheshire.spi.pipeline.PipelineProcessor
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class PipelineConfig {

  private String uri;
  private String description;
  private String input; // usually "canonicalInput"
  private Steps pipeline;
  private String output; // usually "canonicalOutput"

  /**
   * Container for the three pipeline stages.
   *
   * <p>Each stage contains one or more processing steps. The {@code process} stage is always a
   * single step, while {@code preprocess} and {@code postprocess} can have multiple steps executed
   * sequentially.
   */
  @Data
  @NoArgsConstructor
  public static class Steps {
    private List<Step> preprocess;
    private Step process;
    private List<Step> postprocess;
  }

  /**
   * Represents a single processing step within a pipeline stage.
   *
   * <p><strong>Key Fields:</strong>
   *
   * <ul>
   *   <li><strong>name:</strong> Step identifier (e.g., "validateInput")
   *   <li><strong>type:</strong> Step category (transformer, executor, validator)
   *   <li><strong>implementation:</strong> Fully qualified class name of the processor
   *   <li><strong>template:</strong> Template type for the process step (e.g., DSL_QUERY, SQL)
   *   <li><strong>description:</strong> Template content or step description
   * </ul>
   *
   * <p><strong>Special Case - Process Step:</strong> The {@code process} step typically includes a
   * {@code template} field specifying the query template type (DSL_QUERY) and a {@code description}
   * field containing the actual query template with placeholders.
   */
  @Data
  @NoArgsConstructor
  public static class Step {
    private String name;
    private String type; // transformer, executor, etc.
    private String implementation; // fully qualified class name
    private String template; // optional, used in process step
    private String description;
  }
}
