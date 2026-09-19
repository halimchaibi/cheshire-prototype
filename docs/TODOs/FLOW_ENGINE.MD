## **Enhance Executor & Query Engine to DAG-Based, Capability-Oriented Execution**

### **Goal**

Refactor the current **Query Engine + Executor** pattern to move beyond simple query execution. Allow the **Executor**
to orchestrate **complex logic**, including:

* Building query APIs dynamically
* Executing machine learning models
* Performing data transformations and aggregations
* Chaining multiple steps in a **Directed Acyclic Graph (DAG)**

### **Proposed Changes**

1. **Executor Evolution**

    * Rename/rethink “Query Engine” as **Execution Engine** or **Task Engine**.
    * Executor should accept **abstract tasks** (not just queries).
    * Tasks can represent:

        * Data fetch
        * ML inference
        * Aggregation/transformation
        * External service calls

2. **DAG Construction**

    * Tasks are nodes in a DAG; edges represent data or execution dependencies.
    * DAG allows:

        * Parallel execution where possible
        * Controlled sequential execution for dependent tasks
        * Reuse of intermediate results

3. **PipelineExecutor Integration**

    * PreProcessors: enrich, validate, or transform input before DAG execution
    * Executor: traverse DAG, execute nodes, manage async tasks
    * PostProcessors: format results, enrich, or persist outputs

4. **Observability & Monitoring**

    * Each DAG node emits **events/logs/metrics** for monitoring via Reactor or ODap streams.
    * Track success/failure, duration, and resource consumption per node.

5. **Extensibility**

    * Support new task types by implementing a **Task interface**
    * Dynamically register tasks using **SPI / ServiceLoader**

### **Benefits**

* Supports **complex workflows** beyond simple queries
* Enables **ML and advanced analytics pipelines**
* DAG structure improves **parallelism, reliability, and observability**
* Maintains **capability-driven, protocol-agnostic design**