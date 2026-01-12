**domain-neutral, Calcite-friendly query template specification** to use **as documentation and as a reference model**.
It is **not SQL**, on purpose. It is a **generic relational query template** that maps cleanly to `RelBuilder`.

Illustrate across schemas, databases, and domains queries.

---

## 1. Top-level query template

```json
{
  "source": "T_MAIN",
  "joins": [],
  "filters": [],
  "groupBy": [],
  "aggregates": [],
  "having": [],
  "projection": [],
  "sort": [],
  "limit": null,
  "offset": null
}
```

---

## 2. Source (FROM)

```json
"source": {
  "table": "T_MAIN",
  "alias": "m"
}
```

* `table` → logical table name (schema resolved at runtime)
* `alias` → optional, but recommended for joins

---

## 3. Joins

```json
"joins": [
  {
    "type": "INNER",              // INNER | LEFT | RIGHT | FULL
    "table": "T_JOIN",
    "alias": "j",
    "on": [
      {
        "left": "m.COL_ID",
        "op": "=",
        "right": "j.COL_REF_ID"
      }
    ]
  }
]
```

Supported join operators:

* `=`, `!=`, `<`, `<=`, `>`, `>=`

---

## 4. Filters (WHERE)

```json
"filters": [
  {
    "field": "m.COL_STATUS",
    "op": "=",
    "param": "status"
  },
  {
    "field": "m.COL_DATE",
    "op": ">=",
    "param": "fromDate"
  }
]
```

Supported operators:

* `=`, `!=`
* `<`, `<=`, `>`, `>=`
* `IN`
* `LIKE`
* `BETWEEN`
* `IS_NULL`, `IS_NOT_NULL`

Values are **always parameters**, never literals.

---

## 5. Logical filter groups (AND / OR)

```json
"filters": {
  "op": "AND",
  "conditions": [
    {
      "field": "m.COL_A",
      "op": "=",
      "param": "a"
    },
    {
      "op": "OR",
      "conditions": [
        { "field": "m.COL_B", "op": ">", "param": "b" },
        { "field": "m.COL_C", "op": "<", "param": "c" }
      ]
    }
  ]
}
```

This maps directly to nested `RexNode` trees.

---

## 6. Projection (SELECT)

```json
"projection": [
  { "field": "m.COL_ID", "alias": "id" },
  { "field": "m.COL_NAME", "alias": "name" }
]
```

Or expressions:

```json
{
  "expr": {
    "op": "COALESCE",
    "args": ["m.COL_DESC", "''"]
  },
  "alias": "description"
}
```

---

## 7. Aggregates (GROUP BY)

```json
"groupBy": [
  "m.COL_CATEGORY"
],
"aggregates": [
  {
    "func": "COUNT",
    "field": "*",
    "alias": "cnt"
  },
  {
    "func": "SUM",
    "field": "m.COL_AMOUNT",
    "alias": "total_amount"
  },
  {
    "func": "AVG",
    "field": "m.COL_SCORE",
    "alias": "avg_score"
  }
]
```

Supported aggregate functions:

* `COUNT`
* `SUM`
* `AVG`
* `MIN`
* `MAX`

---

## 8. HAVING (post-aggregation filter)

```json
"having": [
  {
    "field": "total_amount",
    "op": ">",
    "param": "minTotal"
  }
]
```

Applied **after** aggregation.

---

## 9. Sorting (ORDER BY)

```json
"sort": [
  {
    "field": "m.COL_NAME",
    "direction": "ASC"
  },
  {
    "field": "total_amount",
    "direction": "DESC"
  }
]
```

Notes:

* `direction` is **structural**, not parameterized
* Fields must exist in projection or group output

---

## 10. Pagination

```json
"limit": 100,
"offset": 0
```

Maps to:

* `LogicalSort` with `fetch` and `offset`

---

## 11. Full example (everything together)

```json
{
  "source": { "table": "T_MAIN", "alias": "m" },
  "joins": [
    {
      "type": "LEFT",
      "table": "T_REF",
      "alias": "r",
      "on": [
        { "left": "m.COL_REF_ID", "op": "=", "right": "r.COL_ID" }
      ]
    }
  ],
  "filters": {
    "op": "AND",
    "conditions": [
      { "field": "m.COL_STATUS", "op": "=", "param": "status" },
      { "field": "m.COL_DATE", "op": ">=", "param": "fromDate" }
    ]
  },
  "groupBy": ["m.COL_CATEGORY"],
  "aggregates": [
    { "func": "COUNT", "field": "*", "alias": "cnt" },
    { "func": "SUM", "field": "m.COL_AMOUNT", "alias": "total" }
  ],
  "having": [
    { "field": "total", "op": ">", "param": "minTotal" }
  ],
  "projection": [
    { "field": "m.COL_CATEGORY", "alias": "category" },
    { "field": "cnt" },
    { "field": "total" }
  ],
  "sort": [
    { "field": "total", "direction": "DESC" }
  ],
  "limit": 50,
  "offset": 0
}
```

---

## 12. How this maps to Calcite

| Template concept | Calcite                    |
|------------------|----------------------------|
| source           | `scan()`                   |
| join             | `join()`                   |
| filter           | `filter()`                 |
| groupBy          | `aggregate()`              |
| having           | `filter()` after aggregate |
| projection       | `project()`                |
| sort             | `sort()`                   |
| limit / offset   | `sort(fetch, offset)`      |

---

## Final reality check

This template is:

* schema-agnostic
* backend-agnostic
* safe for MCP / LLM usage
* aligned with relational algebra

This is **exactly** the abstraction layer used by serious query engines.

If you want next:

* mapping this spec → RelBuilder code
* a validation checklist per section
* constraints to keep LLMs from generating nonsense

Say which one.

Perfect. Let’s do this step by step. I’ll provide **complex query examples** using **generic templates** and show how to
turn them into **Calcite `SqlNode`s**. We’ll cover:

* Filters
* Joins
* Aggregates
* Sorts
* Pagination
* Nested AND/OR

---

## 1️⃣ Simple filter + sort

### Template

