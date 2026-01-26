/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.parser;

import io.cheshire.common.utils.ObjectUtils;
import io.cheshire.query.engine.calcite.query.CalciteLogicalQuery;
import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.exception.QueryExecutionException;
import io.cheshire.spi.query.request.LogicalQuery;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.validate.SqlConformanceEnum;

public class QueryParser {

  private final SqlParser.Config parserConfig;

  public QueryParser() {
    this.parserConfig = SqlParser.config().withConformance(SqlConformanceEnum.DEFAULT);
  }

  public QueryParser(SqlParser.Config config) {
    this.parserConfig = config;
  }

  public SqlNode parse(LogicalQuery logicalQuery) throws QueryEngineException {
    try {
      if (!(logicalQuery instanceof CalciteLogicalQuery)) {
        throw new QueryExecutionException("Query must implement CalciteLogicalQuery");
      }

      String sql =
          ObjectUtils.someObjectAs(logicalQuery.query(), String.class)
              .orElseThrow(
                  () -> new QueryExecutionException("Query must implement CalciteLogicalQuery"));
      return SqlParser.create(sql, parserConfig).parseQuery(sql);
    } catch (Exception e) {
      throw new QueryExecutionException("Parse error: " + e.getMessage(), e);
    }
  }
}
