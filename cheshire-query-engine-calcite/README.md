# Cheshire Calcite Query Engine

`cheshire-query-engine-calcite` provides the Apache Calcite-backed query engine for Cheshire.
The current MVP focuses on SQL planning and local materialized execution over configured JDBC
sources.

## Current Scope

- Registers configured JDBC sources as Calcite schemas.
- Parses and validates SQL queries through Calcite.
- Converts validated SQL into relational plans.
- Executes plans through Calcite's local enumerable path.
- Transforms JDBC `ResultSet` output into `QueryEngineResult`.
- Supports `validate(...)` and `explain(...)` through the public `QueryEngine` contract.
- Uses a query plan cache component for future plan reuse; execution currently replans per request.

Streaming is not advertised by this module yet. The executor materializes result rows before
returning them to Cheshire.

## Configuration Shape

```yaml
query-engines:
  calcite:
    factory: io.cheshire.query.engine.calcite.CalciteQueryEngineFactory
    config:
      name: calcite
      sources:
        blog-db:
          name: blog-db
          type: jdbc
          config:
            type: jdbc
            schema: public
            connection:
              url: jdbc:postgresql://localhost:5432/blog
              driver: org.postgresql.Driver
              username: blog_user
              password: blog_password
```

The factory rejects configurations without a non-blank engine name or without at least one source.

## Query Flow

1. `open()` builds the root schema, registers sources, and creates the base Calcite framework config.
2. `execute(...)` builds a query-scoped planning context from the SQL and execution context.
3. Calcite parses, validates, and converts SQL into a `RelNode`.
4. The executor registers schemas on a Calcite JDBC connection and executes the plan.
5. The result transformer returns columns and rows as `QueryEngineResult`.

## Tests

Run the Calcite module tests with:

```bash
./mvnw -Ptest -pl cheshire-query-engine-calcite test
```

Focused tests cover:

- Public query engine contract behavior: validation, explain, streaming capability, config
  validation.
- Query plan cache behavior: disabled cache, LRU eviction, and expiration.
- H2-backed Chinook query execution for constants, filters, joins, aggregation, ordering, and
  nested query shapes.