```json
{
  "source": "T_MAIN",
  "filters": [
    { "field": "COL_ID", "op": "=", "param": "id" }
  ],
  "sort": [
    { "field": "COL_NAME", "direction": "ASC" }
  ],
  "projection": ["COL_ID", "COL_NAME"]
}
```

### Java code: SqlNode with Calcite parser

```java
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.*;
import org.apache.calcite.sql.util.SqlBuilder;

String sqlTemplate = "SELECT COL_ID, COL_NAME FROM T_MAIN WHERE COL_ID = ? ORDER BY COL_NAME ASC";
SqlParser parser = SqlParser.create(sqlTemplate);
SqlNode sqlNode = parser.parseQuery();
```

> Here `?` can be bound later as a parameter using `RelBuilder` or JDBC.

---

## 2️⃣ Join + filters

### Template

```json
{
  "source": "T_MAIN",
  "joins": [
    {
      "type": "INNER",
      "table": "T_REF",
      "alias": "r",
      "on": [
        { "left": "T_MAIN.COL_REF", "op": "=", "right": "r.COL_ID" }
      ]
    }
  ],
  "filters": [
    { "field": "T_MAIN.COL_STATUS", "op": "=", "param": "status" }
  ],
  "projection": ["T_MAIN.COL_ID", "T_MAIN.COL_NAME", "r.COL_DESC"]
}
```

### RelBuilder approach

```java
RelBuilder builder = RelBuilder.create(config);

builder.scan("T_MAIN")
       .scan("T_REF")
       .join(JoinRelType.INNER,
             builder.equals(
               builder.field(2, 0, "COL_REF"),
               builder.field(2, 1, "COL_ID")
             ))
       .filter(
         builder.equals(builder.field("COL_STATUS"),
                        builder.literal(statusParam))
       )
       .project(builder.field("COL_ID"),
                builder.field("COL_NAME"),
                builder.field("COL_DESC"));

RelNode relNode = builder.build();
```

> `2,0` and `2,1` are zero-based positions in the stack; adjust if necessary.

---

## 3️⃣ Aggregates + Group By + Having

### Template

```json
{
  "source": "T_SALES",
  "groupBy": ["COL_REGION"],
  "aggregates": [
    { "func": "SUM", "field": "COL_AMOUNT", "alias": "TOTAL_AMOUNT" },
    { "func": "COUNT", "field": "*", "alias": "NUM_ORDERS" }
  ],
  "having": [
    { "field": "TOTAL_AMOUNT", "op": ">", "param": "minTotal" }
  ],
  "projection": ["COL_REGION", "TOTAL_AMOUNT", "NUM_ORDERS"],
  "sort": [
    { "field": "TOTAL_AMOUNT", "direction": "DESC" }
  ]
}
```

### RelBuilder code

```java
RelBuilder b = RelBuilder.create(config);

b.scan("T_SALES")
 .aggregate(
    b.groupKey(b.field("COL_REGION")),
    b.sum(false, "TOTAL_AMOUNT", b.field("COL_AMOUNT")),
    b.count(false, "NUM_ORDERS", b.literal(1))
 )
 .filter(
    b.call(SqlStdOperatorTable.GREATER_THAN,
           b.field("TOTAL_AMOUNT"),
           b.literal(minTotal))
 )
 .sort(b.desc(b.field("TOTAL_AMOUNT")))
 .project(b.field("COL_REGION"),
          b.field("TOTAL_AMOUNT"),
          b.field("NUM_ORDERS"));

RelNode rel = b.build();
```

---

## 4️⃣ Nested AND/OR filters + Join + Pagination

### Template

```json
{
  "source": "T_CUSTOMER",
  "joins": [
    { "type": "LEFT", "table": "T_ORDERS", "alias": "o",
      "on": [ { "left": "T_CUSTOMER.ID", "op": "=", "right": "o.CUST_ID" } ]
    }
  ],
  "filters": {
    "op": "AND",
    "conditions": [
      { "field": "T_CUSTOMER.ACTIVE", "op": "=", "param": "active" },
      {
        "op": "OR",
        "conditions": [
          { "field": "o.AMOUNT", "op": ">", "param": "minOrder" },
          { "field": "o.STATUS", "op": "=", "param": "orderStatus" }
        ]
      }
    ]
  },
  "projection": ["T_CUSTOMER.ID", "T_CUSTOMER.NAME", "o.AMOUNT", "o.STATUS"],
  "sort": [
    { "field": "T_CUSTOMER.NAME", "direction": "ASC" }
  ],
  "limit": 50,
  "offset": 0
}
```

### RelBuilder code (conceptual)

```java
RelBuilder b = RelBuilder.create(config);

b.scan("T_CUSTOMER")
 .scan("T_ORDERS")
 .join(JoinRelType.LEFT,
       b.equals(b.field(2,0,"ID"), b.field(2,1,"CUST_ID")))
 .filter(
    b.and(
      b.equals(b.field("ACTIVE"), b.literal(activeParam)),
      b.or(
        b.call(SqlStdOperatorTable.GREATER_THAN, b.field("AMOUNT"), b.literal(minOrderParam)),
        b.equals(b.field("STATUS"), b.literal(orderStatusParam))
      )
    )
 )
 .project(b.field("ID"), b.field("NAME"), b.field("AMOUNT"), b.field("STATUS"))
 .sort(b.field("NAME"))
 .limit(50)
 .offset(0);

RelNode rel = b.build();
```

---

## ✅ Key takeaways

1. **RelBuilder builds queries programmatically**, not with SQL strings.
2. Use **parameters** instead of hardcoded values.
3. **Joins, aggregates, nested filters, sort, pagination** are all supported.
4. Templates are **generic**; only runtime parameters and schema bindings change.
5. RelNode can then be passed to **Calcite planner** for optimization and execution.

---

# **Comprehensive reference library of generic query templates** with corresponding `RelBuilder` code.

1. Joins (INNER, LEFT, RIGHT, FULL)
2. Filters (nested AND/OR, IN, LIKE, BETWEEN, IS NULL)
3. Aggregates (COUNT, SUM, AVG, MIN, MAX) + GROUP BY + HAVING
4. Sort + Pagination
5. Window functions (ROW_NUMBER, RANK)
6. CASE WHEN expressions

