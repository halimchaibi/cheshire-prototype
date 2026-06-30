/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DslQueryTest {

  @Test
  void selectBuildsSqlForProjectionOnlyQuery() {
    final DslQuery query = DslQuery.select("1 AS x");

    assertThat(query.toSql()).isEqualTo("SELECT 1 AS x");
    assertThat(query.query()).isEqualTo(Map.of("select", query.select()));
  }

  @Test
  void fromAddsSourceWithoutMutatingOriginalQuery() {
    final DslQuery projection = DslQuery.select("name");
    final DslQuery sourced = projection.from("authors");

    assertThat(projection.toSql()).isEqualTo("SELECT name");
    assertThat(sourced.toSql()).isEqualTo("SELECT name FROM authors");
  }

  @Test
  void constructorDefensivelyCopiesCollections() {
    final var select = new ArrayList<String>();
    select.add("name");

    final DslQuery query = new DslQuery(select, "authors", Map.of("limit", 10));
    select.add("email");

    assertThat(query.select()).containsExactly("name");
    assertThat(query.parameters()).containsExactlyEntriesOf(Map.of("limit", 10));
  }

  @Test
  void constructorRejectsBlankProjection() {
    assertThatThrownBy(() -> DslQuery.select(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("select");
  }
}
