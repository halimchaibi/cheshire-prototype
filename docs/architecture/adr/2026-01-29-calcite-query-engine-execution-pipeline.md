## [ADR-010]: Calcite Query Engine Execution Pipeline & Staged Testing

* **Status:** Accepted
* **Deciders:** Halim Chaibi (Author/Architect)
* **Date:** 2026-01-29
* **Last Updated:** 2026-06-24
* **Technical Story:** Calcite-based query engine refactor & unit test pipeline

### 1. Context and Problem Statement

The original Calcite-based query engine prototype in `cheshire-query-engine-calcite` had:

- Incomplete planning and execution boundaries in the original prototype
- No clear separation between **session-level** and **query-level** responsibilities
- A monolithic `execute()` method without isolated tests for each stage (parse, validate, convert, optimize, execute, transform)
- Tight coupling to configuration maps with hardcoded string keys

This made the engine hard to reason about, difficult to test incrementally, and risky to evolve toward a production-ready query federator.

The goal is to:

- Define a **staged execution pipeline** for the Calcite engine
- Provide **fine-grained tests** that validate stage behavior and public engine contracts
- Establish a baseline architecture for future rule selection and adapter expansion

### 2. Decision Drivers

* **Testability:** Each pipeline stage must be independently testable (PARSE, VALIDATE, CONVERT, OPTIMIZE, EXECUTE, TRANSFORM).
* **Maintainability:** Clear separation of concerns between session setup, planning, optimization, execution, and result transformation.
* **Extensibility:** Support future adapters and rule selectors without rewriting the core engine.
* **Observability:** Consistent logging at each stage for debugging and performance analysis.

### 3. Considered Options

**Staged execution pipeline** with explicit planning, execution, and result-transformation
boundaries orchestrated via a `stage(ExecutionStage, CheckedSupplier)` helper.

### 4. Decision Outcome

**Chosen option:** Staged execution pipeline.

**Justification:**

- Allows each stage to be tested and refactored independently.
- Makes it straightforward to insert instrumentation, metrics, or alternative implementations for specific stages (e.g., different optimizers).
- Aligns with Calcite’s conceptual pipeline (SQL → SqlNode → RelNode → optimized RelNode → results).

---

### 5. Architecture Overview

#### 5.1 Execution Pipeline (CalciteQueryEngine)

`CalciteQueryEngine` now orchestrates the following stages:

1. **PARSE**: `planner.parse(sql)`
2. **VALIDATE**: `planner.validate(parsed)`
3. **CONVERT**: `planner.rel(validated).rel` → `RelNode`
4. **OPTIMIZE**: query-scoped rule program boundary for the `RelNode`
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
  - `CalciteQueryEngine.open()`: initializes the schema manager, base framework config, executor,
    result transformer, and plan cache.

- **Query-level:**
  - `RuleSetBuilder`: selects Calcite rules from configured source capabilities.
  - `CalcitePlanner`: parses, validates, and converts SQL using a query-scoped framework config.

### 6. Pros and Cons

#### Staged Execution Pipeline

**Pros:**

- High testability: each stage is covered by targeted JUnit tests:
- Public contract coverage for `validate(...)`, `explain(...)`, streaming capability, and factory
  config validation.
- H2-backed query execution coverage for constants, filters, joins, aggregation, ordering, and
  nested query shapes.
- Focused unit coverage for query plan cache behavior.
- Clear separation of concerns: easier to trace and debug failures.
- Supports future enhancements such as adapter-specific rule selectors, multiple executors, and
  streaming.

**Cons:**

- More classes and wiring to understand (slightly higher initial complexity).
- Maintains a small amount of internal stage-test complexity while public contract tests cover the
  externally visible behavior.

---

### 7. Consequences

**Positive (+):**

- The engine can be evolved in small, safe steps (e.g., swapping optimizers, adding adapters).
- Bugs in parsing, validation, optimization, execution, or transformation can be isolated quickly.
- Unit tests document the intended behavior of each stage.

**Negative (-):**

- Slightly more boilerplate (builders, context objects, stage enum).

**Neutral:**

- The MVP intentionally reports `supportsStreaming() == false` because the current executor
  materializes rows through `ResultTransformer`.

### 8. Links & References
* Class Diagram `cheshire-prototype/docs/diagrams/src/class/class-calcite-query-engine.puml`
* `cheshire-query-engine-calcite/src/main/java/io/cheshire/query/engine/calcite/CalciteQueryEngine.java`
* `cheshire-query-engine-calcite/src/main/java/io/cheshire/query/engine/calcite/FrameworkInitializer.java`
* `cheshire-query-engine-calcite/src/main/java/io/cheshire/query/engine/calcite/optimizer/QueryOptimizer.java`
* `cheshire-query-engine-calcite/src/main/java/io/cheshire/query/engine/calcite/schema/SchemaManager.java`
* `cheshire-query-engine-calcite/src/main/java/io/cheshire/query/engine/calcite/executor/QueryExecutor.java`
* `cheshire-query-engine-calcite/src/main/java/io/cheshire/query/engine/calcite/transformer/ResultTransformer.java`
* `cheshire-query-engine-calcite/src/test/java/io/cheshire/query/engine/calcite/CalciteQueryEngineTest.java`
* `cheshire-query-engine-calcite/src/test/java/io/cheshire/query/engine/calcite/CalciteQueryEngineContractTest.java`
* `cheshire-query-engine-calcite/src/test/java/io/cheshire/query/engine/calcite/query/QueryPlanCacheTest.java`

---

## 9. Revision History

| Date | Version | Status | Description | Author |
| :--- | :--- | :--- | :--- | :--- |
| 2026-01-29 | v1.0 | **Accepted** | Initial design of the 6-stage execution pipeline. | Halim Chaibi |
| 2026-06-24 | v1.1 | **Accepted** | Updated for MVP behavior: validation, explain, materialized execution, and public contract tests. | Halim Chaibi |