Uses **generic table/column names** so it’s reusable across any schema or domain.

---

## **Template 1 — INNER JOIN + simple filter + sort**

**Template:**

```json
{
  "source": "T_MAIN",
  "joins": [
    { "type": "INNER", "table": "T_REF", "alias": "r",
      "on": [ { "left": "T_MAIN.COL_REF", "op": "=", "right": "r.COL_ID" } ] }
  ],
  "filters": [
    { "field": "T_MAIN.COL_STATUS", "op": "=", "param": "status" }
  ],
  "projection": ["T_MAIN.COL_ID", "T_MAIN.COL_NAME", "r.COL_DESC"],
  "sort": [ { "field": "T_MAIN.COL_NAME", "direction": "ASC" } ]
}
```

**RelBuilder code:**

```java
RelBuilder b = RelBuilder.create(config);

b.scan("T_MAIN")
 .scan("T_REF")
 .join(JoinRelType.INNER,
       b.equals(b.field(2,0,"COL_REF"), b.field(2,1,"COL_ID")))
 .filter(b.equals(b.field("COL_STATUS"), b.literal(statusParam)))
 .project(b.field("COL_ID"), b.field("COL_NAME"), b.field("COL_DESC"))
 .sort(b.field("COL_NAME"));

RelNode rel = b.build();
```

---

## **Template 2 — LEFT JOIN + nested AND/OR filters**

**Template:**

```json
{
  "source": "T_CUSTOMER",
  "joins": [
    { "type": "LEFT", "table": "T_ORDER", "alias": "o",
      "on": [ { "left": "T_CUSTOMER.ID", "op": "=", "right": "o.CUST_ID" } ] }
  ],
  "filters": {
    "op": "AND",
    "conditions": [
      { "field": "T_CUSTOMER.ACTIVE", "op": "=", "param": "active" },
      {
        "op": "OR",
        "conditions": [
          { "field": "o.AMOUNT", "op": ">", "param": "minOrder" },
          { "field": "o.STATUS", "op": "=", "param": "orderStatus" }
        ]
      }
    ]
  },
  "projection": ["T_CUSTOMER.ID", "T_CUSTOMER.NAME", "o.AMOUNT", "o.STATUS"],
  "sort": [ { "field": "T_CUSTOMER.NAME", "direction": "ASC" } ],
  "limit": 50,
  "offset": 0
}
```

**RelBuilder code:**

```java
b.scan("T_CUSTOMER")
 .scan("T_ORDER")
 .join(JoinRelType.LEFT,
       b.equals(b.field(2,0,"ID"), b.field(2,1,"CUST_ID")))
 .filter(
    b.and(
      b.equals(b.field("ACTIVE"), b.literal(activeParam)),
      b.or(
        b.call(SqlStdOperatorTable.GREATER_THAN, b.field("AMOUNT"), b.literal(minOrderParam)),
        b.equals(b.field("STATUS"), b.literal(orderStatusParam))
      )
    )
 )
 .project(b.field("ID"), b.field("NAME"), b.field("AMOUNT"), b.field("STATUS"))
 .sort(b.field("NAME"))
 .limit(50)
 .offset(0);

RelNode rel = b.build();
```

---

## **Template 3 — Aggregates + GROUP BY + HAVING + sort**

**Template:**

```json
{
  "source": "T_SALES",
  "groupBy": ["COL_REGION"],
  "aggregates": [
    { "func": "SUM", "field": "COL_AMOUNT", "alias": "TOTAL_AMOUNT" },
    { "func": "COUNT", "field": "*", "alias": "NUM_ORDERS" }
  ],
  "having": [ { "field": "TOTAL_AMOUNT", "op": ">", "param": "minTotal" } ],
  "projection": ["COL_REGION", "TOTAL_AMOUNT", "NUM_ORDERS"],
  "sort": [ { "field": "TOTAL_AMOUNT", "direction": "DESC" } ]
}
```

**RelBuilder code:**

```java
b.scan("T_SALES")
 .aggregate(
   b.groupKey(b.field("COL_REGION")),
   b.sum(false, "TOTAL_AMOUNT", b.field("COL_AMOUNT")),
   b.count(false, "NUM_ORDERS", b.literal(1))
 )
 .filter(b.call(SqlStdOperatorTable.GREATER_THAN,
                b.field("TOTAL_AMOUNT"),
                b.literal(minTotalParam)))
 .sort(b.desc(b.field("TOTAL_AMOUNT")))
 .project(b.field("COL_REGION"), b.field("TOTAL_AMOUNT"), b.field("NUM_ORDERS"));

RelNode rel = b.build();
```

---

## **Template 4 — Window functions (ROW_NUMBER)**

**Template:**

```json
{
  "source": "T_EMPLOYEE",
  "windowFunctions": [
    { "func": "ROW_NUMBER", "partitionBy": ["DEPT_ID"], "orderBy": [{ "field": "SALARY", "direction": "DESC" }], "alias": "RN" }
  ],
  "filters": [ { "field": "RN", "op": "<=", "param": "topN" } ],
  "projection": ["ID", "NAME", "DEPT_ID", "SALARY", "RN"]
}
```

**RelBuilder / SqlNode code (simplified):**

```java
SqlParser parser = SqlParser.create(
  "SELECT *, ROW_NUMBER() OVER (PARTITION BY DEPT_ID ORDER BY SALARY DESC) AS RN " +
  "FROM T_EMPLOYEE"
);
SqlNode sqlNode = parser.parseQuery();

// Filtering top N per department
// Could also be done via RelBuilder using window aggregate
```

> Note: Window functions often require `SqlNode` + `RelBuilder` combined, because RelBuilder’s native support is limited
> to simple aggregates and sorts.

---

## **Template 5 — CASE WHEN + conditional projection**

**Template:**

```json
{
  "source": "T_CUSTOMER",
  "projection": [
    "ID",
    "NAME",
    {
      "expr": {
        "op": "CASE",
        "conditions": [
          { "when": { "field": "STATUS", "op": "=", "param": "active" }, "then": "'Active'" },
          { "when": { "field": "STATUS", "op": "!=", "param": "active" }, "then": "'Inactive'" }
        ],
        "else": "'Unknown'"
      },
      "alias": "STATUS_LABEL"
    }
  ]
}
```

