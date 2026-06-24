/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.optimizer;

public enum SourceType {
  DEFAULT,

  JDBC,

  REST,

  IN_MEMORY,

  CUSTOM,

  VECTOR_DB,

  NOSQL,

  FILE
}
