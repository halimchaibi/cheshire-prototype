/*-
 * #%L
 * Cheshire :: Pipeline :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.pipeline.step;

import io.cheshire.spi.pipeline.CanonicalOutput;
import io.cheshire.spi.pipeline.Context;

/**
 * Third stage of pipeline processing - output transformation and enrichment.
 * <p>
 * <strong>Purpose:</strong>
 * <p>
 * PostProcessors refine and format output after the executor completes. They transform {@link CanonicalOutput} to
 * {@link CanonicalOutput}, preparing data for client consumption.
 * <p>
 * <strong>Common Use Cases:</strong>
 * <ul>
 * <li><strong>Formatting:</strong> Convert data to client-expected formats</li>
 * <li><strong>Filtering:</strong> Remove sensitive or unnecessary fields</li>
 * <li><strong>Pagination:</strong> Apply offset/limit to result sets</li>
 * <li><strong>Enrichment:</strong> Add computed fields or metadata</li>
 * <li><strong>Aggregation:</strong> Compute totals, averages, summaries</li>
 * <li><strong>Sorting:</strong> Order results by specified criteria</li>
 * </ul>
 * <p>
 * <strong>Functional Interface:</strong>
 * <p>
 * This is a functional interface, allowing lambda implementations:
 *
 * <pre>{@code
 * PostProcessor<MaterializedOutput> paginator = (output, ctx) -> {
 *     int offset = ctx.getOrDefault("offset", 0);
 *     int limit = ctx.getOrDefault("limit", 10);
 *     List<?> data = output.data();
 *     List<?> page = data.subList(offset, Math.min(offset + limit, data.size()));
 *     return MaterializedOutput.of(page);
 * };
 * }</pre>
 * <p>
 * <strong>Chaining:</strong>
 * <p>
 * Multiple postprocessors execute sequentially, each receiving the output of the previous one:
 *
 * <pre>
 * Executor → PostProcessor1 → PostProcessor2 → ... → PostProcessorN → Response
 * </pre>
 * <p>
 * <strong>Immutability:</strong>
 * <p>
 * Implementations should return new instances rather than mutating output.
 * <p>
 * <strong>Type Constraint:</strong>
 * <p>
 * Input and output types are the same ({@code O → O}), ensuring type safety through the postprocessing chain.
 * <p>
 * <strong>Performance Consideration:</strong>
 * <p>
 * Postprocessors run after the executor completes. For expensive operations (sorting large datasets, complex
 * calculations), consider moving logic to the executor or database layer.
 *
 * @param <O>
 *            canonical output type
 * @see PreProcessor
 * @see Executor
 * @see Step
 * @since 1.0.0
 */
@FunctionalInterface
public non-sealed interface PostProcessor<O extends CanonicalOutput<?>> extends Step<O, O> {
    /**
     * Transforms and enriches canonical output.
     *
     * @param output
     *            the output to process
     * @param ctx
     *            shared execution context
     * @return transformed output (may be the same instance if no changes needed)
     */
    O apply(O output, Context ctx);
}