**RelBuilder code (conceptual):**

```java
b.scan("T_CUSTOMER")
 .project(
    b.field("ID"),
    b.field("NAME"),
    b.call(SqlStdOperatorTable.CASE,
           b.call(SqlStdOperatorTable.EQUALS, b.field("STATUS"), b.literal(activeParam)),
           b.literal("Active"),
           b.literal("Inactive")
    ).alias("STATUS_LABEL")
 );

RelNode rel = b.build();
```

---

## **Template 6 — IN, BETWEEN, LIKE filters**

```json
"filters": [
  { "field": "COL_TYPE", "op": "IN", "param": "typeList" },
  { "field": "COL_DATE", "op": "BETWEEN", "param": ["startDate", "endDate"] },
  { "field": "COL_NAME", "op": "LIKE", "param": "namePattern" }
]
```

**RelBuilder code:**

```java
b.filter(
  b.call(SqlStdOperatorTable.IN, b.field("COL_TYPE"), b.literalList(typeList)),
  b.call(SqlStdOperatorTable.BETWEEN,
         b.field("COL_DATE"),
         b.literal(startDate),
         b.literal(endDate)),
  b.call(SqlStdOperatorTable.LIKE, b.field("COL_NAME"), b.literal(namePattern))
);
```

## Template 6 — complex query

```
template: |
  {
    "with": [
      {
        "name": "temp_sales",
        "query": {
          "source": { "table": "T_SALES", "alias": "s" },
          "filters": [
            { "field": "s.SALE_DATE", "op": ">=", "param": "fromDate" },
            { "field": "s.SALE_DATE", "op": "<=", "param": "toDate" }
          ],
          "projection": ["s.ID", "s.CUST_ID", "s.PRODUCT_ID", "s.AMOUNT"]
        }
      }
    ],
    "source": { "table": "T_MAIN", "alias": "m" },
    "joins": [
      {
        "type": "LEFT",
        "table": "T_REF",
        "alias": "r",
        "on": [
          { "left": "m.COL_REF_ID", "op": "=", "right": "r.COL_ID" }
        ]
      },
      {
        "type": "INNER",
        "table": "temp_sales",
        "alias": "ts",
        "on": [
          { "left": "m.CUST_ID", "op": "=", "right": "ts.CUST_ID" }
        ]
      },
      {
        "type": "LEFT",
        "table": "T_PRODUCT",
        "alias": "p",
        "on": [
          { "left": "ts.PRODUCT_ID", "op": "=", "right": "p.ID" }
        ]
      }
    ],
    "filters": {
      "op": "AND",
      "conditions": [
        { "field": "m.COL_STATUS", "op": "=", "param": "status" },
        {
          "op": "OR",
          "conditions": [
            { "field": "m.COL_TYPE", "op": "IN", "param": "types" },
            { "field": "p.CATEGORY", "op": "LIKE", "param": "categoryPattern" }
          ]
        }
      ]
    },
    "groupBy": ["m.COL_CATEGORY", "p.CATEGORY"],
    "aggregates": [
      { "func": "COUNT", "field": "*", "alias": "cnt" },
      { "func": "SUM", "field": "ts.AMOUNT", "alias": "total_sales" },
      { "func": "AVG", "field": "ts.AMOUNT", "alias": "avg_sales" }
    ],
    "having": [
      { "field": "total_sales", "op": ">", "param": "minTotal" },
      { "field": "avg_sales", "op": ">=", "param": "minAvg" }
    ],
    "projection": [
      { "field": "m.COL_CATEGORY", "alias": "main_category" },
      { "field": "p.CATEGORY", "alias": "product_category" },
      { "field": "cnt" },
      { "field": "total_sales" },
      { "field": "avg_sales" }
    ],
    "sort": [
      { "field": "total_sales", "direction": "DESC" },
      { "field": "avg_sales", "direction": "DESC" }
    ],
    "limit": 100,
    "offset": 0
  }

```

Perfect. Here’s a **Scala/Java-style example** showing how to take the JSON-based domain-neutral query template (with
CTE, joins, aggregates, and filters) and build a **Calcite `RelNode`** using `RelBuilder`.

This example assumes:

* You’ve deserialized the JSON into a `QueryTemplate` object.
* Runtime parameters are provided separately and will be bound when building filters.

---

## **1️⃣ Example Classes for Template**

```java
class QueryTemplate {
    List<CTE> withTables;
    Source source;
    List<Join> joins;
    Filter filters;
    List<String> groupBy;
    List<Aggregate> aggregates;
    List<Filter> having;
    List<Projection> projection;
    List<Sort> sort;
    Integer limit;
    Integer offset;
}

class CTE {
    String name;
    QueryTemplate query;
}

class Source {
    String table;
    String alias;
}

class Join {
    String type; // INNER, LEFT, RIGHT, FULL
    String table;
    String alias;
    List<JoinCondition> on;
}

class JoinCondition {
    String left;
    String op;
    String right;
}

class Filter {
    String op; // AND, OR or null
    List<Filter> conditions; // for nested
    String field;
    String param;
    String opSimple; // =, >, <, IN, LIKE etc.
}

class Aggregate {
    String func; // COUNT, SUM, AVG
    String field;
    String alias;
}

class Projection {
    String field;
    String alias;
}

class Sort {
    String field;
    String direction; // ASC, DESC
}
```

---

## **2️⃣ Building the RelNode with RelBuilder**

