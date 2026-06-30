# Cheshire Federated SQL Reference

## Introduction

Cheshire federated SQL is **Calcite SQL**. It is the SQL language parsed, validated,
planned, and executed by the Calcite query engine. It is not PostgreSQL SQL, MySQL SQL,
SQLite SQL, SQL Server SQL, Oracle SQL, or Elasticsearch SQL.

Apache Calcite's SQL reference is the source of truth for grammar and core semantics:
https://calcite.apache.org/docs/reference.html

Use federated SQL when a capability needs a portable relational query over one or more
registered Cheshire sources. Use the [SQL Template DSL](SQL_TEMPLATE_DSL_REFERENCE.md)
when an action should generate SQL from a structured JSON template.

## Data Model

Cheshire maps configured sources into a Calcite relational model:

| Concept | User meaning |
|---------|--------------|
| Catalog | The root namespace owned by the Calcite engine. Most users do not name it directly. |
| Schema | A SQL-facing source name registered under the Calcite root schema, such as `crm` or `billing`. |
| Table | A relation exposed by a source, such as `customers`, `orders`, or `articles`. |
| Column | A field exposed by a table. |
| Source mapping | The configuration that connects a Cheshire source name to the physical system and schema. |

Prefer SQL-friendly source names, table names, and column names: letters, numbers, and
underscores. If a configured source name contains punctuation, rename it for SQL-facing
use where possible.

### JDBC Source Mapping

For JDBC-backed federation, the source name becomes the SQL schema used in queries. The
inner `schema` value is the physical database schema that Calcite should inspect through
the JDBC connection.

```yaml
query-engines:
  federated:
    name: federated
    description: Calcite federated query engine
    factory: io.cheshire.query.engine.calcite.CalciteQueryEngineFactory
    sources:
      crm:
        name: crm
        type: jdbc
        config:
          type: jdbc
          schema: public
          connection:
            url: jdbc:postgresql://localhost:5432/crm
            driver: org.postgresql.Driver
            username: crm_user
            password: ${CRM_PASSWORD}
    config:
      defaultLimit: 100
      maxLimit: 10000
      timeoutMs: 30000
```

Query tables by qualifying them with the SQL-facing source name:

```sql
SELECT customer_id, email, created_at
FROM crm.customers
WHERE status = ?
ORDER BY created_at DESC, customer_id ASC
OFFSET 0 ROWS FETCH NEXT 50 ROWS ONLY
```

## Supported Query Patterns

Use standard relational SQL patterns that Calcite can parse and plan:

- `SELECT` with explicit projections
- `WHERE` filters with comparison, boolean, range, null, and set predicates
- `JOIN` between tables in the same source or across registered sources
- `GROUP BY`, aggregate functions, and `HAVING`
- `UNION` and `UNION ALL` for compatible row shapes
- Subqueries and common table expressions for readable multi-step queries
- `ORDER BY` with deterministic sort keys
- Standard pagination with `OFFSET ... ROWS FETCH NEXT ... ROWS ONLY`

Federated SQL should be read-oriented. Use source-specific providers or pipeline
processors for writes, mutations, bulk indexing, and administrative operations.

## Unsupported Vendor Syntax

Do not paste vendor-specific SQL into Cheshire federated SQL. Common examples to avoid:

| Vendor syntax | Use instead |
|---------------|-------------|
| PostgreSQL `value::type` | Standard `CAST(value AS type)` |
| PostgreSQL `ILIKE` | Normalize values with `LOWER(...) LIKE LOWER(?)` |
| PostgreSQL `DISTINCT ON` | Use windowing or aggregation where supported |
| PostgreSQL JSON operators such as `->` and `->>` | Expose JSON fields as columns before querying |
| PostgreSQL `ON CONFLICT` or `RETURNING` | Use source-specific write paths |
| MySQL `LIMIT offset, count` | `OFFSET offset ROWS FETCH NEXT count ROWS ONLY` |
| SQL Server `TOP`, `[]` quoting, or `WITH (NOLOCK)` | Standard projection, quoting, and transaction settings |
| Oracle `ROWNUM`, hints, or `CONNECT BY` | Standard pagination and joins |
| Elasticsearch Query DSL or Elasticsearch SQL | Cheshire source-provider queries or SQL-facing validation patterns |

Vendor functions may work only when Calcite knows the function and the source adapter can
execute or translate it. Prefer standard SQL functions for portable capability contracts.

## Parameters

Keep values parameterized. Do not concatenate user input into SQL strings.

```sql
SELECT customer_id, email
FROM crm.customers
WHERE status = ?
  AND created_at >= ?
ORDER BY created_at DESC, customer_id ASC
OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
```

Guidelines:

- Use parameters for values, not identifiers. Table names, column names, operators, and
  sort directions should come from trusted capability configuration.
- Pass typed values from the calling layer: strings, numbers, booleans, dates, times, and
  timestamps.
- Validate required parameters before execution.
- Prefer positional `?` placeholders for raw federated SQL. If a higher-level Cheshire
  layer accepts named parameters, ensure it converts and binds them before execution.
