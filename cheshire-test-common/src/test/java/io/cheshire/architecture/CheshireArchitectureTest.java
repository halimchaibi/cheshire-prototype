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
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture tests using ArchUnit to validate the Cheshire framework structure.
 *
 * <p><strong>Purpose:</strong>
 *
 * <p>Enforces architectural constraints and best practices across the Cheshire framework:
 *
 * <ul>
 *   <li><strong>Module Layering:</strong> SPI → Core → Server → Runtime dependency hierarchy
 *   <li><strong>No Circular Dependencies:</strong> Prevents cyclic dependencies between modules
 *   <li><strong>SPI Compliance:</strong> Query engines and source providers implement correct SPIs
 *   <li><strong>Factory Pattern:</strong> Factories implement appropriate SPI factory interfaces
 *   <li><strong>Exception Hierarchy:</strong> All exceptions extend Exception
 *   <li><strong>Naming Conventions:</strong> Consistent naming across the framework
 *   <li><strong>Immutability:</strong> Records for data carriers where applicable
 * </ul>
 *
 * <p><strong>Module Structure:</strong>
 *
 * <pre>
 * ┌─────────────────────────────────────────┐
 * │           cheshire-runtime              │ (Top layer)
 * └─────────────────────────────────────────┘
 *                  ↓
 * ┌─────────────────────────────────────────┐
 * │           cheshire-server               │
 * └─────────────────────────────────────────┘
 *                  ↓
 * ┌─────────────────────────────────────────┐
 * │           cheshire-core                 │
 * └─────────────────────────────────────────┘
 *                  ↓
 * ┌─────────────────────────────────────────┐
 * │        SPI Modules (base layer)         │
 * │  • cheshire-pipeline-spi                │
 * │  • cheshire-query-engine-spi            │
 * │  • cheshire-source-provider-spi         │
 * └─────────────────────────────────────────┘
 * </pre>
 *
 * <p><strong>Design Principles Enforced:</strong>
 *
 * <ul>
 *   <li>Dependency Inversion: High-level modules depend on abstractions (SPIs)
 *   <li>Open/Closed Principle: Extensions via SPI, core closed for modification
 *   <li>Single Responsibility: Each module has clear, distinct purpose
 *   <li>Interface Segregation: Small, focused SPI interfaces
 * </ul>
 *
 * @see <a href="https://www.archunit.org/">ArchUnit Documentation</a>
 * @since 1.0.0
 */
@AnalyzeClasses(
    packages = "io.cheshire",
    importOptions = {ImportOption.DoNotIncludeTests.class})
public class CheshireArchitectureTest {

  // ============================================
  // Layer Rules (Module Dependencies)
  // ============================================

