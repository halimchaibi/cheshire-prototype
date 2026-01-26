/*-
 * #%L
 * Cheshire :: Query Engine :: JDBC
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.exception.QueryExecutionException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class SqlTemplateQueryBuilder {

  // TODO: this a prototype for demo purposes only. Needs redesign.
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private SqlTemplateQueryBuilder() {
    throw new AssertionError("Utility class - do not instantiate");
  }

  public static SqlQueryEngineRequest buildQuery(
      String templateJson, Map<String, Object> requestParams) throws QueryEngineException {
    JsonNode template = null;
    try {
      template = MAPPER.readTree(templateJson);
    } catch (JsonProcessingException e) {
      throw new QueryExecutionException("Failed to parse the Json template", e);
    }

    String operation =
        Optional.ofNullable(template.get("operation"))
            .map(n -> n.asText().toUpperCase())
            .orElse("SELECT");

    SqlQueryEngineRequest request =
        switch (operation) {
          case "INSERT" -> buildInsert(template, requestParams);
          case "UPDATE" -> buildUpdate(template, requestParams);
          case "DELETE" -> buildDelete(template, requestParams);
          default -> buildSelect(template, requestParams);
        };
    return request;
  }

  private static SqlQueryEngineRequest buildSelect(
      JsonNode template, Map<String, Object> requestParams) throws QueryEngineException {
    Set<String> requestedFields = parseFields(requestParams.get("fields"));
    StringBuilder sql = new StringBuilder();
    Map<String, Object> params = new LinkedHashMap<>();

    buildCtes(sql, template, requestParams, params);

    sql.append("SELECT ");
    buildProjection(sql, template, requestedFields);
    sql.append(" FROM ");
    buildSource(sql, template);
    buildJoins(sql, template);
    buildFilters(sql, template, requestParams, params);
    buildGroupBy(sql, template);
    buildHaving(sql, template, requestParams, params);
    buildOrderBy(sql, template, requestParams);
    buildPagination(sql, template, requestParams);

    return new SqlQueryEngineRequest(sql.toString(), params, "jdbc");
  }

  /** Builds Common Table Expressions (WITH clause). */
  private static void buildCtes(
      StringBuilder sql,
      JsonNode template,
      Map<String, Object> requestParams,
      Map<String, Object> params)
      throws QueryEngineException {
    if (template.has("ctes") && template.get("ctes").isArray()) {
      JsonNode ctesArray = template.get("ctes");
      List<String> cteClauses = new ArrayList<>();

      for (JsonNode cte : ctesArray) {
        if (!cte.has("name") || !cte.has("query")) {
          throw new IllegalArgumentException("CTE definition requires 'name' and 'query' fields");
        }

        String cteName = cte.get("name").asText();

        List<String> columns = new ArrayList<>();
        if (cte.has("columns") && cte.get("columns").isArray()) {
          for (JsonNode column : cte.get("columns")) {
            columns.add(column.asText());
          }
        }

        StringBuilder cteQuery = new StringBuilder();

        JsonNode queryNode = cte.get("query");

        if (queryNode.has("subquery")) {

          String subquerySql = buildSubquery(queryNode.get("subquery"), requestParams, params);
          cteQuery.append(cteName);
          if (!columns.isEmpty()) {
            cteQuery.append("(").append(String.join(", ", columns)).append(")");
          }
          cteQuery.append(" AS (").append(subquerySql).append(")");
        } else {

          try {
            SqlQueryEngineRequest cteRequest = buildQuery(queryNode.toString(), requestParams);

            params.putAll(cteRequest.parameters());

            cteQuery.append(cteName);
            if (!columns.isEmpty()) {
              cteQuery.append("(").append(String.join(", ", columns)).append(")");
            }
            cteQuery.append(" AS (").append(cteRequest.query()).append(")");
          } catch (QueryEngineException e) {
            throw new IllegalArgumentException("Failed to build CTE query: " + e.getMessage(), e);
          }
        }

        cteClauses.add(cteQuery.toString());
      }

      if (!cteClauses.isEmpty()) {
        sql.append("WITH ");

        sql.append(String.join(", ", cteClauses)).append(" ");
      }
    }
  }

  private static String buildSubquery(
      JsonNode subqueryTemplate, Map<String, Object> requestParams, Map<String, Object> params)
      throws QueryEngineException {

    if (subqueryTemplate.has("operation")) {

      SqlQueryEngineRequest subqueryRequest =
          buildQuery(subqueryTemplate.toString(), requestParams);
      params.putAll(subqueryRequest.parameters());
      return subqueryRequest.query();
    } else if (subqueryTemplate.has("select")) {

      StringBuilder subquerySql = new StringBuilder("SELECT ");

      JsonNode select = subqueryTemplate.get("select");
      if (select.isArray()) {
        List<String> fields = new ArrayList<>();
        for (JsonNode field : select) {
          fields.add(field.asText());
        }
        subquerySql.append(String.join(", ", fields));
      } else {
        subquerySql.append(select.asText());
      }

      subquerySql.append(" FROM ");

      if (subqueryTemplate.has("from")) {
        JsonNode from = subqueryTemplate.get("from");
        if (from.has("table")) {
          subquerySql.append(from.get("table").asText());
          if (from.has("alias")) {
            subquerySql.append(" ").append(from.get("alias").asText());
          }
        } else {
          subquerySql.append(from.asText());
        }
      }

      if (subqueryTemplate.has("where")) {
        String whereClause = buildConditions(subqueryTemplate.get("where"), requestParams, params);
        if (whereClause != null && !whereClause.isBlank()) {
          subquerySql.append(" WHERE ").append(whereClause);
        }
      }

      return subquerySql.toString();
    } else {

      String sql = subqueryTemplate.asText();
      return extractAndBindParams(sql, requestParams, params, false);
    }
  }

  private static SqlQueryEngineRequest buildInsert(
      JsonNode template, Map<String, Object> requestParams) throws QueryEngineException {
    StringBuilder sql = new StringBuilder();
    Map<String, Object> params = new LinkedHashMap<>();

    buildCtes(sql, template, requestParams, params);

    sql.append("INSERT INTO ");
    buildSource(sql, template);

    JsonNode columns = template.get("columns");
    if (columns == null || !columns.isArray() || columns.isEmpty()) {
      throw new IllegalArgumentException("INSERT requires 'columns' array");
    }

    List<String> columnNames = new ArrayList<>();
    List<String> valuePlaceholders = new ArrayList<>();

    for (JsonNode column : columns) {
      String field = column.get("field").asText();
      columnNames.add(field);

      handleValueMapping(
          column,
          requestParams,
          params,
          valuePlaceholders,
          column.path("optional").asBoolean(false));
    }

    sql.append(" (").append(String.join(", ", columnNames)).append(")");
    sql.append(" VALUES (").append(String.join(", ", valuePlaceholders)).append(")");
    buildReturning(sql, template);

    return new SqlQueryEngineRequest(sql.toString(), params, "jdbc");
  }

  private static SqlQueryEngineRequest buildUpdate(
      JsonNode template, Map<String, Object> requestParams) throws QueryEngineException {
    StringBuilder sql = new StringBuilder();
    Map<String, Object> params = new LinkedHashMap<>();

    buildCtes(sql, template, requestParams, params);

    sql.append("UPDATE ");
    buildSource(sql, template);

    JsonNode setFields = template.get("set");
    if (setFields == null || !setFields.isArray() || setFields.isEmpty()) {
      throw new IllegalArgumentException("UPDATE requires 'set' array");
    }

    List<String> setClauses = new ArrayList<>();
    for (JsonNode node : setFields) {
      String fieldName = node.get("field").asText();
      List<String> placeholderWrapper = new ArrayList<>();

      if (node.has("function")) {
        setClauses.add(fieldName + " = " + node.get("function").asText());
        continue;
      }

      handleValueMapping(node, requestParams, params, placeholderWrapper);

      if (!placeholderWrapper.isEmpty()) {
        setClauses.add(fieldName + " = " + placeholderWrapper.get(0));
      }
    }

    boolean hasUpdateParams = params.keySet().stream().anyMatch(key -> !"id".equals(key));

    if (!hasUpdateParams) {
      throw new IllegalArgumentException(
          "UPDATE requires at least one non-optional field to be provided");
    }

    sql.append(" SET ").append(String.join(", ", setClauses));
    buildFilters(sql, template, requestParams, params);
    buildReturning(sql, template);

    return new SqlQueryEngineRequest(sql.toString(), params, "jdbc");
  }

  private static SqlQueryEngineRequest buildDelete(
      JsonNode template, Map<String, Object> requestParams) throws QueryEngineException {
    StringBuilder sql = new StringBuilder();
    Map<String, Object> params = new LinkedHashMap<>();

    buildCtes(sql, template, requestParams, params);

    sql.append("DELETE FROM ");
    buildSource(sql, template);
    buildFilters(sql, template, requestParams, params);

    if (params.isEmpty() && !template.path("allowDeleteAll").asBoolean()) {
      throw new IllegalArgumentException(
          "DELETE without WHERE is blocked. Set 'allowDeleteAll: true' if intended.");
    }

    buildReturning(sql, template);
    return new SqlQueryEngineRequest(sql.toString(), params, "jdbc");
  }

  private static void handleValueMapping(
      JsonNode node,
      Map<String, Object> requestParams,
      Map<String, Object> params,
      List<String> placeholders) {
    boolean isOptional = node.has("optional") && node.get("optional").asBoolean();
    handleValueMapping(node, requestParams, params, placeholders, isOptional);
  }

  private static void handleValueMapping(
      JsonNode node,
      Map<String, Object> requestParams,
      Map<String, Object> params,
      List<String> placeholders,
      boolean isOptional) {
    if (node.has("param")) {
      String paramName = node.get("param").asText();
      Object val = requestParams.get(paramName);

      boolean isNullable = node.path("nullable").asBoolean(false);

      if (val == null) {
        if (isOptional) {
          return;
        } else if (isNullable) {
          placeholders.add("NULL");
          return;
        } else {
          throw new IllegalArgumentException("Missing required parameter: " + paramName);
        }
      }

      placeholders.add(":" + paramName);
      params.put(paramName, convertValue(val));

    } else if (node.has("value")) {
      JsonNode valueNode = node.get("value");
      if (valueNode.isObject() && valueNode.has("expression")) {
        String expression = valueNode.get("expression").asText();
        String processedExpression =
            extractAndBindParams(expression, requestParams, params, isOptional);
        if (processedExpression != null) {
          placeholders.add(processedExpression);
        } else if (isOptional) {
        } else {
          throw new IllegalArgumentException(
              "Missing required parameters for expression: " + expression);
        }
      } else {
        placeholders.add("'" + valueNode.asText() + "'");
      }

    } else if (node.has("function")) {
      placeholders.add(node.get("function").asText());

    } else if (node.has("expression")) {
      String expression = node.get("expression").asText();
      String processedExpression =
          extractAndBindParams(expression, requestParams, params, isOptional);
      if (processedExpression != null) {
        placeholders.add(processedExpression);
      } else if (isOptional) {
      } else {
        throw new IllegalArgumentException(
            "Missing required parameters for expression: " + expression);
      }
    }
  }

  private static void buildProjection(
      StringBuilder sql, JsonNode template, Set<String> requestedFields) {
    List<String> selectItems = new ArrayList<>();
    List<String> windowFunctions = new ArrayList<>();

    boolean hasWindowFunctions =
        template.has("windowFunctions") && template.get("windowFunctions").isArray();

    if (hasWindowFunctions) {
      for (JsonNode wf : template.get("windowFunctions")) {
        String expr = wf.get("expression").asText();
        if (wf.has("alias")) expr += " AS " + wf.get("alias").asText();
        windowFunctions.add(expr);
      }
    }

    boolean hasAggregates = template.has("aggregates") && template.get("aggregates").isArray();
    boolean hasProjection = template.has("projection") && template.get("projection").isArray();

    if (hasAggregates) {
      for (JsonNode agg : template.get("aggregates")) {
        String expr = agg.get("func").asText() + "(" + agg.get("field").asText() + ")";
        if (agg.has("alias")) expr += " AS " + agg.get("alias").asText();
        selectItems.add(expr);
      }
    }

    if (hasProjection) {
      for (JsonNode proj : template.get("projection")) {
        String field = proj.get("field").asText();
        String alias = proj.path("alias").asText(null);

        if (!requestedFields.isEmpty()) {
          String match = alias != null ? alias : field;
          if (!requestedFields.contains(match)) continue;
        }

        String expr = field + (alias != null && !alias.equals(field) ? " AS " + alias : "");
        selectItems.add(expr);
      }
    }

    selectItems.addAll(windowFunctions);

    if (!hasAggregates && !hasProjection && !hasWindowFunctions) {
      selectItems.add("*");
    }

    if (selectItems.isEmpty() && hasProjection) {
      selectItems.add("*");
    }

    sql.append(String.join(", ", selectItems));
  }

  private static void buildFilters(
      StringBuilder sql,
      JsonNode template,
      Map<String, Object> requestParams,
      Map<String, Object> params) {
    Optional.ofNullable(template.get("filters"))
        .ifPresent(
            f -> {
              String clause = buildConditions(f, requestParams, params);
              if (clause != null && !clause.isBlank()) {
                sql.append(" WHERE ").append(clause);
              }
            });
  }

  private static String buildConditions(
      JsonNode filterNode, Map<String, Object> requestParams, Map<String, Object> params) {
    if (filterNode.has("conditions")) {
      String op = filterNode.path("op").asText("AND");
      List<String> clauses =
          StreamSupport.stream(filterNode.get("conditions").spliterator(), false)
              .map(
                  c -> {
                    if (c.has("conditions")) {
                      return buildConditions(c, requestParams, params);
                    } else {
                      return buildSimpleCondition(c, requestParams, params);
                    }
                  })
              .filter(Objects::nonNull)
              .filter(s -> !s.isBlank())
              .toList();

      boolean isOptional = filterNode.path("optional").asBoolean(false);
      if (clauses.isEmpty() && isOptional) {
        return null;
      }

      if (clauses.isEmpty()) {
        return "";
      }

      if (clauses.size() == 1 && !filterNode.get("conditions").get(0).has("conditions")) {
        return clauses.get(0);
      }

      return "(" + String.join(" " + op + " ", clauses) + ")";
    }
    return buildSimpleCondition(filterNode, requestParams, params);
  }

  private static String buildSimpleCondition(
      JsonNode condition, Map<String, Object> requestParams, Map<String, Object> params) {
    boolean isOptional = condition.path("optional").asBoolean(false);

    if (condition.has("expression")) {
      String expression = condition.get("expression").asText();
      return extractAndBindParams(expression, requestParams, params, isOptional);
    }

    if (!condition.has("field") || !condition.has("op")) {
      return null;
    }

    String field = condition.get("field").asText();
    String op = condition.get("op").asText();

    if (condition.has("param")) {
      String pName = condition.get("param").asText();
      Object paramValue = requestParams.get(pName);

      if (paramValue == null || (paramValue instanceof String s && s.isBlank())) {
        return isOptional ? null : "";
      }

      if (condition.has("transform")) {
        JsonNode transform = condition.get("transform");
        String type = transform.path("type").asText();

        switch (type) {
          case "concat":
            String prefix = transform.path("prefix").asText("");
            String suffix = transform.path("suffix").asText("");
            paramValue = prefix + paramValue + suffix;
            break;
          case "plainto_tsquery":
            paramValue = "plainto_tsquery('" + paramValue.toString().replace("'", "''") + "')";
            break;
          case "wrap":
            String wrapPattern = transform.path("pattern").asText("%{value}%");
            paramValue = wrapPattern.replace("{value}", paramValue.toString());
            break;
        }
      }

      params.put(pName, convertValue(paramValue));
      return field + " " + op + " :" + pName;
    }

    return condition.has("value")
        ? field + " " + op + " '" + condition.get("value").asText() + "'"
        : null;
  }

  private static void buildHaving(
      StringBuilder sql,
      JsonNode template,
      Map<String, Object> requestParams,
      Map<String, Object> params) {
    if (template.has("having")) {
      JsonNode havingArray = template.get("having");
      if (havingArray.isArray()) {
        List<String> conditions = new ArrayList<>();

        for (JsonNode condition : havingArray) {
          String clause = buildHavingCondition(condition, requestParams, params);
          if (clause != null && !clause.isBlank()) {
            conditions.add(clause);
          }
        }

        if (!conditions.isEmpty()) {
          sql.append(" HAVING ").append(String.join(" AND ", conditions));
        }
      }
    }
  }

  private static String buildHavingCondition(
      JsonNode condition, Map<String, Object> requestParams, Map<String, Object> params) {
    boolean isOptional = condition.path("optional").asBoolean(false);

    if (condition.has("expression")) {
      String expression = condition.get("expression").asText();
      return extractAndBindParams(expression, requestParams, params, isOptional);
    }

    if (condition.has("field") && condition.has("op")) {
      String field = condition.get("field").asText();
      String op = condition.get("op").asText();

      if (condition.has("param")) {
        String paramName = condition.get("param").asText();
        Object paramValue = requestParams.get(paramName);

        if (paramValue == null || (paramValue instanceof String s && s.isBlank())) {
          return isOptional ? null : "";
        }

        params.put(paramName, convertValue(paramValue));
        return field + " " + op + " :" + paramName;
      }

      if (condition.has("value")) {
        return field + " " + op + " " + condition.get("value").asText();
      }
    }

    return null;
  }

  private static String extractAndBindParams(
      String expression,
      Map<String, Object> requestParams,
      Map<String, Object> params,
      boolean isOptional) {
    Set<String> paramNames = new HashSet<>();
    Pattern paramPattern = Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");
    Matcher matcher = paramPattern.matcher(expression);

    while (matcher.find()) {
      paramNames.add(matcher.group(1));
    }

    if (paramNames.isEmpty()) {
      return expression;
    }

    boolean allParamsValid =
        paramNames.stream()
            .allMatch(
                p -> {
                  Object val = requestParams.get(p);
                  return val != null && !(val instanceof String s && s.isBlank());
                });

    if (!allParamsValid) {
      if (isOptional) {
        return null;
      } else {
        Set<String> problematic =
            paramNames.stream()
                .filter(
                    p -> {
                      Object val = requestParams.get(p);
                      return val == null || (val instanceof String s && s.isBlank());
                    })
                .collect(Collectors.toSet());
        throw new IllegalArgumentException(
            "Missing or empty required parameters for expression: " + problematic);
      }
    }

    for (String paramName : paramNames) {
      Object value = requestParams.get(paramName);
      params.put(paramName, convertValue(value));
    }

    return expression;
  }

  private static void buildSource(StringBuilder sql, JsonNode template) {
    JsonNode source = template.get("source");
    sql.append(source.get("table").asText());
    if (source.has("alias")) sql.append(" ").append(source.get("alias").asText());
  }

  private static void buildJoins(StringBuilder sql, JsonNode template) {
    Optional.ofNullable(template.get("joins"))
        .filter(JsonNode::isArray)
        .ifPresent(
            joins -> {
              for (JsonNode join : joins) {

                if (!join.has("type") || !join.has("table")) {
                  throw new IllegalArgumentException(
                      "Join definition missing required fields (type or table)");
                }

                String type = join.get("type").asText();
                String table = join.get("table").asText();
                boolean isCrossJoin = "CROSS".equalsIgnoreCase(type);

                sql.append(" ").append(type).append(" JOIN ").append(table);

                if (join.has("alias")) {
                  sql.append(" ").append(join.get("alias").asText());
                }

                if (join.has("on")) {
                  sql.append(" ON ");
                  String on =
                      StreamSupport.stream(join.get("on").spliterator(), false)
                          .map(
                              o -> {
                                if (!o.has("left") || !o.has("op") || !o.has("right")) {
                                  throw new IllegalArgumentException(
                                      "Join condition missing required fields (left, op, or right)");
                                }
                                String left = o.get("left").asText();
                                String op = o.get("op").asText();
                                String right = o.get("right").asText();
                                return left + " " + op + " " + right;
                              })
                          .collect(Collectors.joining(" AND "));

                  if (on.isEmpty()) {
                    throw new IllegalArgumentException("Join ON clause cannot be empty");
                  }
                  sql.append(on);
                } else if (!isCrossJoin) {

                  throw new IllegalArgumentException(type + " JOIN requires 'on' clause");
                }
              }
            });
  }

  private static void buildPagination(
      StringBuilder sql, JsonNode template, Map<String, Object> requestParams) {
    Integer limit = null;
    if (template.has("limit")) {
      JsonNode limitNode = template.get("limit");
      limit = extractIntegerValue(limitNode, requestParams, "limit");
    }

    Integer offset = null;
    if (template.has("offset")) {
      JsonNode offsetNode = template.get("offset");
      offset = calculateOffset(offsetNode, requestParams, limit);
    }

    if (limit != null) {
      sql.append(" LIMIT ").append(limit);
      if (offset != null && offset > 0) sql.append(" OFFSET ").append(offset);
    }
  }

  private static Integer calculateOffset(
      JsonNode offsetNode, Map<String, Object> requestParams, Integer limit) {

    if (offsetNode.has("calculated") && "offset".equals(offsetNode.get("calculated").asText())) {
      if (limit != null) {
        Integer page = parseInteger(requestParams.get("page"));
        if (page != null && page > 0) {
          return (page - 1) * limit;
        }
      }
      return offsetNode.has("default") ? offsetNode.get("default").asInt() : 0;
    }

    return extractIntegerValue(offsetNode, requestParams, "offset");
  }

  private static Integer extractIntegerValue(
      JsonNode node, Map<String, Object> requestParams, String defaultParamName) {
    if (node.has("param")) {
      String paramName = node.get("param").asText();
      Integer paramValue = parseInteger(requestParams.get(paramName));
      return Optional.ofNullable(paramValue)
          .orElseGet(() -> node.has("default") ? node.get("default").asInt() : null);
    }
    if (node.isInt()) {
      return node.asInt();
    }
    if (node.isTextual()) {
      try {
        return Integer.parseInt(node.asText());
      } catch (NumberFormatException e) {
        return null;
      }
    }
    if (node.has("default")) {
      return node.get("default").asInt();
    }
    return null;
  }

  private static Integer extractIntegerValue(
      JsonNode node, Map<String, Object> requestParams, Integer limit) {
    if (node.has("calculated")) {
      String calculatedType = node.get("calculated").asText();

      switch (calculatedType) {
        case "offset":
          if (limit != null) {
            Integer page = parseInteger(requestParams.get("page"));
            if (page != null && page > 0) {
              return (page - 1) * limit;
            }
          }
          break;
        case "pageSize":
          break;
      }
      return node.has("default") ? node.get("default").asInt() : 0;
    }

    if (node.has("param")) {
      String paramName = node.get("param").asText();
      Integer paramValue = parseInteger(requestParams.get(paramName));
      return Optional.ofNullable(paramValue)
          .orElseGet(() -> node.has("default") ? node.get("default").asInt() : null);
    }

    return node.has("default") ? node.get("default").asInt() : null;
  }

  private static void buildGroupBy(StringBuilder sql, JsonNode template) {
    Optional.ofNullable(template.get("groupBy"))
        .filter(JsonNode::isArray)
        .ifPresent(
            gb -> {
              String groups =
                  StreamSupport.stream(gb.spliterator(), false)
                      .map(JsonNode::asText)
                      .collect(Collectors.joining(", "));
              if (!groups.isBlank()) sql.append(" GROUP BY ").append(groups);
            });
  }

  private static void buildOrderBy(
      StringBuilder sql, JsonNode template, Map<String, Object> requestParams) {
    if (template.has("sort")) {
      JsonNode sortNode = template.get("sort");

      if (sortNode.isTextual() && sortNode.asText().startsWith("{param:")) {
        String sortParamValue = extractParamWithDefault(sortNode.asText(), requestParams);
        if (sortParamValue != null && !sortParamValue.isBlank()) {
          String orderByClause = parseSortObject(sortParamValue);
          if (!orderByClause.isBlank()) {
            sql.append(" ORDER BY ").append(orderByClause);
          }
        }
      } else if (sortNode.isObject()) {
        String orderByClause = parseSortObject(sortNode.toString());
        if (!orderByClause.isBlank()) {
          sql.append(" ORDER BY ").append(orderByClause);
        }
      } else if (sortNode.isArray()) {
        String orderByClause = parseSortArray(sortNode);
        if (!orderByClause.isBlank()) {
          sql.append(" ORDER BY ").append(orderByClause);
        }
      }
    }
  }

  private static String parseSortObject(String sortParam) {
    if (sortParam == null || sortParam.isBlank()) {
      return "";
    }

    String cleanParam = sortParam.trim();

    if (cleanParam.startsWith("{") && !cleanParam.endsWith("}")) {
      cleanParam = cleanParam + "}";
    }

    cleanParam = cleanParam.replace("'", "\"");

    try {
      Map<String, String> sortMap =
          MAPPER.readValue(cleanParam, new TypeReference<LinkedHashMap<String, String>>() {});

      return sortMap.entrySet().stream()
          .map(entry -> entry.getKey() + " " + normalizeDirection(entry.getValue()))
          .collect(Collectors.joining(", "));

    } catch (Exception e) {
      System.err.println("Failed to parse sort object: " + e.getMessage());
      System.err.println("Cleaned input was: " + cleanParam);
      return "";
    }
  }

  private static String normalizeDirection(String direction) {
    if (direction == null) return "ASC";

    String normalized = direction.trim().toUpperCase();
    return "ASC".equals(normalized) || "DESC".equals(normalized) ? normalized : "ASC";
  }

  private static String parseSortObjectAlternative(String sortParam) {
    try {
      JsonNode sortObj = MAPPER.readTree(sortParam);
      if (sortObj.isObject()) {
        List<String> sortItems = new ArrayList<>();

        Iterator<Map.Entry<String, JsonNode>> fields = sortObj.fields();
        while (fields.hasNext()) {
          Map.Entry<String, JsonNode> entry = fields.next();
          String field = entry.getKey();
          String direction = entry.getValue().asText().toUpperCase();

          if (!"ASC".equals(direction) && !"DESC".equals(direction)) {
            direction = "ASC";
          }

          sortItems.add(field + " " + direction);
        }

        return String.join(", ", sortItems);
      }
    } catch (Exception e) {
      System.err.println("Failed to parse sort object: " + e.getMessage());
    }
    return "";
  }

  private static String parseSortArray(JsonNode sortArray) {
    if (sortArray.isArray()) {
      List<String> sortItems = new ArrayList<>();

      for (JsonNode item : sortArray) {
        if (item.isObject()) {
          String field = item.path("field").asText();
          String direction = item.path("direction").asText("ASC").toUpperCase();

          if (!"ASC".equals(direction) && !"DESC".equals(direction)) {
            direction = "ASC";
          }

          sortItems.add(field + " " + direction);
        }
      }

      return String.join(", ", sortItems);
    }
    return "";
  }

  private static String extractParamWithDefault(
      String template, Map<String, Object> requestParams) {
    String content = template.substring(1, template.length() - 1);

    String[] parts = content.split(",", 2);
    String paramPart = parts[0].trim();

    if (!paramPart.startsWith("param:")) {
      return template;
    }

    String paramName = paramPart.substring(6).trim();

    Object paramValue = requestParams.get(paramName);
    if (paramValue != null) {
      return paramValue.toString();
    }

    if (parts.length > 1) {
      String defaultPart = parts[1].trim();
      if (defaultPart.startsWith("default:")) {
        String defaultValue = defaultPart.substring(8).trim();
        if ((defaultValue.startsWith("'") && defaultValue.endsWith("'"))
            || (defaultValue.startsWith("\"") && defaultValue.endsWith("\""))) {
          defaultValue = defaultValue.substring(1, defaultValue.length() - 1);
        }
        return defaultValue;
      }
    }

    return "";
  }

  private static void buildReturning(StringBuilder sql, JsonNode template) {
    Optional.ofNullable(template.get("returning"))
        .filter(JsonNode::isArray)
        .ifPresent(
            r -> {
              String fields =
                  StreamSupport.stream(r.spliterator(), false)
                      .map(JsonNode::asText)
                      .collect(Collectors.joining(", "));
              if (!fields.isBlank()) sql.append(" RETURNING ").append(fields);
            });
  }

  private static String resolveMcpDirection(
      String directionTemplate, Map<String, Object> requestParams) {
    if (directionTemplate == null || !directionTemplate.startsWith("{param:")) {
      return directionTemplate != null ? directionTemplate : "ASC";
    }

    String param = "sort";
    String defaultVal = "ASC";
    Map<String, String> values = new HashMap<>();

    String content = directionTemplate.substring(1, directionTemplate.length() - 1);
    for (String part : content.split(",")) {
      String[] kv = part.split(":", 2);
      if (kv.length < 2) continue;
      String key = kv[0].trim();
      String val = kv[1].trim().replaceAll("'", "");

      switch (key) {
        case "param" -> param = val;
        case "default" -> defaultVal = val;
        case "values" -> {
          String vals = val.substring(1, val.length() - 1);
          for (String entry : vals.split(",")) {
            String[] v = entry.split(":");
            if (v.length == 2) {
              values.put(v[0].trim().replaceAll("'", ""), v[1].trim().replaceAll("'", ""));
            }
          }
        }
      }
    }

    Object requestVal = requestParams.get(param);
    if (requestVal != null && values.containsKey(requestVal.toString().toLowerCase())) {
      return values.get(requestVal.toString().toLowerCase());
    }
    return defaultVal;
  }

  private static Set<String> parseFields(Object fieldsParam) {
    if (fieldsParam == null) return new HashSet<>();
    return switch (fieldsParam) {
      case String s when !s.isBlank() ->
          Arrays.stream(s.replaceAll("[\\[\\]]", "").split(","))
              .map(String::trim)
              .filter(str -> !str.isEmpty())
              .collect(Collectors.toSet());
      case Collection<?> c ->
          c.stream().map(Object::toString).map(String::trim).collect(Collectors.toSet());
      case String[] arr -> Arrays.stream(arr).map(String::trim).collect(Collectors.toSet());
      default -> new HashSet<>();
    };
  }

  private static Integer parseInteger(Object value) {
    if (value == null) return null;
    return switch (value) {
      case Integer i -> i;
      case Number n -> n.intValue();
      case String s when !s.isBlank() -> {
        try {
          yield Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
          yield null;
        }
      }
      default -> null;
    };
  }

  private static Object convertValue(Object value) {
    if (value == null) return null;
    return switch (value) {
      case String s when s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false") ->
          Boolean.parseBoolean(s);

      case String s when s.matches("^\\d{4}-\\d{2}-\\d{2}.*") -> {
        try {
          if (s.contains("+") || s.endsWith("Z")) {
            yield OffsetDateTime.parse(s);
          }
          yield LocalDateTime.parse(s);
        } catch (DateTimeParseException e) {
          yield s;
        }
      }

      case String s -> {
        try {
          yield s.contains(".") ? Double.parseDouble(s) : Long.parseLong(s);
        } catch (NumberFormatException e) {
          yield s;
        }
      }

      case null -> null;
      default -> value;
    };
  }
}
