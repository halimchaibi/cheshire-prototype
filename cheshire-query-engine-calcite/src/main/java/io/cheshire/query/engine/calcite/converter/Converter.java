/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.converter;

import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.exception.QueryExecutionException;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql2rel.SqlToRelConverter;
import org.apache.calcite.tools.FrameworkConfig;

public class Converter {

  private SqlToRelConverter sqlToRelConverter;
  private FrameworkConfig frameworkConfig;

  public Converter() {
    // TODO: Will be initialized with validator and cluster
  }

  public Converter(FrameworkConfig frameworkConfig) {
    this.frameworkConfig = frameworkConfig;
  }

  public void initialize(SqlToRelConverter converter) {
    SqlToRelConverter.Config config = frameworkConfig.getSqlToRelConverterConfig();
  }

  public RelNode convert(SqlNode validatedNode) throws QueryEngineException {
    try {
      return sqlToRelConverter.convertQuery(validatedNode, false, true).rel;
    } catch (Exception e) {
      throw new QueryExecutionException("Conversion error: " + e.getMessage(), e);
    }
  }
}
