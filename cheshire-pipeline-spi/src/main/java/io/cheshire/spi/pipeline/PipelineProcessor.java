/*-
 * #%L
 * Cheshire :: Pipeline :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.pipeline;

import io.cheshire.spi.pipeline.exception.PipelineException;
import io.cheshire.spi.pipeline.step.Executor;
import io.cheshire.spi.pipeline.step.PostProcessor;
import io.cheshire.spi.pipeline.step.PreProcessor;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

/**
 * Orchestrates three-stage pipeline execution for action processing.
 *
 * <p><strong>Pipeline Architecture:</strong>
 *
 * <pre>
 * Input → [PreProcessors] → [Executor] → [PostProcessors] → Output
 * </pre>
 *
 * <p><strong>Stage Semantics:</strong>
 *
 * <ol>
 *   <li><strong>PreProcessors (0-N):</strong> Transform and validate input sequentially
 *   <li><strong>Executor (1):</strong> Execute core business logic (query, computation)
 *   <li><strong>PostProcessors (0-N):</strong> Format and enrich output sequentially
 * </ol>
 *
 * <p><strong>Execution Model:</strong>
 *
 * <ul>
 *   <li><strong>Sequential Processing:</strong> Steps execute in order within each stage
 *   <li><strong>Stream Reduction:</strong> Uses {@link java.util.stream.Stream#reduce} for chaining
 *   <li><strong>Context Propagation:</strong> Shared {@link Context} flows through all steps
 *   <li><strong>Immutable Transformations:</strong> Each step returns new instances
 * </ul>
 *
 * <p><strong>Async Execution:</strong>
 *
 * <p>The {@link #executeAsync(CanonicalInput, Context)} method leverages Java's virtual threads for
 * non-blocking pipeline execution. This is experimental and subject to change.
 *
 * <p><strong>Error Handling:</strong>
 *
 * <p>Any {@link PipelineException} thrown during execution propagates immediately, short-circuiting
 * the pipeline. The exception contains context about which stage failed.
 *
 * <p><strong>Type Safety:</strong>
 *
 * <p>Generic bounds ensure compile-time type safety:
 *
 * <ul>
 *   <li>{@code I extends CanonicalInput<?>} - Input must be canonical
 *   <li>{@code O extends CanonicalOutput<?>} - Output must be canonical
 * </ul>
 *
 * @param <I> input type extending CanonicalInput
 * @param <O> output type extending CanonicalOutput
 * @param name pipeline identifier for logging and debugging
 * @param preProcessors list of preprocessing steps (may be empty)
 * @param executor core execution step (required)
 * @param postProcessors list of postprocessing steps (may be empty)
 * @see PreProcessor
 * @see Executor
 * @see PostProcessor
 * @see Context
 * @since 1.0.0
 */
public record PipelineProcessor<I extends CanonicalInput<?>, O extends CanonicalOutput<?>>(
    String name,
    Class<I> inputClass,
    Class<O> outputClass,
    List<PreProcessor<I>> preProcessors,
    Executor<I, O> executor,
    List<PostProcessor<O>> postProcessors) {

  /**
   * Compact constructor to prevent EI_EXPOSE_REP Mitigate CWE-374 - Defensively copied via
   * List.copyOf")
   */
  public PipelineProcessor {
    preProcessors = List.copyOf(preProcessors);
    postProcessors = List.copyOf(postProcessors);
  }

  /**
   * Executes the complete pipeline synchronously.
   *
   * <p><strong>Execution Flow:</strong>
   *
   * <ol>
   *   <li>Record pipeline start time in context
   *   <li>Apply all preprocessors sequentially via stream reduction
   *   <li>Execute core business logic via executor
   *   <li>Apply all postprocessors sequentially via stream reduction
   *   <li>Return final output
   * </ol>
   *
   * <p><strong>Stream Reduction Pattern:</strong>
   *
   * <pre>{@code
   * I processedInput = preProcessors.stream().reduce(input, (in, processor) -> processor.apply(in, ctx), (a, b) -> b);
   * }</pre>
   *
   * The combiner {@code (a, b) -> b} is unused in sequential streams but required by API.
   *
   * <p><strong>Error Propagation:</strong> Any {@link PipelineException} thrown by a step
   * immediately terminates execution and propagates to the caller.
   *
   * @param input initial canonical input
   * @param ctx shared execution context for state and metadata
   * @return final canonical output after all stages
   * @throws PipelineException if any step fails
   */
  public O execute(I input, Context ctx) throws PipelineException {

    ctx.putIfAbsent("pipeline-processor-at", Instant.now().toString());

    // TODO: no need for reduce here - can be replaced with simple for-loop for better readability,
    // but kept for
    // future use cases.
    I processedInput =
        preProcessors.stream()
            .reduce(input, (in, processor) -> processor.apply(in, ctx), (a, b) -> b);

    // Execute main processing
    O output = executor.apply(processedInput, ctx);

    // Apply post-processors
    return postProcessors.stream()
        .reduce(output, (out, processor) -> processor.apply(out, ctx), (a, b) -> b);
  }

  // TODO : Review virtual thread usage and its implications

  /**
   * Executes the pipeline asynchronously using virtual threads.
   *
   * <p><b>Note:</b> This method is currently <b>experimental</b>. The resource management of the
   * underlying executor is subject to change.
   *
   * @param input the initial canonical input
   * @param ctx the shared execution context
   * @return a CompletableFuture representing the pending result of the pipeline
   */
  public CompletableFuture<O> executeAsync(I input, Context ctx) {
    try (var vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
      return CompletableFuture.supplyAsync(
          () -> {
            try {
              return execute(input, ctx);
            } catch (PipelineException e) {
              throw new CompletionException(e);
            }
          },
          vThreadExecutor);
    }
  }
}