```java
RelBuilder builder = RelBuilder.create(config);

// Optional: build CTEs first
if (template.withTables != null) {
    for (CTE cte : template.withTables) {
        RelNode cteNode = buildRelNode(builder, cte.query, params);
        // Save cteNode in a map to reference in joins
    }
}

// Build main query
RelNode mainNode = buildRelNode(builder, template, params);

// ----------------------
// Recursive helper
// ----------------------
RelNode buildRelNode(RelBuilder b, QueryTemplate t, Map<String, Object> params) {
    // Start from source
    b.scan(t.source.table);

    // Joins
    if (t.joins != null) {
        for (Join j : t.joins) {
            b.scan(j.table); // could be a CTE table
            List<RexNode> onNodes = new ArrayList<>();
            for (JoinCondition jc : j.on) {
                onNodes.add(
                    b.equals(
                        b.field(jc.left),
                        b.field(jc.right)
                    )
                );
            }
            RexNode onClause = b.and(onNodes);
            JoinRelType joinType = JoinRelType.valueOf(j.type.toUpperCase());
            b.join(joinType, onClause);
        }
    }

    // Filters
    if (t.filters != null) {
        b.filter(buildFilter(b, t.filters, params));
    }

    // Aggregates
    if (t.groupBy != null && !t.groupBy.isEmpty()) {
        List<RexNode> aggCalls = new ArrayList<>();
        for (Aggregate a : t.aggregates) {
            switch(a.func.toUpperCase()) {
                case "COUNT": b.count(false, a.alias, b.literal(1)); break;
                case "SUM": b.sum(false, a.alias, b.field(a.field)); break;
                case "AVG": b.avg(false, a.alias, b.field(a.field)); break;
            }
        }
        b.aggregate(b.groupKey(t.groupBy.toArray(new String[0])), aggCalls.toArray(new RexNode[0]));
    }

    // HAVING
    if (t.having != null) {
        for (Filter f : t.having) {
            b.filter(buildFilter(b, f, params));
        }
    }

    // Projection
    if (t.projection != null) {
        List<RexNode> proj = new ArrayList<>();
        for (Projection p : t.projection) {
            proj.add(b.field(p.field));
        }
        b.project(proj);
    }

    // Sort
    if (t.sort != null) {
        List<RexNode> sortNodes = new ArrayList<>();
        for (Sort s : t.sort) {
            sortNodes.add(s.direction.equalsIgnoreCase("ASC") ? b.asc(b.field(s.field)) : b.desc(b.field(s.field)));
        }
        b.sort(sortNodes);
    }

    // Limit / Offset
    if (t.limit != null || t.offset != null) {
        b.limit(t.offset == null ? 0 : t.offset, t.limit == null ? Integer.MAX_VALUE : t.limit);
    }

    return b.build();
}

// ----------------------
// Helper to recursively build filters
// ----------------------
RexNode buildFilter(RelBuilder b, Filter f, Map<String,Object> params) {
    if (f.op != null && f.conditions != null) {
        List<RexNode> nodes = f.conditions.stream().map(c -> buildFilter(b, c, params)).collect(Collectors.toList());
        return f.op.equalsIgnoreCase("AND") ? b.and(nodes) : b.or(nodes);
    } else {
        Object val = params.get(f.param);
        switch(f.opSimple.toUpperCase()) {
            case "=": return b.equals(b.field(f.field), b.literal(val));
            case ">": return b.call(SqlStdOperatorTable.GREATER_THAN, b.field(f.field), b.literal(val));
            case "<": return b.call(SqlStdOperatorTable.LESS_THAN, b.field(f.field), b.literal(val));
            case "LIKE": return b.call(SqlStdOperatorTable.LIKE, b.field(f.field), b.literal(val));
            case "IN": return b.call(SqlStdOperatorTable.IN, b.field(f.field), b.literalList((List<?>) val));
            default: throw new RuntimeException("Unsupported operator " + f.opSimple);
        }
    }
}
```

---

### ✅ Notes

1. Supports **multiple joins**, including joins to **CTEs / temp tables**.
2. Supports **nested filters** and parameter binding.
3. Supports **aggregates, group by, having**.
4. Projection, sort, limit/offset handled.
5. Recursive structure allows **CTEs to be referenced in joins**.

---

---

✅ **Key takeaways**

1. Use **generic table/column names** in templates.
2. Parameters are **bound at runtime**, never hardcoded.
3. RelBuilder maps templates into **safe, type-checked relational algebra trees**.
4. Supports joins, nested filters, aggregates, group/having, sorts, pagination, window functions, and CASE WHEN.
5. This is a full reference for **building a generic query engine** on top of Calcite.

---

**Comprehensive validation checklist** for building a generic, schema-agnostic query engine with Calcite. This ensures
that the templates and parameters are safe, valid, and optimizer-friendly before building a `RelNode`. It's broken down
**section by section**.

---

## **1️⃣ Source (FROM)**

**Checklist:**

* [ ] Table exists in the current schema/catalog.
* [ ] Table alias (if provided) is unique within the query.
* [ ] Table name is valid SQL identifier.
* [ ] Table is accessible to the current user/context.

---

## **2️⃣ Joins**

**Checklist:**

* [ ] Join type is supported: `INNER`, `LEFT`, `RIGHT`, `FULL`.
* [ ] Join table exists and is accessible.
* [ ] Join alias is unique and valid.
* [ ] Join ON conditions are non-empty.
* [ ] Columns in ON conditions exist in the referenced tables.
* [ ] Operators in ON conditions are valid: `=`, `!=`, `<`, `<=`, `>`, `>=`.
* [ ] Data types of join columns are compatible.

---

## **3️⃣ Filters (WHERE)**

**Checklist:**

* [ ] Each filter field exists in the source table(s) or aliases.
* [ ] Operator is valid for the field type (`=`, `!=`, `<`, `>`, `LIKE`, `IN`, `BETWEEN`, `IS_NULL`, etc.).
* [ ] Parameter exists in the runtime parameter map.
* [ ] Parameter type is compatible with the field type.
* [ ] Nested AND/OR groups are well-formed and balanced.
* [ ] No unsupported complex expressions (e.g., functions not defined in the catalog).

---

## **4️⃣ Projection (SELECT)**

**Checklist:**

* [ ] All projected columns exist in the source table(s) or are computed expressions.
* [ ] Aliases are unique.
* [ ] Expressions use only allowed operators/functions.
* [ ] Aggregate fields are either in GROUP BY or aggregated (if aggregation is used).
* [ ] CASE WHEN / COALESCE / functions are supported and type-checked.

---

## **5️⃣ Aggregates (GROUP BY)**

**Checklist:**

