/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.pipeline;

import io.cheshire.core.config.PipelineConfig;
import io.cheshire.spi.pipeline.*;
import io.cheshire.spi.pipeline.step.Executor;
import io.cheshire.spi.pipeline.step.PostProcessor;
import io.cheshire.spi.pipeline.step.PreProcessor;
import io.cheshire.spi.pipeline.step.Step;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory for constructing three-stage processing pipelines from configuration.
 * <p>
 * <strong>Pipeline Architecture:</strong>
 * <p>
 * This factory builds {@link PipelineProcessor} instances from {@link PipelineConfig} definitions. Each pipeline
 * consists of three stages:
 *
 * <pre>
 * [PreProcessors] → [Executor] → [PostProcessors]
 * </pre>
 * <p>
 * <strong>Stage Responsibilities:</strong>
 * <ol>
 * <li><strong>PreProcessors:</strong> Input validation, transformation, enrichment (0-N steps)</li>
 * <li><strong>Executor:</strong> Core business logic, query execution (exactly 1 step)</li>
 * <li><strong>PostProcessors:</strong> Output formatting, filtering, pagination (0-N steps)</li>
 * </ol>
 * <p>
 * <strong>Reflection-Based Instantiation:</strong>
 * <p>
 * The factory uses reflection to instantiate pipeline step implementations from fully qualified class names specified
 * in configuration. It attempts two instantiation strategies:
 * <ol>
 * <li><strong>Config Constructor:</strong> {@code Constructor(Map<String, Object>)} - Allows steps to receive
 * configuration (template, name, etc.)</li>
 * <li><strong>Default Constructor:</strong> {@code Constructor()} - Fallback for simple stateless steps</li>
 * </ol>
 * <p>
 * <strong>Configuration Injection:</strong>
 * <p>
 * If a step provides a {@code Map<String, Object>} constructor, the factory injects:
 * <ul>
 * <li><strong>template:</strong> Query template or processing template</li>
 * <li><strong>name:</strong> Step identifier for logging/debugging</li>
 * </ul>
 * <p>
 * <strong>Example Usage:</strong>
 *
 * <pre>{@code
 * PipelineConfig config = loadFromYaml("blog-pipelines.yaml");
 * PipelineProcessor<MaterializedInput, MaterializedOutput> pipeline = PipelineFactory.build("createAuthor", config);
 *
 * // Execute pipeline
 * MaterializedOutput result = pipeline.process(input);
 * }</pre>
 * <p>
 * <strong>Error Handling:</strong>
 * <ul>
 * <li><strong>ClassNotFoundException:</strong> Implementation class not in classpath</li>
 * <li><strong>RuntimeException:</strong> Instantiation failed (constructor issues, etc.)</li>
 * </ul>
 *
 * @see PipelineProcessor
 * @see PipelineConfig
 * @see PreProcessor
 * @see Executor
 * @see PostProcessor
 * @since 1.0.0
 */
public interface PipelineFactory {

