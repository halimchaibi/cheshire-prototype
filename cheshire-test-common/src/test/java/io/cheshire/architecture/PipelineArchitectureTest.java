package io.cheshire.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * Architecture tests specific to the Three-Stage Pipeline pattern.
 * <p>
 * <strong>Three-Stage Pipeline Pattern:</strong>
 * <pre>
 * MaterializedInput
 *      ↓
 * PreProcessor (validate/transform)
 *      ↓
 * Executor (business logic)
 *      ↓
 * PostProcessor (format/enrich)
 *      ↓
 * MaterializedOutput
 * </pre>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *   <li><strong>Separation of Concerns:</strong> Each stage has distinct responsibility</li>
 *   <li><strong>Composability:</strong> Stages can be chained via stream reduction</li>
 *   <li><strong>Immutability:</strong> MaterializedInput/Output are immutable</li>
 *   <li><strong>Type Safety:</strong> Strong typing prevents incorrect stage ordering</li>
 * </ul>
 *
 * @since 1.0.0
 */
@AnalyzeClasses(
        packages = "io.cheshire",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
public class PipelineArchitectureTest {

    // ============================================
    // Pipeline Processor Implementation Rules
    // ============================================

    /**
     * Ensures PreProcessor implementations follow naming conventions.
     * <p>
     * <strong>Pattern:</strong> Classes implementing PreProcessor should indicate
     * their purpose through naming:
     * <ul>
     *   <li>*InputProcessor</li>
     *   <li>*PreProcessor</li>
     *   <li>*Validator</li>
     * </ul>
     */
    @ArchTest
    static final ArchRule preProcessors_Should_Follow_Naming_Convention =
            classes().that().implement("io.cheshire.spi.pipeline.step.PreProcessor")
                    .and().areNotInterfaces()
                    .should().haveSimpleNameEndingWith("Processor")
                    .orShould().haveSimpleNameEndingWith("Validator")
                    .because("PreProcessor implementations should follow naming conventions");

    /**
     * Ensures Executor implementations follow naming conventions.
     * <p>
     * <strong>Pattern:</strong> Classes implementing Executor should clearly indicate
     * execution responsibility:
     * <ul>
     *   <li>*Executor</li>
     *   <li>*Handler</li>
     *   <li>*Worker</li>
     * </ul>
     */
    @ArchTest
    static final ArchRule executors_Should_Follow_Naming_Convention =
            classes().that().implement("io.cheshire.spi.pipeline.step.Executor")
                    .and().areNotInterfaces()
                    .should().haveSimpleNameEndingWith("Executor")
                    .orShould().haveSimpleNameEndingWith("Handler")
                    .orShould().haveSimpleNameEndingWith("Worker")
                    .because("Executor implementations should follow naming conventions");

    /**
     * Ensures PostProcessor implementations follow naming conventions.
     * <p>
     * <strong>Pattern:</strong> Classes implementing PostProcessor should indicate
     * output transformation:
     * <ul>
     *   <li>*OutputProcessor</li>
     *   <li>*PostProcessor</li>
     *   <li>*Formatter</li>
     *   <li>*Enricher</li>
     * </ul>
     */
    @ArchTest
    static final ArchRule postProcessors_Should_Follow_Naming_Convention =
            classes().that().implement("io.cheshire.spi.pipeline.step.PostProcessor")
                    .and().areNotInterfaces()
                    .should().haveSimpleNameEndingWith("Processor")
                    .orShould().haveSimpleNameEndingWith("Formatter")
                    .orShould().haveSimpleNameEndingWith("Enricher")
                    .because("PostProcessor implementations should follow naming conventions");

    // ============================================
    // Pipeline Method Signature Rules
    // ============================================

    /**
     * Ensures PreProcessor process methods accept MaterializedInput.
     * <p>
     * <strong>Contract:</strong>
     * <pre>{@code
     * MaterializedInput process(MaterializedInput input);
     * }</pre>
     */
    @ArchTest
    static final ArchRule preProcessor_Process_Methods_Should_Accept_MaterializedInput =
            methods().that().areDeclaredInClassesThat()
                    .implement("io.cheshire.spi.pipeline.step.PreProcessor")
                    .and().haveName("process")
                    .should().haveRawParameterTypes("io.cheshire.spi.pipeline.MaterializedInput")
                    .because("PreProcessor.process() must accept MaterializedInput");

    /**
     * Ensures Executor execute methods accept MaterializedInput and SessionTask.
     * <p>
     * <strong>Contract:</strong>
     * <pre>{@code
     * MaterializedOutput execute(MaterializedInput input, SessionTask task);
     * }</pre>
     */
    @ArchTest
    static final ArchRule executor_Execute_Methods_Should_Accept_Correct_Parameters =
            methods().that().areDeclaredInClassesThat()
                    .implement("io.cheshire.spi.pipeline.step.Executor")
                    .and().haveName("execute")
                    .should().haveRawParameterTypes(
                            "io.cheshire.spi.pipeline.MaterializedInput",
                            "io.cheshire.core.task.SessionTask"
                    )
                    .because("Executor.execute() must accept MaterializedInput and SessionTask");

    /**
     * Ensures PostProcessor process methods accept MaterializedOutput.
     * <p>
     * <strong>Contract:</strong>
     * <pre>{@code
     * MaterializedOutput process(MaterializedOutput output);
     * }</pre>
     */
    @ArchTest
    static final ArchRule postProcessor_Process_Methods_Should_Accept_MaterializedOutput =
            methods().that().areDeclaredInClassesThat()
                    .implement("io.cheshire.spi.pipeline.step.PostProcessor")
                    .and().haveName("process")
                    .should().haveRawParameterTypes("io.cheshire.spi.pipeline.MaterializedOutput")
                    .because("PostProcessor.process() must accept MaterializedOutput");

    // ============================================
    // Pipeline Immutability Rules
    // ============================================

    /**
     * Ensures MaterializedInput/Output are immutable records.
     * <p>
     * <strong>Rationale:</strong> Pipeline data carriers must be immutable to ensure:
     * <ul>
     *   <li>Thread safety</li>
     *   <li>Predictable transformations</li>
     *   <li>Easy testing</li>
     * </ul>
     */
    @ArchTest
    static final ArchRule materialized_Classes_Should_Be_Records =
            classes().that().haveSimpleNameStartingWith("Materialized")
                    .and().resideInAPackage("..pipeline..")
                    .should().beRecords()
                    .because("Materialized input/output should be immutable records");

    // ============================================
    // Pipeline Package Organization
    // ============================================

    /**
     * Ensures pipeline step implementations are properly organized.
     * <p>
     * <strong>Package Structure:</strong>
     * <pre>
     * io.application.pipeline
     *   ├─ MyInputProcessor (PreProcessor)
     *   ├─ MyExecutor (Executor)
     *   └─ MyOutputProcessor (PostProcessor)
     * </pre>
     */
    @ArchTest
    static final ArchRule pipeline_Implementations_Should_Be_In_Pipeline_Packages =
            classes().that().implement("io.cheshire.spi.pipeline.step.PreProcessor")
                    .or().implement("io.cheshire.spi.pipeline.step.Executor")
                    .or().implement("io.cheshire.spi.pipeline.step.PostProcessor")
                    .should().resideInAPackage("..pipeline..")
                    .because("Pipeline implementations should be organized in pipeline packages");

    /**
     * Ensures PipelineProcessor orchestrator doesn't leak into application code.
     * <p>
     * <strong>Rationale:</strong> PipelineProcessor is a framework internal class
     * that orchestrates the three stages. Application code should only implement
     * individual stage interfaces (PreProcessor, Executor, PostProcessor).
     */
    @ArchTest
    static final ArchRule applications_Should_Not_Implement_PipelineProcessor =
            classes().that().resideOutsideOfPackage("io.cheshire.core..")
                    .should().notImplement("io.cheshire.spi.pipeline.PipelineProcessor")
                    .because("Applications should implement stage interfaces, not PipelineProcessor");
}

