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

import java.util.Map;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

public final class StaticTableSchema extends AbstractSchema {

  private final Map<String, Table> tables;

  public StaticTableSchema(final Map<String, Table> tables) {
    this.tables = Map.copyOf(tables);
  }

  @Override
  protected Map<String, Table> getTableMap() {
    return tables;
  }
}
