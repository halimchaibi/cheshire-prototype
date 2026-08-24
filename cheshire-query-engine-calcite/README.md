# Cheshire Calcite Query Engine

`cheshire-query-engine-calcite` provides the Apache Calcite-backed query engine for Cheshire.
The current MVP focuses on SQL planning and local materialized execution over configured JDBC,
Arrow, CSV, file, Cassandra, InnoDB, OS metadata, Redis, Kafka, and plugin-backed MAT sources.

## Current Scope

- Registers configured sources as Calcite schemas.
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
        blog-arrow:
          name: blog-arrow
          type: arrow
          config:
            type: arrow
            directory: ./data/blog-arrow
        blog-csv:
          name: blog-csv
          type: csv
          config:
            type: csv
            directory: ./data/blog-csv
```

The factory rejects configurations without a non-blank engine name or without at least one source.
Directory-backed sources must point to existing directories. Arrow exposes `.arrow` files as
tables. CSV uses Calcite's file schema support and exposes `.csv` files as tables.

## Supported Source Adapters

| Type | Config shape |
|------|--------------|
| `jdbc` | Cheshire JDBC source config with `connection` and optional physical `schema`. |
| `arrow` | `directory`, pointing to local Arrow Feather files. |
| `csv` | `directory`, pointing to local CSV files. |
| `file` | Either `directory` for local `.csv` and `.json` discovery, or `tables` entries with `name` and `url`. |
| `cassandra` | `host` and `keyspace`; optional `port`, `username`, and `password`. Schema creation opens the Cassandra session. |
| `innodb` | `sqlFilePath` list plus `ibdDataFileBasePath`; optional `timeZone`. |
| `os` | No required config; exposes safe OS/JVM metadata table functions. |
| `redis` | `host`, `port`, `database`, and `tables`; optional `password`. |
| `kafka` | `tables` entries with Kafka table operands such as `bootstrap.servers`, `topic.name`, and `consumer.params`. |
| `mat` | `factoryClass` and optional `operand`; the MAT Calcite plugin must be present on the runtime classpath. |

Redis tables use a map-friendly Cheshire shape and are converted to Calcite custom table models
before schema creation:

```yaml
cache:
  name: cache
  type: redis
  config:
    type: redis
    host: localhost
    port: 6379
    database: 0
    tables:
      - name: people
        operand:
          dataFormat: raw
          fields:
            - name: id
              type: varchar
              mapping: id
```

Kafka tables are registered through Calcite's table factory:

```yaml
events:
  name: events
  type: kafka
  config:
    type: kafka
    tables:
      - name: events
        operand:
          bootstrap.servers: localhost:9092
          topic.name: events
          consumer.params:
            key.deserializer: org.apache.kafka.common.serialization.ByteArrayDeserializer
            value.deserializer: org.apache.kafka.common.serialization.ByteArrayDeserializer
```

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
- Arrow adapter registration, validation, and rule selection.
- Built-in Calcite source adapter registration and validation for CSV, File, Redis, InnoDB,
  Kafka, OS metadata, Cassandra pre-validation, and MAT plugin hooks.
- H2-backed Chinook query execution for constants, filters, joins, aggregation, ordering, and
  nested query shapes.
