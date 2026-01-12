/**
 * Service Provider Interface (SPI) for three-stage pipeline processing.
 * <p>
 * <strong>Package Overview:</strong>
 * <p>
 * This package defines the core SPI for Cheshire's three-stage pipeline pattern:
 * <ul>
 *   <li><strong>PipelineProcessor</strong> - Orchestrator for the three-stage flow</li>
 *   <li><strong>PreProcessor</strong> - Input validation and transformation (stage 1)</li>
 *   <li><strong>Executor</strong> - Business logic execution (stage 2)</li>
 *   <li><strong>PostProcessor</strong> - Output formatting and enrichment (stage 3)</li>
 *   <li><strong>MaterializedInput</strong> - Immutable input data carrier</li>
 *   <li><strong>MaterializedOutput</strong> - Immutable output data carrier</li>
 * </ul>
 * <p>
 * <strong>Three-Stage Pipeline Pattern:</strong>
 * <pre>
 * Input (Map)
 *      ↓
 * MaterializedInput (record)
 *      ↓
 * PreProcessor (validate/transform)
 *      ↓
 * Executor (business logic)
 *      ↓
 * MaterializedOutput (record)
 *      ↓
 * PostProcessor (format/enrich)
 *      ↓
 * Output (Map)
 * </pre>
 * <p>
 * <strong>Implementation Example:</strong>
 * <pre>{@code
 * // Stage 1: PreProcessor
 * public class BlogInputProcessor implements PreProcessor {
 *     @Override
 *     public MaterializedInput process(MaterializedInput input) {
 *         // Validate parameters
 *         validateRequired(input, "articleId");
 *         validateType(input, "articleId", Integer.class);
 *
 *         // Transform if needed
 *         return input; // or return new MaterializedInput(transformed)
 *     }
 * }
 *
 * // Stage 2: Executor
 * public class BlogExecutor implements Executor {
 *     @Override
 *     public MaterializedOutput execute(MaterializedInput input, SessionTask task) {
 *         // Extract DSL query template from action
 *         Map<String, Object> dslTemplate = getDslTemplate(task.action());
 *
 *         // Build SQL query
 *         SqlQueryRequest query = sqlBuilder.buildQuery(dslTemplate, input.asMap());
 *
 *         // Execute via query engine
 *         MapQueryResult result = queryEngine.execute(query, sourceProvider);
 *
 *         // Wrap result
 *         return MaterializedOutput.of(result.rows());
 *     }
 * }
 *
 * // Stage 3: PostProcessor
 * public class BlogOutputProcessor implements PostProcessor {
 *     @Override
 *     public MaterializedOutput process(MaterializedOutput output) {
 *         // Add metadata
 *         Map<String, Object> enriched = new HashMap<>(output.asMap());
 *         enriched.put("timestamp", Instant.now());
 *         enriched.put("version", "1.0");
 *
 *         return MaterializedOutput.of(enriched);
 *     }
 * }
 * }</pre>
 * <p>
 * <strong>Pipeline Composition:</strong>
 * <p>
 * Pipelines are composed via stream-based reduction:
 * <pre>{@code
 * MaterializedOutput result = Stream.of(input)
 *     .map(preProcessors::reduce)   // Stage 1: All PreProcessors
 *     .map(executor::execute)        // Stage 2: Single Executor
 *     .map(postProcessors::reduce)   // Stage 3: All PostProcessors
 *     .findFirst()
 *     .orElseThrow();
 * }</pre>
 * <p>
 * <strong>Immutability:</strong>
 * <p>
 * MaterializedInput and MaterializedOutput are immutable records:
 * <pre>{@code
 * public record MaterializedInput(Map<String, Object> data) {
 *     public MaterializedInput {
 *         data = data != null ? Map.copyOf(data) : Map.of();
 *     }
 *
 *     public Map<String, Object> asMap() {
 *         return data; // Already immutable
 *     }
 * }
 * }</pre>
 * <p>
 * <strong>Configuration:</strong>
 * <p>
 * Pipelines are configured in YAML:
 * <pre>{@code
 * pipelines:
 *   get_article:
 *     preprocess:
 *       - class: io.blog.pipeline.BlogInputProcessor
 *     process:
 *       class: io.blog.pipeline.BlogExecutor
 *       DSL_QUERY:
 *         operation: SELECT
 *         source:
 *           table: articles
 *           alias: a
 *         filters:
 *           conditions:
 *             - field: a.id
 *               op: "="
 *               param: articleId
 *     postprocess:
 *       - class: io.blog.pipeline.BlogOutputProcessor
 * }</pre>
 * <p>
 * <strong>Design Patterns:</strong>
 * <ul>
 *   <li><strong>Chain of Responsibility:</strong> Sequential processing stages</li>
 *   <li><strong>Template Method:</strong> Fixed pipeline structure, extensible steps</li>
 *   <li><strong>Strategy:</strong> Pluggable processor implementations</li>
 *   <li><strong>Immutable Object:</strong> MaterializedInput/Output</li>
 * </ul>
 * <p>
 * <strong>Extensibility:</strong>
 * <p>
 * Applications can implement custom processors:
 * <ol>
 *   <li>Implement PreProcessor/Executor/PostProcessor interfaces</li>
 *   <li>Add to pipeline configuration YAML</li>
 *   <li>Framework automatically instantiates and chains them</li>
 * </ol>
 *
 * @see io.cheshire.spi.pipeline.step.PreProcessor
 * @see io.cheshire.spi.pipeline.step.Executor
 * @see io.cheshire.spi.pipeline.step.PostProcessor
 * @see io.cheshire.spi.pipeline.PipelineProcessor
 * @since 1.0.0
 */
package io.cheshire.spi.pipeline;

