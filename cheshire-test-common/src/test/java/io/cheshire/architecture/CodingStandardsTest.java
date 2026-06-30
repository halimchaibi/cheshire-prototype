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
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Enforces focused coding standards that are stable enough for architecture tests. */
@AnalyzeClasses(
    packages = "io.cheshire",
    importOptions = {ImportOption.DoNotIncludeTests.class})
public class CodingStandardsTest {

  /** SPI modules should keep fields immutable, including record components and constants. */
  @ArchTest
  static final ArchRule spi_fields_should_be_final =
      fields()
          .that()
          .areDeclaredInClassesThat()
          .resideInAPackage("io.cheshire.spi..")
          .should()
          .beFinal()
          .because("SPI data carriers and exceptions should expose immutable state");

  /** Implementation query request objects are data carriers and should be Java records. */
  @ArchTest
  static final ArchRule implementation_query_objects_should_be_records =
      classes()
          .that()
          .resideInAnyPackage("io.cheshire.query.engine..", "io.cheshire.source..")
          .and()
          .haveSimpleNameEndingWith("Query")
          .and()
          .areNotInterfaces()
          .should()
          .beRecords()
          .because("query value objects should be immutable records");

  /** Implementation result objects are data carriers and should be Java records. */
  @ArchTest
  static final ArchRule implementation_result_objects_should_be_records =
      classes()
          .that()
          .resideInAnyPackage("io.cheshire.query.engine..", "io.cheshire.source..")
          .and()
          .haveSimpleNameEndingWith("Result")
          .and()
          .areNotInterfaces()
          .should()
          .beRecords()
          .because("result value objects should be immutable records");

  /** Public API methods should not accept null by convention; optionality must be explicit. */
  @ArchTest
  static final ArchRule public_methods_should_not_be_annotated_nullable =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .resideInAPackage("io.cheshire..")
          .and()
          .arePublic()
          .should()
          .notBeAnnotatedWith("org.jetbrains.annotations.Nullable")
          .because("public APIs should model absence with Optional or empty collections");

  /** SPI public APIs should not expose generic Throwable. */
  @ArchTest
  static final ArchRule spi_public_methods_should_not_declare_throwable =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .resideInAPackage("io.cheshire.spi..")
          .and()
          .arePublic()
          .should()
          .notDeclareThrowableOfType(Throwable.class)
          .because("SPI APIs should throw domain-specific checked exceptions");
}
