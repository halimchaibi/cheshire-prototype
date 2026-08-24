/*-
 * #%L
 * Cheshire :: Source Provider :: JDBC
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.source.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SqlSourceProviderQueryTest {

  @Test
  void exposesSpiParametersWithoutRecursion() {
    final var query =
        SqlSourceProviderQuery.of("select * from users where id = :id", Map.of("id", 7));

    assertThat(query.parameters()).containsEntry("id", 7);
    assertThat(query.hasParameters()).isTrue();
  }
}
