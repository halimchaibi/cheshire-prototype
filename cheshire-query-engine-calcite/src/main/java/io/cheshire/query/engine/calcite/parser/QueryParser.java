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

import io.cheshire.query.engine.calcite.query.CalciteLogicalQuery;
import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.exception.QueryExecutionException;
import io.cheshire.spi.query.request.LogicalQuery;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParser;

public class QueryParser {

  private SqlParser.Config parserConfig;

  public QueryParser() {
    this.parserConfig = SqlParser.config();
  }

  public QueryParser(SqlParser.Config config) {
    this.parserConfig = config;
  }

  public SqlNode parse(LogicalQuery query) throws QueryEngineException {
    if (!(query instanceof CalciteLogicalQuery)) {
      throw new QueryExecutionException("Query must implement CalciteLogicalQuery");
    }

    String sql = (String) query.query();

    try {
      SqlParser parser = SqlParser.create(sql, parserConfig);
      return parser.parseQuery();
    } catch (Exception e) {
      throw new QueryExecutionException("Parse error: " + e.getMessage(), e);
    }
  }
}
