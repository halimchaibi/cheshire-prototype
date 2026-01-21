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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture tests for Cheshire coding standards and best practices.
 *
 * <p><strong>Coding Philosophy:</strong>
 *
 * <p>Cheshire follows a **Scala-influenced functional Java** approach:
 *
 * <ul>
 *   <li><strong>Immutability by default:</strong> All fields final, prefer records
 *   <li><strong>No nulls:</strong> Use Optional&lt;T&gt; for optional values
 *   <li><strong>Declarative code:</strong> Prefer Streams API over imperative loops
 *   <li><strong>Expressions over statements:</strong> Switch expressions, ternary operators
 *   <li><strong>Pure functions:</strong> Push side effects to boundaries
 * </ul>
 *
 * <p><strong>Java 21 Features:</strong>
 *
 * <ul>
 *   <li>Records for immutable data carriers
 *   <li>Sealed interfaces for restricted hierarchies
 *   <li>Pattern matching for type-safe operations
 *   <li>Virtual Threads for massive concurrency
 *   <li>Sequenced Collections for modern collection ops
 * </ul>
 *
 * @see <a href=".cursorrules">Cheshire Coding Standards</a>
 * @since 1.0.0
 */
@AnalyzeClasses(
    packages = "io.cheshire",
    importOptions = {ImportOption.DoNotIncludeTests.class})
public class CodingStandardsTest {

  // ============================================
  // Immutability Rules
  // ============================================

  /**
   * Enforces immutability by requiring fields to be final.
   *
   * <p><strong>Rationale:</strong> Immutable objects are:
   *
   * <ul>
   *   <li>Thread-safe without synchronization
   *   <li>Easier to reason about
   *   <li>Cacheable
   *   <li>Suitable for use as map keys
   * </ul>
   *
   * <p><strong>Exception:</strong> Some framework internals may require mutability for performance
   * (e.g., AtomicReference for state machines).
   */
  @ArchTest
  static final ArchRule fields_Should_Be_Final_For_Immutability =
      fields()
          .that()
          .areDeclaredInClassesThat()
          .resideInAPackage("io.cheshire..")
          .and()
          .areNotStatic()
          .and()
          .arePrivate()
          .should()
          .beFinal()
          .orShould()
          .haveRawType("java.util.concurrent.atomic.AtomicReference")
          .orShould()
          .haveRawType("java.util.concurrent.atomic.AtomicBoolean")
          .orShould()
          .haveRawType("java.util.concurrent.atomic.AtomicInteger")
          .orShould()
          .haveRawType("java.util.concurrent.atomic.AtomicLong")
          .because(
              "Fields should be final for immutability unless using atomic types for thread-safe state");

  /**
   * Ensures data transfer objects use records.
   *
   * <p><strong>Pattern:</strong> DTOs, value objects, and data carriers should be records:
   *
   * <ul>
   *   <li>Automatic equals/hashCode/toString
   *   <li>Compact constructor for validation
   *   <li>Pattern matching support
   *   <li>Destructuring capabilities
   * </ul>
   */
  @ArchTest
  static final ArchRule value_Objects_Should_Be_Records =
      classes()
          .that()
          .resideInAPackage("io.cheshire..")
          .and()
          .haveSimpleNameEndingWith("DTO")
          .or()
          .haveSimpleNameEndingWith("Value")
          .or()
          .haveSimpleNameEndingWith("Data")
          .should()
          .beRecords()
          .because("Value objects should be immutable records");

  // ============================================
  // Null Safety Rules
  // ============================================

