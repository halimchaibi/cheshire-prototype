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

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Security-focused architecture rules for production-safe framework code. */
@AnalyzeClasses(
    packages = "io.cheshire",
    importOptions = {ImportOption.DoNotIncludeTests.class})
public class SecurityArchitectureTest {

  /** Production code should use structured logging rather than writing to process streams. */
  @ArchTest
  static final ArchRule production_code_should_not_write_to_stdout =
      noClasses()
          .that()
          .resideInAPackage("io.cheshire..")
          .should()
          .accessField(System.class, "out")
          .because(
              "framework code should use SLF4J or explicit response payloads instead of System.out");

  /** Production code should use structured logging rather than writing to process error streams. */
  @ArchTest
  static final ArchRule production_code_should_not_write_to_stderr =
      noClasses()
          .that()
          .resideInAPackage("io.cheshire..")
          .should()
          .accessField(System.class, "err")
          .because(
              "framework code should use SLF4J or explicit response payloads instead of System.err");

  /** Source provider configuration records may carry credentials and must remain immutable. */
  @ArchTest
  static final ArchRule source_provider_configs_should_be_records =
      classes()
          .that()
          .resideInAPackage("io.cheshire.source..")
          .and()
          .haveSimpleNameEndingWith("Config")
          .should()
          .beRecords()
          .because("source provider configs can contain connection data and must be immutable");

  /** Query engine configuration records must keep engine setup immutable after construction. */
  @ArchTest
  static final ArchRule query_engine_configs_should_be_records =
      classes()
          .that()
          .resideInAPackage("io.cheshire.query.engine..")
          .and()
          .haveSimpleNameEndingWith("Config")
          .should()
          .beRecords()
          .because("query engine configs must be immutable after validation");
}
