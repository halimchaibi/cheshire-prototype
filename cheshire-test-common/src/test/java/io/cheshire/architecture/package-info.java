/*-
 * #%L
 * Cheshire :: Test Common
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

/**
 * Architecture tests for the Cheshire Framework using ArchUnit.
 *
 * <p>This package contains comprehensive architecture tests that validate:
 *
 * <ul>
 *   <li><strong>Module Layering:</strong> Proper dependency hierarchy (SPI → Core → Server →
 *       Runtime)
 *   <li><strong>SPI Compliance:</strong> Correct implementation of Service Provider Interfaces
 *   <li><strong>Pipeline Pattern:</strong> Three-stage pipeline architecture enforcement
 *   <li><strong>Security Patterns:</strong> Prevention of common security anti-patterns
 *   <li><strong>Coding Standards:</strong> Scala-influenced functional Java style
 *   <li><strong>Java 21 Features:</strong> Proper use of records, sealed interfaces, pattern
 *       matching
 * </ul>
 *
 * <p><strong>Test Organization:</strong>
 *
 * <pre>
 * io.cheshire.architecture
 *   ├─ CheshireArchitectureTest      - Core layering and module dependencies
 *   ├─ PipelineArchitectureTest      - Three-stage pipeline pattern validation
 *   ├─ SecurityArchitectureTest      - Security best practices enforcement
 *   └─ CodingStandardsTest          - Coding standards and Java 21 usage
 * </pre>
 *
 * <p><strong>Running Tests:</strong>
 *
 * <pre>{@code
 * # Run all architecture tests
 * mvn test -pl cheshire-test-common
 *
 * # Run specific test class
 * mvn test -Dtest=CheshireArchitectureTest
 * }</pre>
 *
 * <p><strong>ArchUnit Documentation:</strong> <a
 * href="https://www.archunit.org/">https://www.archunit.org/</a>
 *
 * @see com.tngtech.archunit.junit.AnalyzeClasses
 * @see com.tngtech.archunit.junit.ArchTest
 * @since 1.0.0
 */
package io.cheshire.architecture;