  /**
   * Encourages use of Optional instead of null returns.
   *
   * <p><strong>Anti-Pattern:</strong>
   *
   * <pre>{@code
   * public User findUser(String id) {
   *     return null; // Caller must remember null check ✗
   * }
   * }</pre>
   *
   * <p><strong>Preferred Pattern:</strong>
   *
   * <pre>{@code
   * public Optional<User> findUser(String id) {
   *     return Optional.ofNullable(userMap.get(id)); // Explicit optionality ✓
   * }
   * }</pre>
   */
  @ArchTest
  static final ArchRule finder_Methods_Should_Return_Optional =
      methods()
          .that()
          .haveName("find")
          .or()
          .haveNameStartingWith("find")
          .or()
          .haveNameStartingWith("get")
          .and()
          .areDeclaredInClassesThat()
          .resideInAPackage("io.cheshire..")
          .and()
          .arePublic()
          .should()
          .haveRawReturnType("java.util.Optional")
          .orShould()
          .haveRawReturnType("java.util.List")
          .orShould()
          .haveRawReturnType("java.util.Set")
          .orShould()
          .haveRawReturnType("java.util.Map")
          .orShould()
          .haveRawReturnType(void.class)
          .because("Finder methods should return Optional instead of nullable references");

  // ============================================
  // Exception Handling Rules
  // ============================================

  /**
   * Discourages generic catch-all exception handling.
   *
   * <p><strong>Anti-Pattern:</strong>
   *
   * <pre>{@code
   * try {
   *     // operation
   * } catch (Exception e) { // Too broad ✗
   *     // handle
   * }
   * }</pre>
   *
   * <p><strong>Better:</strong>
   *
   * <pre>{@code
   * try {
   *     // operation
   * } catch (SpecificException e) { // Specific ✓
   *     // handle
   * }
   * }</pre>
   */
  @ArchTest
  static final ArchRule exceptions_Should_Not_Be_Generic =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .resideInAPackage("io.cheshire..")
          .should()
          .notDeclareThrowableOfType(Exception.class)
          .orShould()
          .notDeclareThrowableOfType(Throwable.class)
          .because("Methods should throw specific exceptions, not generic Exception");

  // ============================================
  // Java 21 Features Usage
  // ============================================

  /**
   * Encourages use of Records for data classes.
   *
   * <p><strong>Java 21 Feature:</strong> Records provide concise syntax for immutable data carriers
   * with built-in equals/hashCode/toString.
   */
  @ArchTest
  static final ArchRule data_Classes_Should_Prefer_Records =
      classes()
          .that()
          .resideInAPackage("io.cheshire..")
          .and()
          .haveSimpleNameEndingWith("Request")
          .or()
          .haveSimpleNameEndingWith("Response")
          .or()
          .haveSimpleNameEndingWith("Result")
          .or()
          .haveSimpleNameEndingWith("Event")
          .or()
          .haveSimpleNameEndingWith("Message")
          .should()
          .beRecords()
          .because("Data classes should use Java 21 records for conciseness and immutability");

  // ============================================
  // Package Organization Rules
  // ============================================

  /**
   * Ensures proper package organization for test utilities.
   *
   * <p><strong>Pattern:</strong> Test utilities should be in test-common module.
   */
  @ArchTest
  static final ArchRule test_Utilities_Should_Be_In_Test_Common =
      classes()
          .that()
          .haveSimpleNameContaining("Test")
          .and()
          .resideInAPackage("io.cheshire..")
          .and()
          .resideOutsideOfPackage("..test..")
          .and()
          .resideOutsideOfPackage("..architecture..")
          .should()
          .resideInAPackage("..test.common..")
          .because("Test utilities should be in cheshire-test-common module");

  // ============================================
  // Dependency Injection Rules
  // ============================================

  /**
   * Discourages field injection in favor of constructor injection.
   *
   * <p><strong>Rationale:</strong> Constructor injection provides:
   *
   * <ul>
   *   <li>Immutable dependencies (final fields)
   *   <li>Clear initialization order
   *   <li>Easier testing (no reflection needed)
   *   <li>Fail-fast behavior
   * </ul>
   *
   * <p><strong>Note:</strong> Cheshire doesn't use a DI framework, but the principle of constructor
   * injection still applies for dependency management.
   */
  @ArchTest
  static final ArchRule dependencies_Should_Be_Constructor_Injected =
      fields()
          .that()
          .areDeclaredInClassesThat()
          .resideInAPackage("io.cheshire..")
          .and()
          .arePrivate()
          .and()
          .areNotStatic()
          .should()
          .beFinal()
          .because("Dependencies should be injected via constructor and stored in final fields");
}
