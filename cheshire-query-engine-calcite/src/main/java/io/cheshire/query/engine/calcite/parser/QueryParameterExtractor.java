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

import java.util.*;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.util.SqlBasicVisitor;

/**
 * Extracts parameter placeholders from a SQL query. Supports both positional (?) and named (:param)
 * parameters.
 */
public class QueryParameterExtractor {

  /** Extract all parameter names from a parsed SQL node */
  public static Set<String> extractParameters(SqlNode sqlNode) {
    ParameterCollector collector = new ParameterCollector();
    sqlNode.accept(collector);
    return collector.getParameters();
  }

  /** Extract parameters from SQL string (regex-based, less accurate) */
  public static Set<String> extractParametersFromString(String sql) {
    Set<String> parameters = new LinkedHashSet<>();

    // Named parameters: :paramName
    java.util.regex.Pattern namedPattern =
        java.util.regex.Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");
    java.util.regex.Matcher namedMatcher = namedPattern.matcher(sql);
    while (namedMatcher.find()) {
      parameters.add(namedMatcher.group(1));
    }

    // Positional parameters: ?
    // Count occurrences
    int count = 0;
    for (int i = 0; i < sql.length(); i++) {
      if (sql.charAt(i) == '?') {
        parameters.add(String.valueOf(count++));
      }
    }

    return parameters;
  }

  /** Visitor that collects dynamic parameters from SQL AST */
  private static class ParameterCollector extends SqlBasicVisitor<Void> {
    private final Set<String> parameters = new LinkedHashSet<>();
    private int positionalIndex = 0;

    public Set<String> getParameters() {
      return parameters;
    }

    @Override
    public Void visit(SqlDynamicParam param) {
      // Positional parameter (?)
      parameters.add(String.valueOf(positionalIndex++));
      return null;
    }

    @Override
    public Void visit(SqlCall call) {
      // Check for named parameters (custom handling if you use :param syntax)
      if (call.getOperator().getName().equals("NAMED_PARAM")) {
        SqlNode operand = call.operand(0);
        if (operand instanceof SqlIdentifier) {
          parameters.add(((SqlIdentifier) operand).getSimple());
        }
      }
      return super.visit(call);
    }

    @Override
    public Void visit(SqlNodeList nodeList) {
      for (SqlNode node : nodeList) {
        node.accept(this);
      }
      return null;
    }
  }

  /**
   * Convert named parameters to positional parameters in SQL string Example: "SELECT * FROM users
   * WHERE id = :userId" -> "SELECT * FROM users WHERE id = ?"
   */
  public static String convertNamedToPositional(String sql, Map<String, Object> parameters) {
    List<String> orderedParams = new ArrayList<>();
    StringBuilder result = new StringBuilder();

    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");
    java.util.regex.Matcher matcher = pattern.matcher(sql);

    int lastEnd = 0;
    while (matcher.find()) {
      String paramName = matcher.group(1);
      result.append(sql, lastEnd, matcher.start());
      result.append("?");
      orderedParams.add(paramName);
      lastEnd = matcher.end();
    }
    result.append(sql.substring(lastEnd));

    return result.toString();
  }

  /** Get ordered parameter values for positional parameters */
  public static List<Object> getOrderedParameterValues(String sql, Map<String, Object> parameters) {

    List<Object> values = new ArrayList<>();
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");
    java.util.regex.Matcher matcher = pattern.matcher(sql);

    while (matcher.find()) {
      String paramName = matcher.group(1);
      if (!parameters.containsKey(paramName)) {
        throw new IllegalArgumentException("Missing parameter: " + paramName);
      }
      values.add(parameters.get(paramName));
    }

    return values;
  }
}
