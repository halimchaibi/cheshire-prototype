QueryResult result = SqlTemplateQueryBuilder.buildQuery(template, httpParams);

// Option 1: Spring NamedParameterJdbcTemplate
NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(dataSource);
List<Map<String, Object>> rows = jdbc.queryForList(
result.sql(), // "SELECT * FROM artists WHERE name = :name"
result.parameters()    // {name: "%david%"}
);

// Option 2: Plain JDBC PreparedStatement
PreparedStatement stmt = connection.prepareStatement(
result.sql().replace(":name", "?")  // Convert to ? placeholders
);
stmt.setString(1, result.parameters().get("name"));
ResultSet rs = stmt.executeQuery();

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

/**

* Thread-safe static SQL query builder from JSON templates.
* All methods are static - no instance creation needed.
  */
  public final class SqlTemplateQueryBuilder {

  // ObjectMapper is thread-safe - single static instance
  private static final ObjectMapper MAPPER = new ObjectMapper();

  // Prevent instantiation
  private SqlTemplateQueryBuilder() {
  throw new AssertionError("Utility class - do not instantiate");
  }

  /**
    * Main entry point: builds SQL from template + HTTP params
    * Thread-safe and stateless - can be called concurrently
      */
      public static QueryResult buildQuery(String templateJson, Map<String, String> httpParams) throws Exception {
      JsonNode template = MAPPER.readTree(templateJson);

      Set<String> requestedFields = parseFields(httpParams.get("fields"));

      StringBuilder sql = new StringBuilder("SELECT ");
      Map<String, Object> params = new LinkedHashMap<>();

      buildProjection(sql, template, requestedFields);
      sql.append(" FROM ");
      buildSource(sql, template);
      buildJoins(sql, template);
      buildFilters(sql, template, httpParams, params);
      buildGroupBy(sql, template);
      buildHaving(sql, template, httpParams, params);
      buildOrderBy(sql, template);
      buildPagination(sql, template, httpParams);

      return new QueryResult(sql.toString(), params);
      }

  private static void buildProjection(StringBuilder sql, JsonNode template, Set<String> requestedFields) {
  JsonNode projection = template.get("projection");
  JsonNode aggregates = template.get("aggregates");

       List<String> selectItems = new ArrayList<>();

       if (aggregates != null && aggregates.isArray()) {
           for (JsonNode agg : aggregates) {
               String func = agg.get("func").asText();
               String field = agg.get("field").asText();
               String alias = agg.has("alias") ? agg.get("alias").asText() : null;

               String aggExpr = func + "(" + field + ")";
               if (alias != null) {
                   aggExpr += " AS " + alias;
               }
               selectItems.add(aggExpr);
           }
       }

       if (projection != null && projection.isArray()) {
           for (JsonNode proj : projection) {
               String field = proj.get("field").asText();
               String alias = proj.has("alias") ? proj.get("alias").asText() : null;

               if (!requestedFields.isEmpty()) {
                   String checkName = alias != null ? alias : field;
                   if (!requestedFields.contains(checkName)) {
                       continue;
                   }
               }

               String projExpr = field;
               if (alias != null && !field.equals(alias)) {
                   projExpr += " AS " + alias;
               }
               selectItems.add(projExpr);
           }
       }

       sql.append(selectItems.isEmpty() ? "*" : String.join(", ", selectItems));
  }

  private static void buildSource(StringBuilder sql, JsonNode template) {
  JsonNode source = template.get("source");
  sql.append(source.get("table").asText());
  if (source.has("alias")) {
  sql.append(" ").append(source.get("alias").asText());
  }
  }

  private static void buildJoins(StringBuilder sql, JsonNode template) {
  JsonNode joins = template.get("joins");
  if (joins == null || !joins.isArray()) return;

       for (JsonNode join : joins) {
           sql.append(" ").append(join.get("type").asText()).append(" JOIN ");
           sql.append(join.get("table").asText());

           if (join.has("alias")) {
               sql.append(" ").append(join.get("alias").asText());
           }

           sql.append(" ON ");
           JsonNode onConditions = join.get("on");
           List<String> onClauses = new ArrayList<>();

           for (JsonNode on : onConditions) {
               String left = on.get("left").asText();
               String op = on.get("op").asText();
               String right = on.get("right").asText();
               onClauses.add(left + " " + op + " " + right);
           }

           sql.append(String.join(" AND ", onClauses));
       }
  }

  private static void buildFilters(StringBuilder sql, JsonNode template, Map<String, String> httpParams,
  Map<String, Object> params) {
  JsonNode filters = template.get("filters");
  if (filters == null) return;

       String whereClause = buildConditions(filters, httpParams, params);
       if (!whereClause.isEmpty()) {
           sql.append(" WHERE ").append(whereClause);
       }
  }

  private static String buildConditions(JsonNode filterNode, Map<String, String> httpParams,
  Map<String, Object> params) {
  if (filterNode.has("conditions")) {
  String op = filterNode.get("op").asText();
  JsonNode conditions = filterNode.get("conditions");

           List<String> clauses = new ArrayList<>();
           for (JsonNode condition : conditions) {
               if (condition.has("conditions")) {
                   String nested = buildConditions(condition, httpParams, params);
                   if (!nested.isEmpty()) {
                       clauses.add("(" + nested + ")");
                   }
               } else {
                   String clause = buildSimpleCondition(condition, httpParams, params);
                   if (clause != null) {
                       clauses.add(clause);
                   }
               }
           }

           return clauses.isEmpty() ? "" : String.join(" " + op + " ", clauses);
       } else {
           String clause = buildSimpleCondition(filterNode, httpParams, params);
           return clause != null ? clause : "";
       }
  }

  private static String buildSimpleCondition(JsonNode condition, Map<String, String> httpParams,
  Map<String, Object> params) {
  String field = condition.get("field").asText();
  String op = condition.get("op").asText();

       if (condition.has("param")) {
           String paramName = condition.get("param").asText();
           String paramValue = httpParams.get(paramName);

           if (paramValue == null) {
               return null; // Skip condition if param not provided
           }

           String placeholder = ":" + paramName;
           params.put(paramName, convertValue(paramValue));

           return field + " " + op + " " + placeholder;
       } else if (condition.has("value")) {
           String value = condition.get("value").asText();
           return field + " " + op + " '" + value + "'";
       }

       return null;
  }

  private static void buildGroupBy(StringBuilder sql, JsonNode template) {
  JsonNode groupBy = template.get("groupBy");
  if (groupBy == null || !groupBy.isArray() || groupBy.isEmpty()) return;

       List<String> groups = new ArrayList<>();
       for (JsonNode field : groupBy) {
           groups.add(field.asText());
       }

       sql.append(" GROUP BY ").append(String.join(", ", groups));
  }

  private static void buildHaving(StringBuilder sql, JsonNode template, Map<String, String> httpParams,
  Map<String, Object> params) {
  JsonNode having = template.get("having");
  if (having == null || !having.isArray() || having.isEmpty()) return;

       List<String> conditions = new ArrayList<>();
       for (JsonNode condition : having) {
           String clause = buildSimpleCondition(condition, httpParams, params);
           if (clause != null) {
               conditions.add(clause);
           }
       }

       if (!conditions.isEmpty()) {
           sql.append(" HAVING ").append(String.join(" AND ", conditions));
       }
  }

  private static void buildOrderBy(StringBuilder sql, JsonNode template) {
  JsonNode sort = template.get("sort");
  if (sort == null || !sort.isArray() || sort.isEmpty()) return;

       List<String> orders = new ArrayList<>();
       for (JsonNode order : sort) {
           String field = order.get("field").asText();
           String direction = order.has("direction") ? order.get("direction").asText() : "ASC";
           orders.add(field + " " + direction);
       }

       sql.append(" ORDER BY ").append(String.join(", ", orders));
  }

  private static void buildPagination(StringBuilder sql, JsonNode template, Map<String, String> httpParams) {
  Integer limit = httpParams.containsKey("limit")
  ? Integer.parseInt(httpParams.get("limit"))
  : (template.has("limit") ? template.get("limit").asInt() : null);

       Integer offset = httpParams.containsKey("offset")
           ? Integer.parseInt(httpParams.get("offset"))
           : (template.has("offset") ? template.get("offset").asInt() : null);

       if (limit != null) {
           sql.append(" LIMIT ").append(limit);
       }

       if (offset != null && offset > 0) {
           sql.append(" OFFSET ").append(offset);
       }
  }

  private static Set<String> parseFields(String fieldsParam) {
  Set<String> fields = new HashSet<>();
  if (fieldsParam == null || fieldsParam.isEmpty()) {
  return fields;
  }

       String cleaned = fieldsParam.replaceAll("[\\[\\]]", "");
       for (String field : cleaned.split(",")) {
           fields.add(field.trim());
       }

       return fields;
  }

  private static Object convertValue(String value) {
  if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
  return Boolean.parseBoolean(value);
  }

       try {
           if (value.contains(".")) {
               return Double.parseDouble(value);
           } else {
               return Long.parseLong(value);
           }
       } catch (NumberFormatException e) {
           return value;
       }
  }

  /**
    * Result record (Java 16+)
      */
      public record QueryResult(String sql, Map<String, Object> parameters) {
      @Override
      public String toString() {
      return "SQL: " + sql + "\nParams: " + parameters;
      }
      }

  // Example usage
  public static void main(String[] args) throws Exception {
  String template = """
  {
  "source": { "table": "artists", "alias": "a" },
  "projection": [
  { "field": "a.id", "alias": "id" },
  { "field": "a.name", "alias": "name" },
  { "field": "a.country", "alias": "country" }
  ],
  "filters": {
  "op": "AND",
  "conditions": [
  { "field": "a.name", "op": "LIKE", "param": "name" },
  { "field": "a.country", "op": "=", "param": "country" }
  ]
  },
  "sort": [
  { "field": "a.name", "direction": "ASC" }
  ],
  "limit": 50
  }
  """;

       Map<String, String> httpParams = Map.of(
           "name", "%david%",
           "fields", "[id,name]"
       );

       // Just call static method directly - no instance needed!
       QueryResult result = SqlTemplateQueryBuilder.buildQuery(template, httpParams);

       System.out.println(result);
  }
  }

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

