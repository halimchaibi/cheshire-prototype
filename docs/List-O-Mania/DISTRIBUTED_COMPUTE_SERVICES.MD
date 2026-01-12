### **TODO – Cluster-Deployable PipelineExecutor & QueryEngine**

This TODO turns **PipelineExecutor and QueryEngine into first-class distributed compute services**, while keeping
RuntimeSession as the orchestration brain.

#### **1. Component Boundary Definition**

* Clearly separate **PipelineExecutor** and **QueryEngine** as standalone deployable services.
* Define strict input/output contracts (request, response, streaming, errors).
* Ensure RuntimeSession interacts only via interfaces (no in-process coupling).

---

#### **2. gRPC API Design**

* Define gRPC service contracts for:

    * `ExecutePipeline`
    * `ExecuteStep`
    * `ExecuteQuery`
    * Streaming partial results / progress
* Define lifecycle operations:

    * Health check
    * Cancel / Pause / Resume execution
* Use Protobuf schemas for:

    * StepInput / StepOutput
    * QueryRequest / QueryResult
    * ExecutionContext / Metadata

---

#### **3. Netty Transport Layer**

* Use **Netty-based gRPC server** for:

    * High-throughput async execution
    * Backpressure handling
* Configure:

    * Connection pooling
    * Flow control
    * Timeouts and retries

---

#### **4. RuntimeSession Invocation Model**

* RuntimeSession acts as a **client**:

    * Discovers remote executors (service registry / K8s service)
    * Routes execution to local or remote instance
* Support:

    * Fire-and-forget
    * Request/response
    * Streaming execution results

---

#### **5. Execution State Management**

* Externalize execution state:

    * Execution ID
    * Step state
    * Progress metrics
* Ensure pause/resume/cancel works across network boundaries.
* Avoid in-memory-only state.

---

#### **6. Serialization Strategy**

* Standardize serialization:

    * Protobuf for control plane
    * Optional Avro for data plane
* Ensure schema compatibility and versioning.

---

#### **7. Fault Tolerance**

* Handle:

    * Node failure during execution
    * gRPC timeouts and retries
* Implement:

    * Idempotent execution calls
    * Execution replay / recovery hooks

---

#### **8. Kubernetes Integration**

* Package PipelineExecutor and QueryEngine as containers.
* Define:

    * Deployments
    * Services (gRPC)
    * Horizontal Pod Autoscalers
* Enable:

    * Rolling upgrades
    * Zero-downtime redeploys

---

#### **9. Load Balancing & Scaling**

* Use K8s service load balancing or gRPC client-side LB.
* Scale based on:

    * CPU / memory
    * Queue depth
    * In-flight executions

---

#### **10. Security**

* Secure gRPC:

    * mTLS between RuntimeSession and executors
* Authenticate RuntimeSession requests.
* Enforce execution-level authorization.

---

#### **11. Observability**

* Emit metrics:

    * Execution latency
    * Step duration
    * Error rates
* Distributed tracing across:

    * RuntimeSession → Executor → QueryEngine
* Structured logs with execution IDs.

---

#### **12. Compatibility Mode**

* Support:

    * In-process execution (dev / single-node)
    * Remote execution (cluster mode)
* Configuration switch at RuntimeSession level.

---
