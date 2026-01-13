## **CapScheduler**

To schedule capabilities and their actions (user configurable)

Summary for implementation:

1. **Scheduler Service**

    * Core component responsible for storing scheduled tasks, coordinating triggers, and managing retries/failures.
    * Supports cron-like, delayed, or recurring schedules.

2. **API Endpoint**

    * `POST /{capability}/schedule`
    * Accepts: capability, action, parameters, schedule time/frequency, callback URL.
    * Returns: `scheduleId` and initial status.

3. **Triggering RuntimeSession**

    * At scheduled time, the scheduler invokes the **RuntimeSession** with a canonical `RequestEnvelope`.
    * Supports synchronous or asynchronous execution depending on task type.

4. **Result Notification**

    * On completion, scheduler sends notification (HTTP callback, webhook, or message) to user with a **result retrieval
      URL**.
    * Can integrate with observability/monitoring (Reactor) for tracking status.

5. **Persistence & Fault Tolerance**

    * Scheduled tasks stored in durable storage (DB, queue).
    * Retry logic for failed tasks.

6. **Optional Features**

    * Dashboard/UI for users to track scheduled tasks.
    * Task dependencies to execute sequences or DAGs of actions.
    * Support for multiple schedulers in clustered environment.

This can be merged with **FlowEngine** for orchestrating complex tasks or DAG-based execution.

---

### **CapScheduler** extension

It will be extended with **dual-mode delivery**:

### 1. **Execution Modes**

* **Pull-Based**:

    * User queries the result using a `resultUrl` once the task is completed.
    * Good for on-demand retrieval or when user prefers to fetch data manually.
* **Push-Based**:

    * Scheduler actively delivers the result once ready.
    * Can push via multiple channels: FTP, S3, Kafka, email, webhook, etc.
    * Delivery format configurable: PDF, CSV, HTML, JSON, Parquet, etc.

### 2. **Workflow**

1. User schedules a capability/action with parameters, execution mode, and delivery preference.
2. Scheduler stores the task and triggers **RuntimeSession** at the right time.
3. Execution result is transformed into requested format.
4. Delivery happens:

    * **Pull**: store result with secure URL for retrieval.
    * **Push**: send to target channel(s) automatically.
5. Observability/monitoring tracks execution, delivery status, and failures.

### 3. **Implementation Considerations**

* **Format Conversion**: pluggable transformers for each output format.
* **Delivery Channels**: pluggable adapters (FTP, S3, Kafka, email).
* **Error Handling**: retry, fallback, and alerting on delivery failure.
* **Security**: ensure access control for both pull URLs and push destinations.
* **Observability**: integrate with Reactor for metrics on execution, delivery, and SLA adherence.

This turns **CapScheduler** into a full-featured, capability-driven **execution & delivery orchestrator**.