- For `IN` filters, prefer a validated finite list generated by the calling layer, or
  model the list as a table and join to it.

## Ordering and Sorting

Rows are unordered unless the query includes `ORDER BY`. Always include ordering when an
API response, page, export, or validation check depends on row order.

Recommended pattern:

```sql
SELECT invoice_id, customer_id, invoice_date, total
FROM billing.invoices
WHERE customer_id = ?
ORDER BY invoice_date DESC, invoice_id ASC
OFFSET 0 ROWS FETCH NEXT 25 ROWS ONLY
```

Guidelines:

- Add a stable tie-breaker, usually the primary key.
- Avoid `ORDER BY 1`; name the field or alias.
- Be explicit about null handling in the query or capability contract if null ordering
  matters.
- Sort only by fields that are projected or clearly available from the source.

## Pagination

Use standard SQL pagination:

```sql
OFFSET 50 ROWS FETCH NEXT 25 ROWS ONLY
```

For user-facing APIs:

- Require a deterministic `ORDER BY`.
- Enforce a maximum page size in capability configuration.
- Prefer keyset pagination for large or frequently changing result sets.
- Avoid deep offsets over large federated joins; they can force the engine to scan and
  sort more rows than the caller receives.

Keyset pagination example:

```sql
SELECT invoice_id, invoice_date, total
FROM billing.invoices
WHERE customer_id = ?
  AND (invoice_date < ? OR (invoice_date = ? AND invoice_id > ?))
ORDER BY invoice_date DESC, invoice_id ASC
FETCH NEXT 25 ROWS ONLY
```

## Pushdown and Execution Behavior

Calcite parses, validates, and optimizes the federated SQL plan. Where a source adapter can
translate part of the plan, Cheshire can push work such as projection, filtering, sorting,
limits, and simple aggregates toward the source. Work that cannot be pushed down is handled
by the execution engine.

Design queries as if pushdown is helpful but not guaranteed:

- Project only the columns needed by the capability response.
- Filter as early and as selectively as possible.
- Keep cross-source joins small and intentional.
- Prefer joining on indexed keys in the underlying systems.
- Avoid source-specific functions in predicates that need pushdown.
- Validate performance with realistic data volumes, not only parser validation.

For JDBC sources, Calcite can often delegate significant work to the physical database
through its JDBC adapter. Cross-source joins, mixed-source aggregates, and non-translatable
expressions may execute in the federated engine.

## JDBC Validation Examples

Validate single-source SQL with portable syntax:

```sql
SELECT artist_id, name
FROM music.artists
WHERE name LIKE ?
ORDER BY name ASC, artist_id ASC
OFFSET 0 ROWS FETCH NEXT 20 ROWS ONLY
```

Validate a cross-source join:

```sql
SELECT
  c.customer_id,
  c.email,
  SUM(i.total) AS lifetime_value
FROM crm.customers AS c
JOIN billing.invoices AS i
  ON i.customer_id = c.customer_id
WHERE c.status = ?
GROUP BY c.customer_id, c.email
HAVING SUM(i.total) >= ?
ORDER BY lifetime_value DESC, c.customer_id ASC
FETCH NEXT 100 ROWS ONLY
```

Use these examples for capability contracts that read from JDBC-backed systems. The same
query should parse, validate against registered source metadata, and produce a stable row
shape for REST or MCP exposure.

## Elasticsearch Validation Examples

Elasticsearch Query DSL is not Cheshire federated SQL. Keep Elasticsearch-backed
capabilities clear about which layer is being validated:

- Validate native search request bodies with the Elasticsearch source provider.
- Validate SQL-facing contracts with Calcite-style relational SQL over the fields the
  capability exposes.

SQL-facing validation pattern for an article search contract:

```sql
SELECT article_id, title, author_id, published_at, score
FROM search.articles
WHERE published = TRUE
  AND author_id = ?
  AND published_at >= ?
ORDER BY published_at DESC, article_id ASC
OFFSET 0 ROWS FETCH NEXT 25 ROWS ONLY
```

Equivalent native Elasticsearch intent:

```json
{
  "query": {
    "bool": {
      "filter": [
        { "term": { "published": true } },
        { "term": { "author_id": "a-123" } },
        { "range": { "published_at": { "gte": "2026-01-01T00:00:00Z" } } }
      ]
    }
  },
  "sort": [
    { "published_at": "desc" },
    { "article_id": "asc" }
  ],
  "from": 0,
  "size": 25
}
```

Do not mix the JSON body into SQL. Use SQL to document and validate the relational row
shape, and use the [Elasticsearch Source Provider](ELASTICSEARCH_SOURCE_PROVIDER.md) for
native Elasticsearch execution details.

## Checklist

Before exposing federated SQL through a capability:

1. Confirm every referenced source is registered with a SQL-friendly schema name.
2. Qualify table names with the source schema.
3. Use Calcite-compatible SQL, not vendor dialect extensions.
4. Parameterize values and validate required inputs.
5. Include deterministic ordering for any paginated response.
6. Set page-size limits and avoid deep offsets on large federated data sets.
7. Test validation and execution with representative source metadata and data volume.
