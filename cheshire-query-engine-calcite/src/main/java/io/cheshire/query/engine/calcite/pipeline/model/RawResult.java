/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.pipeline.model;

import java.sql.ResultSet;
import java.util.Objects;

public record RawResult(ResultSet resultSet) implements AutoCloseable {
  public RawResult {
    Objects.requireNonNull(resultSet, "resultSet");
  }

  @Override
  public void close() throws Exception {
    resultSet.close();
  }
}
