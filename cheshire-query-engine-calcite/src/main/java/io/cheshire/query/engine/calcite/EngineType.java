/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite;

import lombok.Getter;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.AnsiSqlDialect;
import org.apache.calcite.sql.dialect.HiveSqlDialect;
import org.apache.calcite.sql.dialect.SparkSqlDialect;

@Getter
public enum EngineType {
  JDBC("jdbc", AnsiSqlDialect.DEFAULT),
  SPARK("spark", SparkSqlDialect.DEFAULT),
  HIVE("hive", HiveSqlDialect.DEFAULT),
  KAFKA("kafka", AnsiSqlDialect.DEFAULT);

  private final String configKey;
  private final SqlDialect dialect;

  EngineType(String configKey, SqlDialect dialect) {
    this.configKey = configKey;
    this.dialect = dialect;
  }

  /** Helper to resolve type from config strings safely. */
  public static EngineType fromString(String text) {
    for (EngineType b : EngineType.values()) {
      if (b.configKey.equalsIgnoreCase(text)) {
        return b;
      }
    }
    throw new IllegalArgumentException("No engine type found for: " + text);
  }
}
