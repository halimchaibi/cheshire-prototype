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

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ModuleCoverageTest {

  private static final Set<String> REQUIRED_PACKAGE_PREFIXES =
      Set.of(
          "io.cheshire.common.",
          "io.cheshire.core.",
          "io.cheshire.spi.pipeline.",
          "io.cheshire.spi.query.",
          "io.cheshire.spi.source.",
          "io.cheshire.query.engine.jdbc.",
          "io.cheshire.query.engine.calcite.",
          "io.cheshire.source.jdbc.",
          "io.cheshire.source.elasticsearch.",
          "io.cheshire.jetty.",
          "io.cheshire.stdio.",
          "io.cheshire.runtime.");

  @Test
  void architectureSuiteShouldImportEveryCoreModulePackage() {
    final var importedClasses =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("io.cheshire");

    assertThat(REQUIRED_PACKAGE_PREFIXES)
        .allSatisfy(
            prefix ->
                assertThat(importedClasses)
                    .matches(classes -> contains(prefix, classes), "contains package " + prefix));
  }

  private static boolean contains(
      String packagePrefix, Collection<? extends JavaClass> importedClasses) {
    return importedClasses.stream()
        .map(javaClass -> javaClass.getPackageName() + ".")
        .anyMatch(packageName -> packageName.startsWith(packagePrefix));
  }
}