* [ ] All fields in `groupBy` exist in source tables or aliases.
* [ ] Aggregate functions are supported: `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`.
* [ ] Aggregate functions operate on compatible types.
* [ ] Projection contains either groupBy fields or aggregates.
* [ ] HAVING fields exist in aggregates or groupBy.

---

## **6️⃣ HAVING**

**Checklist:**

* [ ] Fields referenced exist in `groupBy` or aggregates.
* [ ] Operators are valid for the field types.
* [ ] Parameters exist and match expected types.
* [ ] Logical expressions (AND/OR) are well-formed.

---

## **7️⃣ Sort (ORDER BY)**

**Checklist:**

* [ ] Sort fields exist in projection or output of aggregation.
* [ ] Direction is valid: `ASC` or `DESC`.
* [ ] No unsupported expressions used in ORDER BY.

---

## **8️⃣ Limit / Offset (Pagination)**

**Checklist:**

* [ ] Limit and offset are non-negative integers.
* [ ] Limit is not excessively large (optional for performance guard).
* [ ] Offset does not exceed total rows (optional).

---

## **9️⃣ Window Functions**

**Checklist:**

* [ ] PartitionBy fields exist in the source or join tables.
* [ ] OrderBy fields exist in the source or computed expressions.
* [ ] Supported functions: `ROW_NUMBER`, `RANK`, `DENSE_RANK`, `SUM`, `AVG`, etc.
* [ ] Aliases for window functions are unique.
* [ ] Combined with filters, projections, and aggregation correctly.

---

## **10️⃣ Expressions / Computed Fields**

**Checklist:**

* [ ] Functions/operators exist in Calcite catalog.
* [ ] Type compatibility is enforced (e.g., `string + int` invalid).
* [ ] CASE WHEN conditions are exhaustive or have ELSE clause.
* [ ] Nested expressions are well-formed.

---

## **11️⃣ Parameters**

**Checklist:**

* [ ] All parameters referenced in filters, having, or computed expressions exist.
* [ ] Parameter types match field types.
* [ ] For lists (IN, BETWEEN), ensure correct length and types.
* [ ] No unsafe inputs (e.g., SQL injection risk if converting to SQL strings).

---

## **12️⃣ General**

**Checklist:**

* [ ] Aliases across tables, projections, and joins are unique.
* [ ] No reserved SQL keywords used as identifiers unless quoted.
* [ ] Query plan is not empty after all filters applied.
* [ ] Templates are reusable and contain **parameters**, not hard-coded values.
* [ ] Error handling exists for missing fields, tables, or invalid operators.

---

### ✅ Notes

* This checklist can be applied **programmatically** when parsing templates before building the `RelNode`.
* It ensures **schema-agnostic safety**, **parameter validation**, and **RelBuilder correctness**.
* It also acts as **documentation for users or API clients** to know what is allowed in templates.

---

# When exposing a **generic query engine** to an LLM, we **must constrain inputs** to prevent:

* Invalid columns, tables, or joins
* Unsupported operators/functions
* SQL injection attempts
* Nonsensical or contradictory queries

Draft of a tructured approach with **constraints and guardrails**.

---

## **1️⃣ Schema Constraints**

* **Allowed tables:** Maintain a catalog of tables (`T_MAIN`, `T_CUSTOMER`, etc.). LLM can only reference these.
* **Allowed columns per table:** Maintain a mapping `table -> [columns]`. Reject references outside this.
* **Column types:** Ensure types are compatible with operators and aggregates.
* **Aliases:** Force unique and predictable aliases; forbid arbitrary names.

**Example validation:**

```json
{
  "table": "T_CUSTOMER",
  "allowedColumns": ["ID", "NAME", "STATUS", "DEPT_ID"]
}
```

Reject: `T_CUSTOMER.PASSWORD` or `HACKED_TABLE.COL_X`.

---

## **2️⃣ Operator Constraints**

* **Whitelist operators:**

    * Comparison: `=`, `!=`, `<`, `<=`, `>`, `>=`
    * Logical: `AND`, `OR`, `NOT`
    * Aggregate: `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`
    * Window: `ROW_NUMBER`, `RANK`, `DENSE_RANK`
    * Conditional: `CASE`, `COALESCE`
* **Reject unsupported operators/functions** that LLM might invent.
* **Ensure type compatibility:**

    * `SUM`/`AVG` only on numeric fields
    * `LIKE` only on strings
    * `IN` lists must match the column type

---

## **3️⃣ Join Constraints**

* Only allow joins between columns of **compatible types**.
* Only use tables in the **allowed schema**.
* Limit join depth to prevent complex nonsense (`JOIN` chains > 5 could be rejected).
* Only supported join types: `INNER`, `LEFT`, `RIGHT`, `FULL`.

---

## **4️⃣ Filter / Predicate Constraints**

* **Parameterized filters only**: LLM cannot inject literal SQL values directly.
* **Nested filters:** Limit recursion depth (e.g., max 3 levels).
* **Reject contradictory filters** if detectable (e.g., `COL_ID = 1 AND COL_ID = 2`).
* **Guard against always-true / always-false nonsense**.

---

## **5️⃣ Projection / Aggregate Constraints**

* Projection must be a subset of:

    * Columns from source/joins
    * Aliases of computed fields or aggregates
* Grouped fields must appear in `GROUP BY`.
* HAVING references must only use aggregates or group fields.
* Reject invented aggregates or functions LLM generates.

---

## **6️⃣ Sort / Pagination Constraints**

* Sort fields must exist in projection or aggregated output.
* Sort direction limited to `ASC` or `DESC`.
* Limit and offset must be integers, within acceptable ranges.

---

## **7️⃣ Window Functions Constraints**

* Only allow partition/order by fields that exist in source/joins.
* Only allow whitelisted window functions (`ROW_NUMBER`, `RANK`, etc.).
* Enforce a max number of simultaneous window functions to prevent combinatorial explosion.

---

## **8️⃣ Parameter / Value Constraints**

* All dynamic values must be provided via **typed parameters**, never injected directly by LLM.
* Enforce type checks at runtime: numeric, string, date, boolean.
* Reject large or unsafe collections (e.g., `IN` lists > 1000 elements).

---

## **9️⃣ General LLM Guardrails**