    /**
     * Builds a complete pipeline from configuration.
     * <p>
     * <strong>Construction Process:</strong>
     * <ol>
     * <li>Extract pipeline steps from config (preprocess, process, postprocess)</li>
     * <li>Instantiate each step via reflection</li>
     * <li>Assemble into a {@link PipelineProcessor}</li>
     * </ol>
     * <p>
     * <strong>Type Safety:</strong> The factory ensures type compatibility by casting steps to their appropriate
     * interfaces ({@link PreProcessor}, {@link Executor}, {@link PostProcessor}).
     *
     * @param name
     *            pipeline identifier for logging and debugging
     * @param config
     *            pipeline configuration with step definitions
     * @return fully constructed pipeline processor
     * @throws RuntimeException
     *             if any step fails to instantiate
     */
    @SuppressWarnings("unchecked")
    static <I extends CanonicalInput<?>, O extends CanonicalOutput<?>> PipelineProcessor<I, O> build(String name,
            PipelineConfig config) throws ClassNotFoundException {

        // Ensure to load the classes during bootstrap, fail fast and throw ClassNotFoundException if they are not found
        Class<I> inputClass = (Class<I>) Class.forName(config.getInput());
        Class<O> outputClass = (Class<O>) Class.forName(config.getOutput());

        var steps = config.getPipeline();

        List<PreProcessor<I>> preProcessors = steps.getPreprocess().stream()
                .map(step -> (PreProcessor<I>) instantiate(name, step, PreProcessor.class)).toList();

        Executor<I, O> executor = (Executor<I, O>) instantiate(name, steps.getProcess(), Executor.class);

        List<PostProcessor<O>> postProcessors = steps.getPostprocess().stream()
                .map(step -> (PostProcessor<O>) instantiate(name, step, PostProcessor.class)).toList();

        return new PipelineProcessor<>(name, inputClass, outputClass, preProcessors, executor, postProcessors);
    }

    /**
     * Instantiates a pipeline step from configuration using reflection.
     * <p>
     * <strong>Instantiation Strategy:</strong>
     * <ol>
     * <li>Load class by fully qualified name</li>
     * <li>Try {@code Constructor(Map<String, Object>)} for config injection</li>
     * <li>Fallback to {@code Constructor()} if config constructor not found</li>
     * <li>Cast to target type and return</li>
     * </ol>
     * <p>
     * <strong>Configuration Map:</strong> Contains:
     * <ul>
     * <li><strong>template:</strong> Query/processing template (or "no template provided")</li>
     * <li><strong>name:</strong> Step name (or "no name provided")</li>
     * </ul>
     *
     * @param step
     *            configuration for the step to instantiate
     * @param targetType
     *            expected interface type (PreProcessor, Executor, PostProcessor)
     * @param <T>
     *            the target step type
     * @return instantiated and configured step
     * @throws RuntimeException
     *             if class not found or instantiation fails
     */
    private static <T> T instantiate(String pipelineName, PipelineConfig.Step step, Class<T> targetType) {
        final String impl = step.getImplementation();
        final String stepName = Optional.ofNullable(step.getName()).orElse("unnamed");

        final Map<String, Object> config = Map.of("template", Optional.ofNullable(step.getTemplate()).orElse(""),
                "name", stepName);

        try {
            Class<?> implementationClass = Class.forName(impl);

            if (!targetType.isAssignableFrom(implementationClass)) {
                throw new IllegalArgumentException(
                        "Class %s does not implement %s".formatted(impl, targetType.getSimpleName()));
            }

            try {
                return targetType.cast(implementationClass.getConstructor(Map.class).newInstance(config));
            } catch (NoSuchMethodException e) {
                // Fallback: The step must handle its own config or doesn't need any
                return targetType.cast(implementationClass.getDeclaredConstructor().newInstance());
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Pipeline '%s' step '%s': Failed to initialize %s".formatted(pipelineName, stepName, impl), e);
        }
    }

    /**
     * Reflective instantiation wrapped in a functional style.
     */
    private static <T extends Step<?, ?>> T instantiate2(PipelineConfig.Step configStep, Class<T> targetType) {
        final String impl = configStep.getImplementation();
        final String stepName = Optional.ofNullable(configStep.getName()).orElse("unnamed");
        try {
            T instance = targetType.cast(Class.forName(impl).getDeclaredConstructor().newInstance());

            Map<String, Object> configMap = new HashMap<>();
            configMap.put("template", configStep.getTemplate());
            configMap.put("name", stepName);

            // "Bake" the configuration into the instance
            // instance.configure(configMap);

            return instance;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Step '%s': Implementation class not found: %s".formatted(stepName, impl), e);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Step '%s': Failed to assemble step '%s' - %s".formatted(stepName, impl, e.getMessage()), e);
        }
    }
}
