package io.cheshire.spi.pipeline.step;

import io.cheshire.spi.pipeline.CanonicalInput;
import io.cheshire.spi.pipeline.CanonicalOutput;

/**
 * Second stage of pipeline processing - core business logic execution.
 * <p>
 * <strong>Purpose:</strong>
 * <p>
 * The Executor is the heart of the pipeline, responsible for executing the
 * primary business operation. It transforms {@link CanonicalInput} into
 * {@link CanonicalOutput}, typically by:
 * <ul>
 *   <li>Executing database queries</li>
 *   <li>Calling external APIs</li>
 *   <li>Performing computations</li>
 *   <li>Orchestrating business workflows</li>
 * </ul>
 * <p>
 * <strong>Single Executor Per Pipeline:</strong>
 * <p>
 * Unlike preprocessors and postprocessors (which can be chained), each
 * pipeline has exactly ONE executor. This enforces a clear separation of
 * concerns:
 * <pre>
 * [PreProcessors] → [Executor] → [PostProcessors]
 *    (0-N)            (1)            (0-N)
 * </pre>
 * <p>
 * <strong>Functional Interface:</strong>
 * <p>
 * This is a functional interface, allowing lambda implementations:
 * <pre>{@code
 * Executor<MaterializedInput, MaterializedOutput> queryExecutor =
 *     (input, ctx) -> {
 *         String sql = buildQuery(input);
 *         List<Map<String, Object>> results = queryEngine.execute(sql);
 *         return MaterializedOutput.of(results);
 *     };
 * }</pre>
 * <p>
 * <strong>Type Transformation:</strong>
 * <p>
 * Unlike preprocessors/postprocessors which preserve types ({@code I → I},
 * {@code O → O}), executors transform types ({@code I → O}), reflecting
 * the fundamental business transformation.
 * <p>
 * <strong>Example Implementations:</strong>
 * <ul>
 *   <li><strong>JDBC Executor:</strong> Executes SQL queries against databases</li>
 *   <li><strong>Calcite Executor:</strong> Executes federated queries</li>
 *   <li><strong>API Executor:</strong> Calls REST/GraphQL endpoints</li>
 *   <li><strong>Computation Executor:</strong> Performs calculations/transformations</li>
 * </ul>
 * <p>
 * <strong>Error Handling:</strong>
 * <p>
 * Executors should throw {@link io.cheshire.spi.pipeline.exception.PipelineException}
 * or its subclasses for business logic failures. These exceptions are caught
 * by the dispatcher and converted to appropriate error responses.
 *
 * @param <I> canonical input type
 * @param <O> canonical output type
 * @see PreProcessor
 * @see PostProcessor
 * @see Step
 * @since 1.0.0
 */
@FunctionalInterface
public non-sealed interface Executor<I extends CanonicalInput<?>, O extends CanonicalOutput<?>> extends Step<I, O> {
}
