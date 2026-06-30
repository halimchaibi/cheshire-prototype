/*-
 * #%L
 * Cheshire :: Test Common
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Verifies the three-stage pipeline SPI and its core materialized data carriers. */
@AnalyzeClasses(
    packages = "io.cheshire",
    importOptions = {ImportOption.DoNotIncludeTests.class})
public class PipelineArchitectureTest {

  /** PreProcessor implementations transform input and should advertise that in their names. */
  @ArchTest
  static final ArchRule pre_processors_should_follow_naming_convention =
      classes()
          .that()
          .implement("io.cheshire.spi.pipeline.step.PreProcessor")
          .and()
          .areNotInterfaces()
          .should()
          .haveSimpleNameEndingWith("Processor")
          .orShould()
          .haveSimpleNameEndingWith("Validator")
          .because("PreProcessor implementations should follow naming conventions");

  /** Executor implementations contain the active pipeline step and should be named as such. */
  @ArchTest
  static final ArchRule executors_should_follow_naming_convention =
      classes()
          .that()
          .implement("io.cheshire.spi.pipeline.step.Executor")
          .and()
          .areNotInterfaces()
          .should()
          .haveSimpleNameEndingWith("Executor")
          .orShould()
          .haveSimpleNameEndingWith("Handler")
          .orShould()
          .haveSimpleNameEndingWith("Worker")
          .because("Executor implementations should follow naming conventions");

  /** PostProcessor implementations transform output and should advertise that in their names. */
  @ArchTest
  static final ArchRule post_processors_should_follow_naming_convention =
      classes()
          .that()
          .implement("io.cheshire.spi.pipeline.step.PostProcessor")
          .and()
          .areNotInterfaces()
          .should()
          .haveSimpleNameEndingWith("Processor")
          .orShould()
          .haveSimpleNameEndingWith("Formatter")
          .orShould()
          .haveSimpleNameEndingWith("Enricher")
          .because("PostProcessor implementations should follow naming conventions");

  /** Step defines the single generic apply(input, context) pipeline contract. */
  @ArchTest
  static final ArchRule step_apply_method_should_accept_context =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .haveFullyQualifiedName("io.cheshire.spi.pipeline.step.Step")
          .and()
          .haveName("apply")
          .should()
          .haveRawParameterTypes("java.lang.Object", "io.cheshire.spi.pipeline.Context")
          .because("Step.apply() must accept a stage value and Context");

  /** Pipeline stage interfaces must remain lambda-friendly functional interfaces. */
  @ArchTest
  static final ArchRule pipeline_stage_interfaces_should_be_functional_interfaces =
      classes()
          .that()
          .resideInAPackage("io.cheshire.spi.pipeline.step..")
          .and()
          .areInterfaces()
          .and()
          .haveSimpleNameEndingWith("Processor")
          .or()
          .haveSimpleName("Executor")
          .should()
          .beAnnotatedWith(FunctionalInterface.class)
          .because("pipeline stages should remain easy to compose as lambdas");

  /** Materialized data carriers must stay immutable records. */
  @ArchTest
  static final ArchRule materialized_classes_should_be_records =
      classes()
          .that()
          .haveSimpleNameStartingWith("Materialized")
          .and()
          .resideInAPackage("io.cheshire.core.pipeline..")
          .should()
          .beRecords()
          .because("materialized input/output must be immutable records");

  /** Pipeline step implementations owned by the framework live in core pipeline packages. */
  @ArchTest
  static final ArchRule framework_pipeline_implementations_should_be_in_pipeline_packages =
      classes()
          .that()
          .resideInAPackage("io.cheshire.core..")
          .and()
          .implement("io.cheshire.spi.pipeline.step.PreProcessor")
          .or()
          .implement("io.cheshire.spi.pipeline.step.Executor")
          .or()
          .implement("io.cheshire.spi.pipeline.step.PostProcessor")
          .should()
          .resideInAPackage("..pipeline..")
          .because("Pipeline implementations should be organized in pipeline packages");

  /** The pipeline orchestrator is immutable and keeps stage lists defensively copied. */
  @ArchTest
  static final ArchRule pipeline_processor_should_be_record =
      classes()
          .that()
          .haveFullyQualifiedName("io.cheshire.spi.pipeline.PipelineProcessor")
          .should()
          .beRecords()
          .because("PipelineProcessor should remain an immutable orchestration record");
}
