/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.cheshire.spi.query.exception.QueryEngineInitializationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class SourceAdapterRegistryTest {

  @Test
  void loadsBuiltInAdaptersWithNormalizedTypeKeys() throws QueryEngineInitializationException {
    final SourceAdapterRegistry registry = SourceAdapterRegistry.load();

    assertThat(registry.registeredTypes())
        .contains("jdbc", "arrow", "csv", "file", "os", "kafka", "mat");
    assertThat(registry.adapterFor("JDBC")).isInstanceOf(JdbcAdapter.class);
    assertThat(registry.adapterFor("Arrow")).isInstanceOf(SchemaFactorySourceAdapter.class);
  }

  @Test
  void rejectsDuplicateProviderTypes() {
    final SourceAdapterProvider duplicateJdbc =
        new TypedSourceAdapterProvider("jdbc", new JdbcAdapter());

    assertThatThrownBy(
            () -> SourceAdapterRegistry.of(List.of(duplicateJdbc, duplicateJdbc)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Duplicate source adapter type: jdbc");
  }

  @Test
  void reportsUnknownTypeWithRegisteredTypes() {
    final SourceAdapterRegistry registry = SourceAdapterRegistry.of(BuiltInSourceAdapters.providers());

    assertThatThrownBy(() -> registry.adapterFor("spark"))
        .isInstanceOf(QueryEngineInitializationException.class)
        .hasMessageContaining("Unknown source type: spark")
        .hasMessageContaining("Registered types:");
  }
}
