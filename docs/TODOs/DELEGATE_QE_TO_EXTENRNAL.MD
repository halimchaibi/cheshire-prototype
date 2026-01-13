# Cheshire API Fabric – Query Engine Flexibility Reference

## Overview

The **QueryEngine** in Cheshire is designed as a **pluggable SPI**. This allows the framework to operate with different
execution backends, enabling flexibility, interoperability, and customer-specific optimizations.

* **Embedded QueryEngine:** Low-latency, in-process execution (default: Apache Calcite).
* **External QueryEngine:** Delegates query execution to an external service (example: Apache Drill or Apache Druid),
  leveraging distributed capabilities without modifying the core framework.

This design ensures **future-proof integration** with existing enterprise systems.

---

## 1. Architectural Concept

```
Request
 → Runtime Session (long-lived singleton)
   → Pipeline
     → QueryEngine (SPI)
       → Source Providers (SPI)
```

* **Runtime Session:** Entry point to the backend, maintains lifecycle, capability routing, and registry management.
* **Pipeline:** Defines the sequence of transformations and queries.
* **QueryEngine (SPI):** Abstracts query execution, supporting multiple implementations.

---

### 1.1 Embedded QueryEngine (Default)

* Uses **Apache Calcite** for SQL parsing, query optimization, and logical plan generation.
* Executes queries **directly against Source Providers**.
* Low-latency, in-process, suitable for **high-throughput, per-request execution**.

---

### 1.2 External QueryEngine (Optional)

* Examples: **Apache Drill** or **Apache Druid** as backends.
* Accepts SQL or logical query objects.
* Delegates execution to an external cluster via **JDBC, REST, or client APIs**.

**Apache Drill:**

* General-purpose SQL engine for federated queries across multiple sources.
* Suitable for organizations with pre-existing Drill clusters.

**Apache Druid:**

* Specialized time-series OLAP engine.
* Supports fast aggregations, filters, and rollups.
* Queries via **Druid SQL (JDBC/ODBC) or native JSON API**.
* Ideal for **time-series analytics** or dashboard-heavy workloads.

> **Key:** Cheshire’s QueryEngine becomes a **wrapper** around the external engine while keeping SPI contracts intact.

---

## 2. Plug-and-Play Wrapper Pattern

**QueryEngine SPI**

```java
public interface QueryEngine {
    QueryResult execute(QueryRequest request, SessionContext ctx);
}
```

**Embedded Implementation – Calcite**

```java
public final class CalciteQueryEngine implements QueryEngine {
    public QueryResult execute(QueryRequest request, SessionContext ctx) {
        // parse, optimize, and execute using in-process Calcite
    }
}
```

**External Implementation – Drill**

```java
public final class DrillQueryEngine implements QueryEngine {
    private final DrillClient client;

    public QueryResult execute(QueryRequest request, SessionContext ctx) {
        return client.submitSql(request.sql());
    }
}
```

**External Implementation – Druid (Time-Series)**

```java
public final class DruidQueryEngine implements QueryEngine {
    private final DruidClient client;

    public QueryResult execute(QueryRequest request, SessionContext ctx) {
        // Submit time-series query via JDBC or HTTP JSON
        return client.submitSql(request.sql());
    }
}
```

**Benefits of Wrapper Approach:**

* Maintains **SPI compatibility** across different backends.
* Enables **easy integration** with customer-specific execution engines.
* Ensures **runtime session** and **pipeline orchestration** remain unchanged.

---

## 3. Integration Possibilities

* **Dual support:** Both embedded and external engines can co-exist.
* **Dynamic selection:** Runtime session or pipeline configuration can select which QueryEngine to use.
* **Minimal code changes:** Only the QueryEngine implementation changes; all pipeline and session logic remain intact.

---

## 4. Decision Considerations

| Engine Type      | Use Case                                     | Pros                                             | Cons                                          |
|------------------|----------------------------------------------|--------------------------------------------------|-----------------------------------------------|
| Embedded Calcite | Low-latency, in-process queries              | Fast, lightweight, full SPI control              | Limited to process memory, single-node        |
| External Drill   | Federated, analytical, or pre-existing Drill | Scales horizontally, leverages existing clusters | Higher latency, dependent on external service |
| External Druid   | Time-series analytics, fast aggregation      | Optimized for time-series, dashboards, rollups   | Limited general-purpose SQL support           |

---

## 5. Visual Representation (Simplified)

```
                 ┌──────────────────────────┐
                 │      Runtime Session     │
                 │ (Singleton / long-lived) │
                 └───────────┬──────────────┘
                             │
                             ▼
                  ┌─────────────────────┐
                  │   QueryEngine SPI   │
                  └─────────┬───────────┘
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ Embedded Calcite│ │ External Drill  │ │ External Druid  │
│  (In-process)   │ │ (Out-of-process)│ │ (Time-series)   │
└─────────────────┘ └─────────────────┘ └─────────────────┘
          │               │               │
          └───────────────┴───────────────┘
                          ▼
                  ┌─────────────────────┐
                  │ Source Providers    │
                  │Parquet/ORC/HDFS, S3 |
                  | (JDBC, Vector, Velox|
                  │ API, Spark, ...)    │
                  └─────────────────────┘
```

---

## 6. Summary

* **Cheshire’s QueryEngine SPI** enables **modular and pluggable query execution**.
* Supports **embedded and external engines**, including **time-series optimized Druid**.
* **Wrapper pattern** ensures minimal impact on pipeline orchestration and runtime sessions.
* Provides flexibility for enterprises to integrate **existing query engines** without modifying the core framework.