* **Reject unknown SQL tokens**: LLM cannot invent arbitrary syntax.
* **Reject reserved keywords** in table/column/alias names.
* **Maximum query complexity:**

    * Max joins
    * Max nested filters
    * Max aggregates/window functions
* **No cross-schema joins** unless explicitly allowed.
* **Reject arbitrary SQL expressions** (no `DROP`, `TRUNCATE`, subqueries outside template).

---

## **10️⃣ Enforcement Mechanism**

* Maintain **whitelists**: tables, columns, operators, functions.
* Maintain **schema metadata**: type info, nullable info.
* Run a **validation pass** on any LLM-generated template before building RelNode.
* Only after validation pass, bind **parameters**, build RelNode, and execute.

> This ensures LLMs can only create queries that are **safe, valid, and meaningful** within the schema.

---

# JOOQ

> **Conversion of the JSON/YAML template into a SQL string using jOOQ** <br> parse a domain-neutral template and build
> SQL via jOOQ:

---

### **1️⃣ Sample YAML / JSON template** (same as before)

```yaml
template: |
  {
    "source": { "table": "T_MAIN", "alias": "m" },
    "joins": [
      {
        "type": "LEFT",
        "table": "T_REF",
        "alias": "r",
        "on": [
          { "left": "m.COL_REF_ID", "op": "=", "right": "r.COL_ID" }
        ]
      }
    ],
    "filters": {
      "op": "AND",
      "conditions": [
        { "field": "m.COL_STATUS", "op": "=", "param": "status" },
        { "field": "m.COL_DATE", "op": ">=", "param": "fromDate" }
      ]
    },
    "projection": ["m.COL_CATEGORY", "m.COL_AMOUNT"],
    "sort": [{ "field": "m.COL_AMOUNT", "direction": "DESC" }],
    "limit": 50
  }
```

---

### **2️⃣ Java code using jOOQ to build SQL dynamically**

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.io.File;
import java.util.Map;

import static org.jooq.impl.DSL.*;

public class JooqFromTemplateExample {

