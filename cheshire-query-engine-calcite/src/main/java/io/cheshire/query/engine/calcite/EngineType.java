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

import java.util.Arrays;
import java.util.Locale;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.AnsiSqlDialect;

/** SQL dialect metadata for registered Calcite source types. */
public enum EngineType {
  JDBC("jdbc", AnsiSqlDialect.DEFAULT),
  ARROW("arrow", AnsiSqlDialect.DEFAULT),
  CSV("csv", AnsiSqlDialect.DEFAULT),
  CASSANDRA("cassandra", AnsiSqlDialect.DEFAULT),
  FILE("file", AnsiSqlDialect.DEFAULT),
  INNODB("innodb", AnsiSqlDialect.DEFAULT),
  OS("os", AnsiSqlDialect.DEFAULT),
  REDIS("redis", AnsiSqlDialect.DEFAULT),
  MAT("mat", AnsiSqlDialect.DEFAULT),
  KAFKA("kafka", AnsiSqlDialect.DEFAULT);

  private final String configKey;
  private final SqlDialect dialect;

  EngineType(final String configKey, final SqlDialect dialect) {
    this.configKey = configKey;
    this.dialect = dialect;
  }

  public String configKey() {
    return configKey;
  }

  public SqlDialect dialect() {
    return dialect;
  }

  public static EngineType fromString(final String text) {
    final String normalized = text.toLowerCase(Locale.ROOT);
    return Arrays.stream(EngineType.values())
        .filter(engineType -> engineType.configKey.equals(normalized))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No engine type found for: " + text));
  }
}
