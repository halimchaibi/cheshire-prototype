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
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Verifies the stable module boundaries and SPI contracts of the Cheshire runtime. */
@AnalyzeClasses(
    packages = "io.cheshire",
    importOptions = {ImportOption.DoNotIncludeTests.class})
public class CheshireArchitectureTest {

  /** SPI modules define the base contracts and must never point at concrete runtime layers. */
  @ArchTest
  static final ArchRule spi_packages_should_not_depend_on_concrete_layers =
      noClasses()
          .that()
          .resideInAPackage("io.cheshire.spi..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "io.cheshire.core..",
              "io.cheshire.query.engine..",
              "io.cheshire.source..",
              "io.cheshire.jetty..",
              "io.cheshire.stdio..",
              "io.cheshire.runtime..")
          .because(
              "SPI packages are reusable contracts and cannot depend on core, providers, transports, or runtime");

  /** Core orchestrates abstractions through ServiceLoader and must not know concrete providers. */
  @ArchTest
  static final ArchRule core_should_not_depend_on_implementations_or_transports =
      noClasses()
          .that()
          .resideInAPackage("io.cheshire.core..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "io.cheshire.query.engine..",
              "io.cheshire.source..",
              "io.cheshire.jetty..",
              "io.cheshire.stdio..",
              "io.cheshire.runtime..")
          .because("core must remain implementation-neutral and route through SPI contracts");

  /** Query engines and source providers are plugin implementations, not transport code. */
  @ArchTest
  static final ArchRule provider_implementations_should_not_depend_on_transports_or_runtime =
      noClasses()
          .that()
          .resideInAnyPackage("io.cheshire.query.engine..", "io.cheshire.source..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("io.cheshire.jetty..", "io.cheshire.stdio..", "io.cheshire.runtime..")
          .because("query engines and source providers must stay transport-agnostic");

  /** Transports adapt protocols and delegate to core; they should not bootstrap runtime state. */
  @ArchTest
  static final ArchRule transport_packages_should_not_depend_on_runtime =
      noClasses()
          .that()
          .resideInAnyPackage("io.cheshire.jetty..", "io.cheshire.stdio..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("io.cheshire.runtime..")
          .because(
              "protocol adapters should not couple to runtime startup and shutdown orchestration");

  /** Lower layers must not depend on runtime-only classes. */
  @ArchTest
  static final ArchRule runtime_should_be_the_top_level_consumer =
      noClasses()
          .that()
          .resideOutsideOfPackage("io.cheshire.runtime..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("io.cheshire.runtime..")
          .because("runtime is the composition layer and should not leak into lower modules");

  /** Every concrete query engine must implement the query engine SPI. */
  @ArchTest
  static final ArchRule query_engines_should_implement_spi =
      classes()
          .that()
          .resideInAPackage("io.cheshire.query.engine..")
          .and()
          .haveSimpleNameEndingWith("QueryEngine")
          .and()
          .areNotInterfaces()
          .and()
          .areNotNestedClasses()
          .should()
          .implement("io.cheshire.spi.query.engine.QueryEngine")
          .because("Query engines must implement the QueryEngine SPI");

  /** Every concrete source provider must implement the source provider SPI. */
  @ArchTest
  static final ArchRule source_providers_should_implement_spi =
      classes()
          .that()
          .resideInAPackage("io.cheshire.source..")
          .and()
          .haveSimpleNameEndingWith("SourceProvider")
          .and()
          .areNotInterfaces()
          .and()
          .areNotNestedClasses()
          .should()
          .implement("io.cheshire.spi.source.SourceProvider")
          .because("Source providers must implement the SourceProvider SPI");

  /** Query engine factories are loaded through ServiceLoader and must implement the factory SPI. */
  @ArchTest
  static final ArchRule query_engine_factories_should_implement_spi =
      classes()
          .that()
          .resideInAPackage("io.cheshire.query.engine..")
          .and()
          .haveSimpleNameEndingWith("Factory")
          .and()
          .areNotInterfaces()
          .and()
          .areNotNestedClasses()
          .should()
          .implement("io.cheshire.spi.query.engine.QueryEngineFactory")
          .because(
              "Query engine factories must implement QueryEngineFactory SPI for ServiceLoader discovery");

  /**
   * Source provider factories are loaded through ServiceLoader and must implement the factory SPI.
   */
  @ArchTest
  static final ArchRule source_provider_factories_should_implement_spi =
      classes()
          .that()
          .resideInAPackage("io.cheshire.source..")
          .and()
          .haveSimpleNameEndingWith("Factory")
          .and()
          .areNotInterfaces()
          .and()
          .areNotNestedClasses()
          .should()
          .implement("io.cheshire.spi.source.SourceProviderFactory")
          .because(
              "Source provider factories must implement SourceProviderFactory SPI for ServiceLoader discovery");

  /** Classes named Exception must be throwable; domain error records are intentionally excluded. */
  @ArchTest
  static final ArchRule exception_classes_should_extend_exception =
      classes()
          .that()
          .resideInAPackage("io.cheshire..")
          .and()
          .haveSimpleNameEndingWith("Exception")
          .should()
          .beAssignableTo(Exception.class)
          .because("classes named Exception participate in the exception hierarchy");

  /** Core internals should remain acyclic at the package-slice level. */
  @ArchTest
  static final ArchRule core_packages_should_be_free_of_cycles =
      slices()
          .matching("io.cheshire.core.(*)..")
          .should()
          .beFreeOfCycles()
          .because("core package cycles make lifecycle and orchestration changes risky");

  /** Manager classes belong in manager packages. */
  @ArchTest
  static final ArchRule managers_should_reside_in_manager_package =
      classes()
          .that()
          .resideInAPackage("io.cheshire.core..")
          .and()
          .haveSimpleNameEndingWith("Manager")
          .should()
          .resideInAPackage("..manager..")
          .because("core lifecycle manager classes should be organized in manager packages");
}