  /**
   * Ensures SPI modules remain independent and don't depend on higher layers.
   *
   * <p><strong>Rationale:</strong> SPI modules define contracts that implementations must follow.
   * They should be pure interfaces with no dependencies on concrete implementations (Core, Server,
   * Runtime).
   *
   * <p><strong>Allowed Dependencies:</strong>
   *
   * <ul>
   *   <li>Other SPI modules (for composition)
   *   <li>Java standard library
   *   <li>Common utilities (exceptions, config)
   * </ul>
   */
  @ArchTest
  static final ArchRule spi_Should_Not_Depend_On_Core_Server_Runtime =
      classes()
          .that()
          .resideInAPackage("..spi..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage("..spi..", "..common..", "java..", "lombok..", "org.slf4j..")
          .because(
              "SPI modules must remain independent and cannot depend on Core, Server, or Runtime modules");

  /**
   * Ensures Core module only depends on SPIs and not on implementations.
   *
   * <p><strong>Rationale:</strong> Core orchestrates the framework but should depend only on
   * abstractions (SPIs). This enables plugin-based architecture via ServiceLoader.
   *
   * <p><strong>Allowed Dependencies:</strong>
   *
   * <ul>
   *   <li>All SPI modules (abstractions)
   *   <li>Common utilities
   *   <li>Jackson (for configuration)
   *   <li>SLF4J (for logging)
   * </ul>
   */
  @ArchTest
  static final ArchRule core_Should_Only_Depend_On_Spi_And_Common =
      classes()
          .that()
          .resideInAPackage("..core..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "..core..",
              "..common..",
              "..spi..",
              "java..",
              "lombok..",
              "org.slf4j..",
              "com.fasterxml.jackson..")
          .because("Core modules can depend on SPI and Common but not Server or Runtime");

  /**
   * Ensures Server module respects layering: Server → Core → SPI.
   *
   * <p><strong>Rationale:</strong> Server implementations handle protocol-specific concerns (HTTP,
   * stdio) and delegate to Core for business logic.
   *
   * <p><strong>Allowed Dependencies:</strong>
   *
   * <ul>
   *   <li>Core module (for orchestration)
   *   <li>SPI modules (for contracts)
   *   <li>Jetty (for HTTP server)
   *   <li>MCP SDK (for MCP protocol)
   * </ul>
   */
  @ArchTest
  static final ArchRule server_Should_Only_Depend_On_Core_And_Spi =
      classes()
          .that()
          .resideInAPackage("..server..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "..server..",
              "..core..",
              "..common..",
              "..spi..",
              "java..",
              "lombok..",
              "org.slf4j..",
              "com.fasterxml.jackson..",
              "org.eclipse.jetty..",
              "io.modelcontextprotocol..")
          .because("Server modules can depend on Core and SPI only, not Runtime");

  /**
   * Ensures Runtime module is the top layer with access to all modules.
   *
   * <p><strong>Rationale:</strong> Runtime orchestrates startup, shutdown, monitoring, and
   * deployment. It's the only module that can depend on all lower layers.
   *
   * <p><strong>Allowed Dependencies:</strong>
   *
   * <ul>
   *   <li>All Cheshire modules (top-level orchestration)
   *   <li>Standard dependencies (logging, configuration)
   * </ul>
   */
  @ArchTest
  static final ArchRule runtime_Should_Only_Depend_On_Server_Core_Spi =
      classes()
          .that()
          .resideInAPackage("..runtime..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "..runtime..",
              "..server..",
              "..core..",
              "..common..",
              "..spi..",
              "java..",
              "lombok..",
              "org.slf4j..",
              "com.fasterxml.jackson..")
          .because("Runtime is the top layer and can depend on all lower layers");

  // ============================================
  // SPI Implementation Rules
  // ============================================

  /**
   * Ensures all query engine implementations properly implement the QueryEngine SPI.
   *
   * <p><strong>Pattern:</strong> Classes ending with "QueryEngine" in query.engine packages must
   * implement {@code io.cheshire.spi.query.engine.QueryEngine}.
   *
   * <p><strong>Examples:</strong>
   *
   * <ul>
   *   <li>JdbcQueryEngine implements QueryEngine ✓
   *   <li>CalciteQueryEngine implements QueryEngine ✓
   * </ul>
   */
  @ArchTest
  static final ArchRule queryEngines_Should_Implement_Spi =
      classes()
          .that()
          .resideInAPackage("..query.engine..")
          .and()
          .haveSimpleNameEndingWith("QueryEngine")
          .and()
          .areNotInterfaces()
          .and()
          .areNotNestedClasses()
          .should()
          .implement("io.cheshire.spi.query.engine.QueryEngine")
          .because("Query engines must implement the QueryEngine SPI");

  /**
   * Ensures all source provider implementations properly implement the SourceProvider SPI.
   *
   * <p><strong>Pattern:</strong> Classes ending with "Provider" in source packages must implement
   * {@code io.cheshire.spi.source.SourceProvider}.
   *
   * <p><strong>Examples:</strong>
   *
   * <ul>
   *   <li>JdbcDataSourceProvider implements SourceProvider ✓
   *   <li>RestApiProvider implements SourceProvider ✓
   * </ul>
   */
  @ArchTest
  static final ArchRule source_Providers_Should_Implement_Spi =
      classes()
          .that()
          .resideInAPackage("..source..")
          .and()
          .haveSimpleNameEndingWith("Provider")
          .and()
          .areNotInterfaces()
          .and()
          .areNotNestedClasses()
          .should()
          .implement("io.cheshire.spi.source.SourceProvider")
          .because("Source providers must implement the SourceProvider SPI");

  // ============================================
  // Factory Pattern Rules
  // ============================================

  /**
   * Ensures query engine factories implement the QueryEngineFactory SPI.
   *
   * <p><strong>Pattern:</strong> Factory classes in query.engine packages must implement {@code
   * io.cheshire.spi.query.engine.QueryEngineFactory}.
   *
   * <p><strong>ServiceLoader Integration:</strong> These factories are discovered via Java's
   * ServiceLoader mechanism for plugin-based architecture.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * public class JdbcQueryEngineFactory implements QueryEngineFactory {
   *     &#64;Override
   *     public JdbcQueryEngine create(JdbcQueryEngineConfig config) { ... }
   * }
   * }</pre>
   */
  @ArchTest
  static final ArchRule queryEngine_Factories_Should_Implement_Spi =
      classes()
          .that()
          .haveSimpleNameEndingWith("Factory")
          .and()
          .resideInAPackage("..query.engine..")
          .and()
          .areNotInterfaces()
          .and()
          .areNotNestedClasses()
          .should()
          .implement("io.cheshire.spi.query.engine.QueryEngineFactory")
          .because(
              "Query engine factories must implement QueryEngineFactory SPI for ServiceLoader discovery");

  /**
   * Ensures source provider factories implement the SourceProviderFactory SPI.
   *
   * <p><strong>Pattern:</strong> Factory classes in source packages must implement {@code
   * io.cheshire.spi.source.SourceProviderFactory}.
   *
   * <p><strong>ServiceLoader Integration:</strong> Registered in {@code
   * META-INF/services/io.cheshire.spi.source.SourceProviderFactory}.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * public class JdbcDataSourceProviderFactory implements SourceProviderFactory {
   *     &#64;Override
   *     public JdbcDataSourceProvider create(JdbcDataSourceConfig config) { ... }
   * }
   * }</pre>
   */
  @ArchTest
  static final ArchRule sourceProvider_Factories_Should_Implement_Spi =
      classes()
          .that()
          .haveSimpleNameEndingWith("Factory")
          .and()
          .resideInAPackage("..source..")
          .and()
          .areNotInterfaces()
          .and()
          .areNotNestedClasses()
          .should()
          .implement("io.cheshire.spi.source.SourceProviderFactory")
          .because(
              "Source provider factories must implement SourceProviderFactory SPI for ServiceLoader discovery");

  // ============================================
  // Exception Rules
  // ============================================

  /**
   * Ensures consistent exception hierarchy across the framework.
   *
   * <p><strong>Rationale:</strong> All custom exceptions should extend Exception (either directly
   * or via RuntimeException) for consistent error handling.
   *
   * <p><strong>Exception Hierarchy:</strong>
   *
   * <pre>
   * Exception
   *   └─ RuntimeException
   *       └─ CheshireException (base)
   *           ├─ ConfigurationException
   *           ├─ ValidationException
   *           ├─ ExecutionException
   *           ├─ QueryEngineException
   *           ├─ SourceProviderException
   *           └─ CheshireRuntimeError
   * </pre>
   */
  @ArchTest
  static final ArchRule exceptions_Should_Extend_Exception =
      classes()
          .that()
          .haveSimpleNameEndingWith("Exception")
          .or()
          .haveSimpleNameEndingWith("Error")
          .and()
          .resideInAPackage("io.cheshire..")
          .should()
          .beAssignableTo(Exception.class)
          .because("All exceptions should extend Exception for consistent error handling");

  // ============================================
  // Circular Dependency Detection
  // ============================================

  /**
   * Prevents circular dependencies between modules.
   *
   * <p><strong>Rationale:</strong> Circular dependencies lead to:
   *
   * <ul>
   *   <li>Tight coupling between modules
   *   <li>Difficult testing (can't mock dependencies)
   *   <li>Maintenance nightmares
   *   <li>Build order issues
   * </ul>
   *
   * <p><strong>Detection:</strong> Analyzes package slices (e.g., io.cheshire.core,
   * io.cheshire.server) and ensures no cycles exist.
   *
   * <p><strong>Example Violation:</strong>
   *
   * <pre>
   * core → server → core ✗ (circular!)
   * </pre>
   */
  @ArchTest
  static final ArchRule no_Circular_Dependencies =
      slices()
          .matching("io.cheshire.(*)..")
          .should()
          .beFreeOfCycles()
          .because("No circular dependencies should exist between modules for maintainability");

  // ============================================
  // Naming Convention Rules
  // ============================================

  /**
   * Ensures manager classes follow naming conventions.
   *
   * <p><strong>Pattern:</strong> Classes ending with "Manager" should reside in manager packages
   * and handle lifecycle/registration concerns.
   *
   * <p><strong>Examples:</strong>
   *
   * <ul>
   *   <li>ConfigurationManager - Configuration lifecycle
   *   <li>LifecycleManager - Component initialization/shutdown
   *   <li>CapabilityManager - Capability registration
   * </ul>
   */
  @ArchTest
  static final ArchRule managers_Should_Reside_In_Manager_Package =
      classes()
          .that()
          .haveSimpleNameEndingWith("Manager")
          .and()
          .resideInAPackage("io.cheshire..")
          .should()
          .resideInAPackage("..manager..")
          .because("Manager classes should be organized in manager packages");

  /**
   * Ensures server-related classes use appropriate naming.
   *
   * <p><strong>Pattern:</strong> Classes related to servers should clearly indicate their purpose
   * through naming:
   *
   * <ul>
   *   <li>*Server - Server interface/abstraction
   *   <li>*ServerHandle - Capability-specific server handle
   *   <li>*ServerContainer - Physical server container
   *   <li>*ServerFactory - Server creation factory
   * </ul>
   */
  @ArchTest
  static final ArchRule server_Classes_Should_Follow_Naming_Convention =
      classes()
          .that()
          .haveSimpleNameContaining("Server")
          .and()
          .resideInAPackage("io.cheshire..")
          .and()
          .areNotInterfaces()
          .should()
          .haveSimpleNameEndingWith("Server")
          .orShould()
          .haveSimpleNameEndingWith("ServerHandle")
          .orShould()
          .haveSimpleNameEndingWith("ServerContainer")
          .orShould()
          .haveSimpleNameEndingWith("ServerFactory")
          .orShould()
          .haveSimpleNameEndingWith("ServerRegistry")
          .because("Server classes should follow consistent naming patterns");

  // ============================================
  // Immutability Rules (Java 21 Records)
  // ============================================

  /**
   * Encourages use of Records for configuration classes.
   *
   * <p><strong>Rationale:</strong> Configuration objects should be immutable to ensure:
   *
   * <ul>
   *   <li>Thread safety without synchronization
   *   <li>Predictable behavior
   *   <li>Easy testing
   *   <li>Pattern matching support
   * </ul>
   *
   * <p><strong>Pattern:</strong> Classes ending with "Config" should preferably be records.
   *
   * <p><strong>Example:</strong>
   *
   * <pre>{@code
   * public record JdbcQueryEngineConfig(String name, List<String> sources) implements QueryEngineConfig {
   *     // Compact constructor for validation
   *     public JdbcQueryEngineConfig {
   *         if (name == null || name.isBlank()) {
   *             throw new IllegalArgumentException("name cannot be null or blank");
   *         }
   *         sources = sources != null ? List.copyOf(sources) : List.of();
   *     }
   * }
   * }</pre>
   */
  @ArchTest
  static final ArchRule config_Classes_Should_Be_Records_Or_Builders =
      classes()
          .that()
          .haveSimpleNameEndingWith("Config")
          .and()
          .resideInAPackage("io.cheshire..")
          .and()
          .areNotNestedClasses()
          .and()
          .areNotInterfaces()
          .should()
          .beRecords()
          .orShould()
          .haveSimpleNameContaining("Builder")
          .because("Configuration classes should be immutable records for thread safety");

  /**
   * Ensures request/response classes use Records for immutability.
   *
   * <p><strong>Rationale:</strong> Request and response objects are data carriers that should be
   * immutable to prevent accidental modification during processing.
   *
   * <p><strong>Pattern:</strong>
   *
   * <ul>
   *   <li>*Request classes → records
   *   <li>*Response classes → records
   *   <li>*Result classes → records
   * </ul>
   */
  @ArchTest
  static final ArchRule request_Response_Classes_Should_Be_Records =
      classes()
          .that()
          .haveSimpleNameEndingWith("Request")
          .or()
          .haveSimpleNameEndingWith("Response")
          .or()
          .haveSimpleNameEndingWith("Result")
          .and()
          .resideInAPackage("io.cheshire..")
          .and()
          .areNotNestedClasses()
          .and()
          .areNotInterfaces()
          .and()
          .areNotEnums()
          .should()
          .beRecords()
          .because("Request, Response, and Result classes should be immutable records");

  // ============================================
  // Package Organization Rules
  // ============================================

  /**
   * Ensures proper package organization for SPI modules.
   *
   * <p><strong>Pattern:</strong> SPI classes should be in .spi. packages and not mixed with
   * implementations.
   */
  @ArchTest
  static final ArchRule spi_Interfaces_Should_Be_In_Spi_Packages =
      classes()
          .that()
          .areInterfaces()
          .and()
          .haveSimpleNameContaining("SPI")
          .or()
          .haveSimpleNameEndingWith("Factory")
          .and()
          .resideInAPackage("io.cheshire..")
          .should()
          .resideInAPackage("..spi..")
          .because("SPI interfaces should be organized in .spi. packages");
}