public class SqlTemplateQueryBuilder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Main entry point: builds SQL from template + HTTP params
     */
    public QueryResult buildQuery(String templateJson, Map<String, String> httpParams) throws Exception {
        JsonNode template = objectMapper.readTree(templateJson);

        // Parse dynamic fields if provided
        Set<String> requestedFields = parseFields(httpParams.get("fields"));

        // Build SQL components
        StringBuilder sql = new StringBuilder("SELECT ");
        Map<String, Object> params = new LinkedHashMap<>();

        // Projection (SELECT clause)
        buildProjection(sql, template, requestedFields);

        // FROM clause
        sql.append(" FROM ");
        buildSource(sql, template);

        // JOINs
        buildJoins(sql, template);

        // WHERE clause
        buildFilters(sql, template, httpParams, params);

        // GROUP BY
        buildGroupBy(sql, template);

        // HAVING
        buildHaving(sql, template, httpParams, params);

        // ORDER BY
        buildOrderBy(sql, template);

        // LIMIT/OFFSET
        buildPagination(sql, template, httpParams);

        return new QueryResult(sql.toString(), params);
    }

    private void buildProjection(StringBuilder sql, JsonNode template, Set<String> requestedFields) {
        JsonNode projection = template.get("projection");
        JsonNode aggregates = template.get("aggregates");

        List<String> selectItems = new ArrayList<>();

        // Add aggregates first if present
        if (aggregates != null && aggregates.isArray()) {
            for (JsonNode agg : aggregates) {
                String func = agg.get("func").asText();
                String field = agg.get("field").asText();
                String alias = agg.has("alias") ? agg.get("alias").asText() : null;

                String aggExpr = func + "(" + field + ")";
                if (alias != null) {
                    aggExpr += " AS " + alias;
                }
                selectItems.add(aggExpr);
            }
        }

        // Add projection fields
        if (projection != null && projection.isArray()) {
            for (JsonNode proj : projection) {
                String field = proj.get("field").asText();
                String alias = proj.has("alias") ? proj.get("alias").asText() : null;

                // If specific fields requested, filter
                if (!requestedFields.isEmpty()) {
                    String checkName = alias != null ? alias : field;
                    if (!requestedFields.contains(checkName)) {
                        continue;
                    }
                }

                String projExpr = field;
                if (alias != null && !field.equals(alias)) {
                    projExpr += " AS " + alias;
                }
                selectItems.add(projExpr);
            }
        }

        sql.append(selectItems.isEmpty() ? "*" : String.join(", ", selectItems));
    }

    private void buildSource(StringBuilder sql, JsonNode template) {
        JsonNode source = template.get("source");
        sql.append(source.get("table").asText());
        if (source.has("alias")) {
            sql.append(" ").append(source.get("alias").asText());
        }
    }

    private void buildJoins(StringBuilder sql, JsonNode template) {
        JsonNode joins = template.get("joins");
        if (joins == null || !joins.isArray()) return;

        for (JsonNode join : joins) {
            sql.append(" ").append(join.get("type").asText()).append(" JOIN ");
            sql.append(join.get("table").asText());

            if (join.has("alias")) {
                sql.append(" ").append(join.get("alias").asText());
            }

            sql.append(" ON ");
            JsonNode onConditions = join.get("on");
            List<String> onClauses = new ArrayList<>();

            for (JsonNode on : onConditions) {
                String left = on.get("left").asText();
                String op = on.get("op").asText();
                String right = on.get("right").asText();
                onClauses.add(left + " " + op + " " + right);
            }

            sql.append(String.join(" AND ", onClauses));
        }
    }

    private void buildFilters(StringBuilder sql, JsonNode template, Map<String, String> httpParams,
                             Map<String, Object> params) {
        JsonNode filters = template.get("filters");
        if (filters == null) return;

        String whereClause = buildConditions(filters, httpParams, params);
        if (!whereClause.isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
    }

    private String buildConditions(JsonNode filterNode, Map<String, String> httpParams,
                                   Map<String, Object> params) {
        if (filterNode.has("conditions")) {
            // Complex condition with AND/OR
            String op = filterNode.get("op").asText();
            JsonNode conditions = filterNode.get("conditions");

            List<String> clauses = new ArrayList<>();
            for (JsonNode condition : conditions) {
                if (condition.has("conditions")) {
                    // Nested condition
                    String nested = buildConditions(condition, httpParams, params);
                    if (!nested.isEmpty()) {
                        clauses.add("(" + nested + ")");
                    }
                } else {
                    // Simple condition
                    String clause = buildSimpleCondition(condition, httpParams, params);
                    if (clause != null) {
                        clauses.add(clause);
                    }
                }
            }

            return clauses.isEmpty() ? "" : String.join(" " + op + " ", clauses);
        } else {
            // Single condition
            String clause = buildSimpleCondition(filterNode, httpParams, params);
            return clause != null ? clause : "";
        }
    }

    private String buildSimpleCondition(JsonNode condition, Map<String, String> httpParams,
                                       Map<String, Object> params) {
        String field = condition.get("field").asText();
        String op = condition.get("op").asText();

        if (condition.has("param")) {
            // Dynamic parameter from HTTP
            String paramName = condition.get("param").asText();
            String paramValue = httpParams.get(paramName);

            if (paramValue == null) {
                return null; // Skip condition if param not provided
            }

            String placeholder = ":" + paramName;
            params.put(paramName, convertValue(paramValue));

            return field + " " + op + " " + placeholder;
        } else if (condition.has("value")) {
            // Static value
            String value = condition.get("value").asText();
            return field + " " + op + " '" + value + "'";
        }

        return null;
    }

    private void buildGroupBy(StringBuilder sql, JsonNode template) {
        JsonNode groupBy = template.get("groupBy");
        if (groupBy == null || !groupBy.isArray() || groupBy.isEmpty()) return;

        List<String> groups = new ArrayList<>();
        for (JsonNode field : groupBy) {
            groups.add(field.asText());
        }

        sql.append(" GROUP BY ").append(String.join(", ", groups));
    }

    private void buildHaving(StringBuilder sql, JsonNode template, Map<String, String> httpParams,
                            Map<String, Object> params) {
        JsonNode having = template.get("having");
        if (having == null || !having.isArray() || having.isEmpty()) return;

        List<String> conditions = new ArrayList<>();
        for (JsonNode condition : having) {
            String clause = buildSimpleCondition(condition, httpParams, params);
            if (clause != null) {
                conditions.add(clause);
            }
        }

        if (!conditions.isEmpty()) {
            sql.append(" HAVING ").append(String.join(" AND ", conditions));
        }
    }

    private void buildOrderBy(StringBuilder sql, JsonNode template) {
        JsonNode sort = template.get("sort");
        if (sort == null || !sort.isArray() || sort.isEmpty()) return;

        List<String> orders = new ArrayList<>();
        for (JsonNode order : sort) {
            String field = order.get("field").asText();
            String direction = order.has("direction") ? order.get("direction").asText() : "ASC";
            orders.add(field + " " + direction);
        }

        sql.append(" ORDER BY ").append(String.join(", ", orders));
    }

    private void buildPagination(StringBuilder sql, JsonNode template, Map<String, String> httpParams) {
        // Check HTTP params first, then template defaults
        Integer limit = httpParams.containsKey("limit")
            ? Integer.parseInt(httpParams.get("limit"))
            : (template.has("limit") ? template.get("limit").asInt() : null);

        Integer offset = httpParams.containsKey("offset")
            ? Integer.parseInt(httpParams.get("offset"))
            : (template.has("offset") ? template.get("offset").asInt() : null);

        if (limit != null) {
            sql.append(" LIMIT ").append(limit);
        }

        if (offset != null && offset > 0) {
            sql.append(" OFFSET ").append(offset);
        }
    }

    private Set<String> parseFields(String fieldsParam) {
        Set<String> fields = new HashSet<>();
        if (fieldsParam == null || fieldsParam.isEmpty()) {
            return fields;
        }

        // Remove brackets and split: [id,name] -> ["id", "name"]
        String cleaned = fieldsParam.replaceAll("[\\[\\]]", "");
        for (String field : cleaned.split(",")) {
            fields.add(field.trim());
        }

        return fields;
    }

    private Object convertValue(String value) {
        // Try to detect type
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }

        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            return value; // Keep as string
        }
    }

    /**
     * Result container
     */
    public static class QueryResult {
        private final String sql;
        private final Map<String, Object> parameters;

        public QueryResult(String sql, Map<String, Object> parameters) {
            this.sql = sql;
            this.parameters = parameters;
        }

        public String getSql() {
            return sql;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        @Override
        public String toString() {
            return "SQL: " + sql + "\nParams: " + parameters;
        }
    }

    // Example usage
    public static void main(String[] args) throws Exception {
        String template = """
        {
          "source": { "table": "artists", "alias": "a" },
          "projection": [
            { "field": "a.id", "alias": "id" },
            { "field": "a.name", "alias": "name" },
            { "field": "a.country", "alias": "country" }
          ],
          "filters": {
            "op": "AND",
            "conditions": [
              { "field": "a.name", "op": "LIKE", "param": "name" },
              { "field": "a.country", "op": "=", "param": "country" }
            ]
          },
          "sort": [
            { "field": "a.name", "direction": "ASC" }
          ],
          "limit": 50
        }
        """;

        Map<String, String> httpParams = new HashMap<>();
        httpParams.put("name", "%david%");
        httpParams.put("fields", "[id,name]");
        // country param omitted - will skip that filter

        SqlTemplateQueryBuilder builder = new SqlTemplateQueryBuilder();
        QueryResult result = builder.buildQuery(template, httpParams);

        System.out.println(result);
        // Output:
        // SQL: SELECT a.id AS id, a.name AS name FROM artists a WHERE a.name LIKE :name ORDER BY a.name ASC LIMIT 50
        // Params: {name=%david%}
    }

}
