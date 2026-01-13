# **Runtime Control & Observability - Cheshire TODOs**

## **1. Administrator Section (High-Privilege Actions)**

**Purpose:** Allows system admins or developers to manage capabilities, actions, pipelines, and query engines at
runtime.

### Features

1. **Dynamic Management**

    * Add/remove capabilities, actions, source providers, pipelines, and query engines **at runtime**.
    * Enable/disable specific actions or pipelines without restarting the system.

2. **Executor & Pipeline Control**

    * Configure parallelism, thread pool sizes, and scaling for Executors.
    * Adjust resource allocation per capability/action or pipeline.
    * Manage DAG execution and dependencies for complex workflows (ML models, query APIs, aggregation pipelines).

3. **Scheduler Control**

    * Register scheduled tasks with flexible triggers (cron-like, delayed, event-based).
    * Configure **push/pull mode** for scheduled data delivery.
    * Set output formats: PDF, CSV, HTML, JSON, etc.
    * Define delivery channels: FTP, S3, Kafka, or HTTP push.

4. **Runtime Hooks**

    * Monitor resource usage: CPU, memory, I/O per task or capability.
    * Backpressure handling and failure recovery strategies.
    * Force pause, resume, or cancel tasks globally or selectively.

5. **Dashboard / Monitoring**

    * Admin dashboard for live monitoring of system-wide tasks, pipelines, and resource utilization.
    * DAG visualization for complex pipelines.
    * Real-time metrics using **Reactor** for observability and event streaming.

---

## **2. User Section (Monitoring & Interaction)**

**Purpose:** Enables end-users to track, control, and interact with running actions within their permitted capabilities.

### Features

1. **Action Monitoring**

    * Query the status of running or scheduled actions: queued, running, paused, completed, or failed.
    * View metrics: progress, execution time, and basic resource usage.

2. **Action Control**

    * **Cancel**: Stop a running or scheduled action.
    * **Pause / Resume**: Temporarily suspend execution and resume later.
    * Receive notifications or callbacks when actions complete, including URLs to download results.

3. **Scheduler Interaction**

    * Register new scheduled tasks (depending on allowed capabilities).
    * Select **pull-based** or **push-based** execution modes.
    * Choose preferred output format: PDF, CSV, HTML, JSON.
    * Configure delivery channels within user permissions (e.g., S3, Kafka, HTTP callback).

4. **Dashboard / Live Updates**

    * Personal dashboard showing user-specific tasks and schedule status.
    * Live progress updates via WebSocket or SSE streams.
    * Download or access action results from completed tasks.

### **Dynamic Capability Management**

**Goal:** Allow runtime registration, removal, and update of all pluggable components without restarting the system.

#### 1. **Components**

* **Capabilities / Actions**: Add, remove, or update business-aligned units and their actions dynamically.
* **Source Providers**: Enable plugging new data sources or removing old ones at runtime.
* **Pipeline Execution / Executors**: Swap or upgrade pre/post-processing, or executor logic dynamically.
* **Query Engines**: Allow dynamic replacement or extension to support new query patterns, ML models, DAGs, etc.

#### 2. **Implementation Considerations**

* Use **Java `ServiceLoader` / SPI** to discover and load modules dynamically.
* Maintain a **registry / catalog** of active capabilities, actions, and pluggable modules.
* Provide **thread-safe operations** for add/remove/update, possibly via `AtomicReference` or `ConcurrentHashMap`.
* Ensure **RuntimeSession / PipelineExecutor** can pick up updated components seamlessly.
* Include **observability hooks** for tracking runtime changes.
* Handle **versioning and compatibility**, e.g., older pipelines still valid while updating.

#### 3. **API / Developer UX**

* Expose endpoints (REST / MCP / GQL) or admin interface to:

    * Register / deregister capabilities/actions.
    * Add/remove/update Source Providers.
    * Swap or update pipeline stages and executors.
    * Extend Query Engine logic.

---