    public static void main(String[] args) throws Exception {

        // 1. Load YAML as string -> Map
        ObjectMapper yamlMapper = new ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
        Map<String,Object> templateMap = yamlMapper.readValue(new File("query.yaml"), Map.class);

        Map<String,Object> queryJson = (Map<String,Object>) templateMap.get("template");

        DSLContext ctx = DSL.using(SQLDialect.DEFAULT);

        // 2. Build jOOQ tables and fields
        String mainTable = ((Map<String,Object>)queryJson.get("source")).get("table").toString();
        String mainAlias = ((Map<String,Object>)queryJson.get("source")).get("alias").toString();

        org.jooq.Table<?> tMain = table(name(mainTable)).as(mainAlias);

        // Projection
        java.util.List<String> projections = (java.util.List<String>) queryJson.get("projection");

        // Joins
        java.util.List<Map<String,Object>> joins = (java.util.List<Map<String,Object>>) queryJson.get("joins");

        org.jooq.SelectJoinStep<?> jooqQuery = ctx.select(
                projections.stream().map(f -> tMain.field(f.split("\\.")[1])).toArray(org.jooq.Field[]::new)
        ).from(tMain);

        // Apply joins
        for (Map<String,Object> join : joins) {
            String joinType = join.get("type").toString();
            Map<String,Object> joinTable = (Map<String,Object>) join.get("table");
            String joinTableName = join.get("table").toString();
            String joinAlias = join.get("alias").toString();

            org.jooq.Table<?> tJoin = table(name(joinTableName)).as(joinAlias);
            java.util.List<Map<String,String>> onList = (java.util.List<Map<String,String>>) join.get("on");

            org.jooq.Condition onCondition = null;
            for (Map<String,String> on : onList) {
                String left = on.get("left");
                String right = on.get("right");

                org.jooq.Condition c = field(name(left)).eq(field(name(right)));
                onCondition = (onCondition == null) ? c : onCondition.and(c);
            }

            switch (joinType.toUpperCase()) {
                case "LEFT": jooqQuery = jooqQuery.leftJoin(tJoin).on(onCondition); break;
                case "INNER": jooqQuery = jooqQuery.join(tJoin).on(onCondition); break;
                case "RIGHT": jooqQuery = jooqQuery.rightJoin(tJoin).on(onCondition); break;
            }
        }

        // Filters
        Map<String,Object> filters = (Map<String,Object>) queryJson.get("filters");
        java.util.List<Map<String,Object>> conditions = (java.util.List<Map<String,Object>>) filters.get("conditions");

        org.jooq.Condition whereCondition = null;
        for (Map<String,Object> cond : conditions) {
            String field = cond.get("field").toString();
            String op = cond.get("op").toString();
            String param = cond.get("param").toString();

            org.jooq.Condition c = null;
            switch(op) {
                case "=": c = field(name(field)).eq(param(":"+param, Object.class)); break;
                case ">=": c = field(name(field)).ge(param(":"+param, Object.class)); break;
            }

            whereCondition = (whereCondition == null) ? c : whereCondition.and(c);
        }

        jooqQuery = jooqQuery.where(whereCondition);

        // Sort
        java.util.List<Map<String,String>> sorts = (java.util.List<Map<String,String>>) queryJson.get("sort");
        for (Map<String,String> s : sorts) {
            String sortField = s.get("field");
            String dir = s.get("direction");
            jooqQuery = dir.equalsIgnoreCase("DESC") ? jooqQuery.orderBy(field(name(sortField)).desc())
                                                     : jooqQuery.orderBy(field(name(sortField)).asc());
        }

        // Limit
        if (queryJson.get("limit") != null) {
            jooqQuery = jooqQuery.limit(Integer.parseInt(queryJson.get("limit").toString()));
        }

        // -----------------------------
        // Generate SQL string
        // -----------------------------
        String sql = jooqQuery.getSQL(org.jooq.ParamType.NAMED);
        System.out.println("Generated SQL:\n" + sql);

        // This SQL can now be sent to Calcite for parsing
    }
}
```

---

### ✅ Key points

1. Takes **domain-neutral JSON template** (embedded in YAML) as input.
2. Builds SQL **programmatically using jOOQ**.
3. Supports **joins, filters, projections, sort, limit**.
4. Parameters (`:status`, `:fromDate`) are **named** and can be bound later.
5. Output SQL string is ready to **feed into Calcite** for parsing or optimization.

---

# **OData – Summary & Samples**

**OData (Open Data Protocol)** is a standardized REST-based protocol for querying and manipulating data. It exposes *
*data resources** (entities) via HTTP endpoints and uses **URL query options** to express filters, projections, sorting,
pagination, and aggregation.

It’s **protocol-agnostic on the backend**, so the server can implement the storage in SQL, NoSQL, or in-memory objects.

---

## **1️⃣ Core Concepts**

| Concept                                          | Description                                                                                      |
|--------------------------------------------------|--------------------------------------------------------------------------------------------------|
| **Entity**                                       | A data resource (e.g., Product, Order, Artist). Similar to a table in SQL.                       |
| **EntitySet**                                    | A collection of entities (e.g., `/Products`).                                                    |
| **NavigationProperty**                           | Defines relationships between entities (like JOINs).                                             |
| **Query Options**                                | `$filter`, `$select`, `$orderby`, `$top`, `$skip`, `$expand`, `$apply` for grouping/aggregation. |
| **CSDL (Conceptual Schema Definition Language)** | Metadata describing the entity model exposed via OData.                                          |

---

## **2️⃣ Common Query Options**

| Option     | Description                       | Example                                                                        |
|------------|-----------------------------------|--------------------------------------------------------------------------------|
| `$filter`  | Filter entities by condition      | `/Products?$filter=Price gt 100 and Category eq 'Books'`                       |
| `$select`  | Return only selected fields       | `/Products?$select=Name,Price`                                                 |
| `$orderby` | Sort results                      | `/Products?$orderby=Price desc`                                                |
| `$top`     | Limit number of results           | `/Products?$top=10`                                                            |
| `$skip`    | Skip first N results (pagination) | `/Products?$skip=50&$top=50`                                                   |
| `$expand`  | Include related entities (joins)  | `/Orders?$expand=Customer`                                                     |
| `$apply`   | Aggregations / grouping           | `/Products/$apply=groupby((Category),aggregate(Price with sum as TotalPrice))` |

---

## **3️⃣ Example Queries**

### **Basic filtering and selection**

```http
GET /Products?$filter=Price gt 100 and InStock eq true&$select=Name,Price
```

**Equivalent SQL:**

```sql
SELECT Name, Price FROM Products
WHERE Price > 100 AND InStock = true;
```

---

### **Sorting and pagination**

```http
GET /Products?$orderby=Price desc&$skip=50&$top=20
```

**Equivalent SQL:**

```sql
SELECT * FROM Products
ORDER BY Price DESC
LIMIT 20 OFFSET 50;
```

---

### **Join / Expand**

```http
GET /Orders?$expand=Customer,OrderItems
```

**Equivalent SQL:**

```sql
SELECT o.*, c.*, oi.*
FROM Orders o
LEFT JOIN Customers c ON o.CustomerID = c.ID
LEFT JOIN OrderItems oi ON o.ID = oi.OrderID;
```

---

### **Aggregation / GroupBy**

```http
GET /Products/$apply=groupby((Category),aggregate(Price with sum as TotalPrice))
```

**Equivalent SQL:**

```sql
SELECT Category, SUM(Price) AS TotalPrice
FROM Products
GROUP BY Category;
```

---

## **4️⃣ Mapping Domain-Neutral Templates to OData**

You can use a **JSON/YAML domain-neutral query template** for OData just like for SQL:

**Template Example**

```json
{
  "source": "Products",
  "filters": [
    { "field": "Price", "op": ">", "param": "minPrice" },
    { "field": "InStock", "op": "=", "param": "inStock" }
  ],
  "projection": ["Name","Price"],
  "sort": [{ "field": "Price", "direction": "desc" }],
  "limit": 20,
  "offset": 50
}
```

**Generated OData URL**

```
GET /Products?$filter=Price gt :minPrice and InStock eq :inStock
&$select=Name,Price
&$orderby=Price desc
&$skip=50&$top=20
```

---

## **5️⃣ Notes / Best Practices**

* All filters, sorting, and pagination are **expressed in the URL** — no SQL required.
* `$expand` is the preferred way to retrieve related entities (avoids manual join logic).
* `$apply` supports **grouping and aggregation**, making OData suitable for analytics-style queries.
* Can be used alongside other protocols in a unified framework (REST, GraphQL, MCP) to provide **consistent query
  abstraction**.

---
INSERT Template
HTTP Request: POST /artists?name=Pink Floyd
Generated SQL: INSERT INTO artists (name, country, created_at) VALUES (:name, 'UK', NOW()) RETURNING id, name

{
"operation": "INSERT",
"source": { "table": "artists" },
"columns": [
{ "field": "name", "param": "name" },
{ "field": "country", "value": "UK" },
{ "field": "created_at", "function": "NOW()" }
],
"returning": ["id", "name"]
}

HTTP Request: PUT /artists?id=123&name=The Beatles
Generated SQL: UPDATE artists SET name = :name, updated_at = NOW() WHERE id = :id

UPDATE Template
{
"operation": "UPDATE",
"source": { "table": "artists" },
"set": [
{ "field": "name", "param": "name" },
{ "field": "updated_at", "function": "NOW()" }
],
"filters": {
"conditions": [
{ "field": "id", "op": "=", "param": "id" }
]
}
}

DELETE Template
HTTP Request: DELETE /artists?id=123
Generated SQL: DELETE FROM artists WHERE id = :id
Safety feature: DELETE without WHERE throws an error unless you add "allowDeleteAll": true

{
"operation": "DELETE",
"source": { "table": "artists" },
"filters": {
"conditions": [
{ "field": "id", "op": "=", "param": "id" }
]
}
}

UPDATE Template
@PostMapping("/{db}/{resource}")
public Map<String, Object> create(@RequestParam Map<String, String> params) {
QueryResult result = SqlTemplateQueryBuilder.buildQuery(insertTemplate, params);
return jdbcTemplate.queryForMap(result.sql(), result.parameters());
}
