## [ADR-010]: Calcite Query Engine Execution Pipeline & Staged Testing

* **Status:** Accepted  
* **Deciders:** Halim Chaibi (Author/Architect)  
* **Date:** 2026-01-29
* **Last Updated:** 2026-01-29 (Implementation in progress)
* **Technical Story:** Calcite-based query engine refactor & unit test pipeline

### 1. Context and Problem Statement

The original Calcite-based query engine prototype in `cheshire-query-engine-calcite` had:

- Incomplete or placeholder components (`FrameworkInitializer`, `QueryExecutor`, `Converter`, `QueryOptimizer`)
- No clear separation between **session-level** and **query-level** responsibilities
- A monolithic `execute()` method without isolated tests for each stage (parse, validate, convert, optimize, execute, transform)
- Tight coupling to configuration maps with hardcoded string keys

This made the engine hard to reason about, difficult to test incrementally, and risky to evolve toward a production-ready query federator.

The goal is to:

- Define a **staged execution pipeline** for the Calcite engine
- Provide **fine-grained unit tests** that validate each stage independently
- Establish a baseline architecture for future rule selection and adapter expansion

### 2. Decision Drivers

* **Testability:** Each pipeline stage must be independently testable (PARSE, VALIDATE, CONVERT, OPTIMIZE, EXECUTE, TRANSFORM).
* **Maintainability:** Clear separation of concerns between session setup, planning, optimization, execution, and result transformation.
* **Extensibility:** Support future adapters and rule selectors without rewriting the core engine.
* **Observability:** Consistent logging at each stage for debugging and performance analysis.

### 3. Considered Options

**Staged execution pipeline** with explicit components (`QueryParser`, `QueryValidator`, `Converter`, `QueryOptimizer`, `QueryExecutor`, `ResultTransformer`) orchestrated via a `stage(ExecutionStage, CheckedSupplier)` helper.

### 4. Decision Outcome

**Chosen option:** Staged execution pipeline.

**Justification:**

- Allows each stage to be tested and refactored independently (Phase 1–3 tests).
- Makes it straightforward to insert instrumentation, metrics, or alternative implementations for specific stages (e.g., different optimizers).
- Aligns with Calcite’s conceptual pipeline (SQL → SqlNode → RelNode → optimized RelNode → results).

---

### 5. Architecture Overview

#### 5.1 Execution Pipeline (CalciteQueryEngine)

`CalciteQueryEngine` now orchestrates the following stages:

1. **PARSE**: `planner.parse(sql)`  
2. **VALIDATE**: `planner.validate(parsed)` (eventually via `QueryValidator`)  
3. **CONVERT**: `converter.convert(validated)` → `RelNode`  
4. **OPTIMIZE**: `QueryOptimizer.optimizeWithVolcano(logicalPlan, OptimizationContext)`  
5. **EXECUTE**: `QueryExecutor.execute(optimizedPlan)` → `ResultSet`  
6. **TRANSFORM**: `ResultTransformer.transform(resultSet)` → `QueryEngineResult`

Each stage is wrapped by:

```java
private <T> T stage(ExecutionStage stage, LambdaUtils.CheckedSupplier<T> supplier)
```

which provides logging, timing, and unified error handling (`QueryExecutionException`).

#### 5.2 Session vs Query Responsibilities

- **Session-level:**
  - `SchemaManager`: registers sources and builds Calcite schemas.
  - `FrameworkInitializer`: builds `FrameworkConfig` (parser config, operator table, programs, trait defs, executor, default schema).
  - `CalciteQueryEngine.open()`: initializes parser, validator, converter, optimizer, executor, result transformer, and plan cache.

- **Query-level:**
  - `OptimizationContext`: captures query type, involved schemas, characteristics, and hints.
  - `QueryCharacteristics`: describes structural aspects (joins, aggregates, table count, etc.).
  - `QueryOptimizer`: selects rules via `RuleSelector` and applies them (HepPlanner / Volcano).

### 6. Pros and Cons

#### Staged Execution Pipeline

**Pros:**

- High testability: each stage is covered by targeted JUnit tests:
  - Phase 1: Instantiation (engine, factory, config adapter)
  - Phase 2: `open()` initializes `SchemaManager` and `FrameworkConfig`
  - Phase 3: PARSE, VALIDATE, CONVERT, OPTIMIZE, EXECUTE, TRANSFORM stages
- Clear separation of concerns: easier to trace and debug failures.
- Supports future enhancements (custom rule selectors, multiple executors, streaming).

**Cons:**

- More classes and wiring to understand (slightly higher initial complexity).
- Uses reflection in tests to exercise internal `stage()` method (test-only complexity).

---

### 7. Consequences

**Positive (+):**

- The engine can be evolved in small, safe steps (e.g., swapping optimizers, adding adapters).
- Bugs in parsing, validation, optimization, execution, or transformation can be isolated quickly.
- Unit tests document the intended behavior of each stage.

**Negative (-):**

- Slightly more boilerplate (builders, context objects, stage enum).

**Neutral:**

- Tests rely on reflection to call private `stage()` – acceptable as they are strictly in the test layer.

### 8. Links & References
* Class Diragram `cheshire-prototype/docs/diagrams/src/class/class-calcite-query-engine.puml`
* `cheshire-query-engine-calcite/src/main/java/io/cheshire/query/engine/calcite/CalciteQueryEngine.java`
* `cheshire-query-engine-calcite/src/main/java/io/cheshire/query/engine/calcite/FrameworkInitializer.java`
* `cheshire-query-engine-calcite/src/main/java/io/cheshire/query/engine/calcite/optimizer/QueryOptimizer.java`
* `cheshire-query-engine-calcite/src/main/java/io/cheshire/query/engine/calcite/schema/SchemaManager.java`
* `cheshire-query-engine-calcite/src/main/java/io/cheshire/query/engine/calcite/executor/QueryExecutor.java`
* `cheshire-query-engine-calcite/src/main/java/io/cheshire/query/engine/calcite/transformer/ResultTransformer.java`
* `cheshire-query-engine-calcite/src/test/java/io/cheshire/query/engine/calcite/CalciteQueryEngineTest.java`

---

## 9. Revision History

| Date | Version | Status | Description | Author |
| :--- | :--- | :--- | :--- | :--- |
| 2026-01-29 | v1.0 | **Accepted** | Initial design of the 6-stage execution pipeline. | Halim Chaibi |
| 2026-01-29 | v1.1 | **In Progress** | Implementation in progress; added internal `stage()` helper logic. | Halim Chaibi |

> **Note:** This ADR is currently being refined in parallel with the implementation of the `cheshire-query-engine-calcite` module.