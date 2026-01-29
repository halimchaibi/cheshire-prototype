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

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.*;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.DateString;
import org.apache.calcite.util.TimeString;
import org.apache.calcite.util.TimestampString;

/**
 * Resolves query parameters in Calcite SQL expressions. Handles both positional (?) and named
 * (:param) parameters.
 */
public class ParameterResolver {

  private final Map<String, Object> parameters;
  private final RelDataTypeFactory typeFactory;
  private final RexBuilder rexBuilder;

  public ParameterResolver(
      Map<String, Object> parameters, RelDataTypeFactory typeFactory, RexBuilder rexBuilder) {
    this.parameters = parameters != null ? parameters : new HashMap<>();
    this.typeFactory = typeFactory;
    this.rexBuilder = rexBuilder;
  }

  /** Resolve a named parameter (e.g., :userId) */
  public RexNode resolveNamedParameter(String parameterName) {
    if (!parameters.containsKey(parameterName)) {
      throw new IllegalArgumentException("Parameter not found: " + parameterName);
    }

    Object value = parameters.get(parameterName);
    return createLiteral(value);
  }

  /**
   * Resolve a positional parameter (e.g., ?)
   *
   * @param index 0-based index
   */
  public RexNode resolvePositionalParameter(int index) {
    String key = String.valueOf(index);
    if (!parameters.containsKey(key)) {
      throw new IllegalArgumentException("Parameter at index " + index + " not found");
    }

    Object value = parameters.get(key);
    return createLiteral(value);
  }

  /** Create a RexLiteral from a Java object */
  private RexNode createLiteral(Object value) {
    if (value == null) {
      return rexBuilder.makeNullLiteral(typeFactory.createSqlType(SqlTypeName.NULL));
    }

    // String
    if (value instanceof String) {
      return rexBuilder.makeLiteral((String) value);
    }

    // Numbers
    if (value instanceof Integer) {
      return rexBuilder.makeExactLiteral(BigDecimal.valueOf((Integer) value));
    }
    if (value instanceof Long) {
      return rexBuilder.makeExactLiteral(BigDecimal.valueOf((Long) value));
    }
    if (value instanceof Double || value instanceof Float) {
      return rexBuilder.makeApproxLiteral(BigDecimal.valueOf(((Number) value).doubleValue()));
    }
    if (value instanceof BigDecimal) {
      return rexBuilder.makeExactLiteral((BigDecimal) value);
    }

    // Boolean
    if (value instanceof Boolean) {
      return rexBuilder.makeLiteral((Boolean) value);
    }

    // Date/Time
    if (value instanceof Date) {
      return rexBuilder.makeDateLiteral(new DateString(value.toString()));
    }
    if (value instanceof Time) {
      return rexBuilder.makeTimeLiteral(new TimeString(value.toString()), 0);
    }
    if (value instanceof Timestamp) {
      return rexBuilder.makeTimestampLiteral(new TimestampString(value.toString()), 0);
    }

    // Fallback: convert to string
    return rexBuilder.makeLiteral(value.toString());
  }

  /** Validates that all required parameters are provided */
  public void validateParameters(Set<String> requiredParameters) {
    Set<String> missing = new HashSet<>(requiredParameters);
    missing.removeAll(parameters.keySet());

    if (!missing.isEmpty()) {
      throw new IllegalArgumentException("Missing required parameters: " + missing);
    }
  }

  /** Get all parameter names */
  public Set<String> getParameterNames() {
    return parameters.keySet();
  }

  /** Get parameter value */
  public Object getParameterValue(String name) {
    return parameters.get(name);
  }
}
