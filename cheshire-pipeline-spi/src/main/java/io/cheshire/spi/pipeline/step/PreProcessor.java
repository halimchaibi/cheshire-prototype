package io.cheshire.spi.pipeline.step;

import io.cheshire.spi.pipeline.Canonical;
import io.cheshire.spi.pipeline.CanonicalInput;
import io.cheshire.spi.pipeline.Context;

/**
 * First stage of pipeline processing - input transformation and validation.
 * <p>
 * <strong>Purpose:</strong>
 * <p>
 * PreProcessors prepare and validate input before it reaches the executor.
 * They transform {@link CanonicalInput} to {@link CanonicalInput}, enriching
 * or normalizing data as needed.
 * <p>
 * <strong>Common Use Cases:</strong>
 * <ul>
 *   <li><strong>Validation:</strong> Check required fields, data types, constraints</li>
 *   <li><strong>Normalization:</strong> Standardize formats (dates, currencies, etc.)</li>
 *   <li><strong>Enrichment:</strong> Add derived fields or lookup data</li>
 *   <li><strong>Security:</strong> Validate permissions, sanitize inputs</li>
 *   <li><strong>Transformation:</strong> Convert between data representations</li>
 * </ul>
 * <p>
 * <strong>Functional Interface:</strong>
 * <p>
 * This is a functional interface, allowing lambda implementations:
 * <pre>{@code
 * PreProcessor<MaterializedInput> validator = (input, ctx) -> {
 *     if (input.data().get("email") == null) {
 *         throw new ValidationException("Email required");
 *     }
 *     return input;
 * };
 * }</pre>
 * <p>
 * <strong>Chaining:</strong>
 * <p>
 * Multiple preprocessors execute sequentially, each receiving the output
 * of the previous one:
 * <pre>
 * Input → PreProcessor1 → PreProcessor2 → ... → PreProcessorN → Executor
 * </pre>
 * <p>
 * <strong>Immutability:</strong>
 * <p>
 * Implementations should return new instances rather than mutating input.
 * <p>
 * <strong>Type Constraint:</strong>
 * <p>
 * Input and output types are the same ({@code I → I}), ensuring type safety
 * through the preprocessing chain.
 *
 * @param <I> canonical input type
 * @see Executor
 * @see PostProcessor
 * @see Step
 * @since 1.0.0
 */
@FunctionalInterface
public non-sealed interface PreProcessor<I extends CanonicalInput<? extends Canonical<?>>> extends Step<I, I> {
    /**
     * Transforms and validates canonical input.
     *
     * @param input the input to process
     * @param ctx   shared execution context
     * @return transformed input (may be the same instance if no changes needed)
     */
    I apply(I input, Context ctx);
}
