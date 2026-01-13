# Envoy Integration Draft – Fronting Jetty-Based Execution Framework

## 1. Objective

Introduce **Envoy Proxy** as a **dedicated edge and transport layer** in front of the existing Jetty-based backend
framework, without impacting core execution semantics, SPI extensibility, or session lifecycle.

The goal is to **externalize cross-cutting transport concerns** (security, traffic control, observability) while keeping
**business execution and orchestration fully owned by the framework**.

---

## 2. Non-Goals

This integration explicitly does **not** aim to:

* Replace Jetty as the application server
* Replace the Protocol Adapter / Request Handler logic
* Move execution semantics or session lifecycle outside the framework
* Embed Envoy inside the pipeline or runtime session

Envoy is treated as **infrastructure**, not application logic.

---

## 3. High-Level Architecture

### Logical Placement

```
Clients
  |
Envoy Proxy (Edge / L7)
  |
Jetty Server (Application Runtime)
  |
RequestHandler
  |
Runtime Session (singleton, long-lived)
  |
PipelineExecutor (SPI-driven)
```

### Deployment Model

* Envoy and Jetty run as **separate processes**
* Typically co-located:

    * Same Kubernetes pod (sidecar pattern), or
    * Same VM / host
* Communication via `localhost`

---

## 4. Responsibility Split

### Envoy Responsibilities (Externalized)

Envoy owns **all transport and policy-level concerns**:

* Connection management

    * HTTP/1.1, HTTP/2, gRPC
    * WebSocket upgrades
* Security

    * TLS / mTLS termination
    * Certificate rotation
    * Identity-based routing
* Traffic management

    * Rate limiting
    * Retries (bounded, idempotent)
    * Circuit breaking
    * Timeouts
* Traffic shaping

    * Canary releases
    * A/B testing
    * Traffic shadowing
* Observability

    * Request metrics
    * Latency histograms
    * Distributed tracing headers
* Zero-trust patterns

    * Service mesh compatibility

---

### Jetty + Framework Responsibilities (Preserved)

Jetty and the backend framework retain **semantic ownership**:

* Protocol-to-domain translation

    * Mapping inbound requests → `RequestEnvelope`
* Input validation and normalization
* Async request handling
* Runtime session management

    * Long-lived, singleton entry point
    * Tracking active pipelines and results
* Pipeline orchestration

    * PreProcessors / Executor / PostProcessors (SPI)
* Query execution and federation
* Streaming responses tied to execution state

No framework behavior is delegated to Envoy.

---

## 5. Request Flow with Envoy

1. Client sends request (HTTP / gRPC / WS)
2. Envoy:

    * Terminates TLS
    * Applies rate limits / retries
    * Emits transport metrics
3. Envoy forwards request to Jetty over internal HTTP
4. Jetty receives request:

    * RequestHandler translates to `RequestEnvelope`
    * Dispatches into Runtime Session
5. Runtime Session:

    * Executes pipeline asynchronously
    * Tracks execution lifecycle
6. Response:

    * Streamed or completed response returned to Jetty
    * Jetty returns response to Envoy
    * Envoy forwards response to client

---

## 6. Configuration Boundaries

### Envoy Configuration

* Listener configuration (ports, protocols)
* TLS / mTLS certificates
* Rate limit policies
* Retry budgets and timeouts
* Routing rules (headers, paths, weights)
* Observability sinks (Prometheus, OpenTelemetry)

### Application Configuration (Unchanged)

* Pipeline definitions
* SPI registrations
* Query engine rules
* Source providers
* Session policies

This keeps **infra configuration and application configuration decoupled**.

---

## 7. Failure and Backpressure Model

* Envoy enforces **edge backpressure**

    * Connection limits
    * Request queueing
* Jetty enforces **execution backpressure**

    * Async request limits
    * Virtual thread isolation
* Runtime Session remains authoritative for:

    * Cancellation
    * Partial results
    * Execution visibility

No retries are performed inside the pipeline.

---

## 8. Security Model

* Envoy handles:

    * External authentication (mTLS, JWT validation)
    * Zero-trust enforcement
* Jetty and framework:

    * Receive already-authenticated identity headers
    * Apply domain-level authorization if needed

This avoids security logic duplication.

---

## 9. Observability Strategy

### Envoy (Transport-Level)

* Request counts
* Error rates
* Latency percentiles
* Connection metrics

### Framework (Execution-Level)

* Pipeline duration
* Query execution time
* Source-level performance
* Business KPIs

Together, they provide **end-to-end visibility without overlap**.

---

## 10. Risks and Mitigations

| Risk                 | Mitigation                        |
|----------------------|-----------------------------------|
| Overlapping retries  | Disable retries in application    |
| Double TLS           | TLS only at Envoy                 |
| Debug complexity     | Clear ownership boundaries        |
| Performance overhead | Localhost communication; async IO |

---

## 11. Why This Design Is Sustainable

* Aligns with industry-standard sidecar / edge proxy patterns
* Preserves framework IP and extensibility
* Allows independent evolution of infra and application
* Enables service mesh adoption without refactoring core logic

---

## 12. Summary

Envoy is introduced as a **pure edge and transport layer** that enhances security, scalability, and observability, while
Jetty and the backend framework remain the **semantic and execution authority**.

This integration strengthens the platform without diluting its architectural clarity.

Below are **both deliverables**, clean and reusable.

---
# Deployment Diagram
---

## 1. Deployment Diagram – K8s and VM Variants

### 1.1 Kubernetes (Recommended)

This is the **canonical model**. Envoy is a sidecar; Jetty remains the application runtime.

```
┌──────────────────────────────┐
│          Kubernetes          │
│                              │
│  ┌────────────────────────┐  │
│  │        Pod              │  │
│  │                        │  │
│  │  ┌───────────────┐     │  │
│  │  │ Envoy Proxy   │     │  │
│  │  │ (Sidecar)     │     │  │
│  │  │               │     │  │
│  │  │ - TLS/mTLS    │     │  │
│  │  │ - Rate Limit  │     │  │
│  │  │ - Retries     │     │  │
│  │  │ - Metrics     │     │  │
│  │  └──────┬────────┘     │  │
│  │         │ localhost    │  │
│  │  ┌──────▼────────┐     │  │
│  │  │ Jetty Server  │     │  │
│  │  │               │     │  │
│  │  │ - Request     │     │  │
│  │  │   Handler     │     │  │
│  │  │ - Runtime     │     │  │
│  │  │   Session     │     │  │
│  │  │ - Pipelines   │     │  │
│  │  └───────────────┘     │  │
│  │                        │  │
│  └────────────────────────┘  │
│                              │
└──────────────────────────────┘
```

**Ingress options**

* External LB → Envoy (recommended)
* Service Mesh (Istio / Linkerd-compatible)

**Key properties**

* Zero network hops between Envoy and Jetty
* Independent scaling of Pods
* Envoy config managed via ConfigMap / xDS
* Jetty remains stateless; Runtime Session is per-pod singleton

---

### 1.2 VM / Bare-Metal Deployment

Useful for legacy or non-K8s environments.

```
┌──────────────────────────────┐
│            VM                │
│                              │
│  ┌────────────────────────┐  │
│  │ Envoy Proxy             │  │
│  │                         │  │
│  │ - TLS termination       │  │
│  │ - Traffic control       │  │
│  │ - Observability         │  │
│  └──────────┬─────────────┘  │
│             │ localhost      │
│  ┌──────────▼─────────────┐  │
│  │ Jetty Server            │  │
│  │                         │  │
│  │ - Request Handler       │  │
│  │ - Runtime Session       │  │
│  │ - Execution Pipelines   │  │
│  └────────────────────────┘  │
│                              │
└──────────────────────────────┘
```

**Key difference**

* Manual lifecycle management
* Envoy and Jetty supervised separately (systemd / Docker)
* Same responsibility boundaries

---

## 2. Architecture Decision Record (ADR)

### ADR-007: Introduce Envoy Proxy in Front of Jetty Runtime

**Status**
Accepted

---

### Context

The platform exposes multiple APIs (REST, WebSocket, async protocols) backed by a Jetty-based execution framework with a
long-lived Runtime Session and SPI-driven pipelines.

Currently, transport-level concerns (TLS, rate limiting, retries, observability) are tightly coupled or inconsistently
handled across deployments.

We need:

* Stronger edge security
* Consistent traffic management
* Better observability
* A path toward service mesh adoption

Without impacting core execution semantics.

---

### Decision

Introduce **Envoy Proxy as an external edge layer**, deployed alongside Jetty, responsible exclusively for **transport-
and policy-level concerns**.

Jetty remains the application server and execution authority.

---

### Scope of Envoy

Envoy will handle:

* TLS / mTLS termination
* Connection management (HTTP/1.1, HTTP/2, gRPC, WS)
* Rate limiting, retries, circuit breaking
* Traffic shaping (canary, A/B)
* Transport-level observability
* Zero-trust integration

Envoy will **not**:

* Perform protocol-to-domain translation
* Handle request validation
* Manage sessions or pipelines
* Execute business logic

---

### Consequences

**Positive**

* Clear separation of infrastructure vs application logic
* Improved security posture
* Production-grade traffic control
* Mesh compatibility without refactoring
* Reduced complexity inside Jetty

**Negative**

* Additional operational component
* Envoy configuration complexity
* Debugging spans multiple layers (mitigated by tracing)

---

### Alternatives Considered

1. **Jetty-only solution**
   Rejected: reinvents solved infra problems, poor mesh compatibility.

2. **Replace Jetty with Envoy + WASM / gRPC-only backend**
   Rejected: breaks existing APIs and execution model.

3. **Embed Envoy inside application**
   Rejected: violates separation of concerns.

---

### Risk Mitigation

* Disable retries in application layer
* Single TLS termination point
* Clear ownership documentation
* Standardized tracing headers

---

### Resulting Architecture Principle

> Transport concerns are infrastructure responsibilities; execution semantics remain application-owned.

---

# **minimal, representative code snippets** showing how Envoy is integrated **on top of Jetty

**, without bleeding responsibilities.

No fluff. These are the pieces people actually ask for in reviews.

---

## 1. Envoy – Listener + HTTP Connection Manager

**envoy.yaml (static bootstrap, simplified)**

```yaml
static_resources:
  listeners:
    - name: ingress_listener
      address:
        socket_address:
          address: 0.0.0.0
          port_value: 8443
      filter_chains:
        - transport_socket:
            name: envoy.transport_sockets.tls
            typed_config:
              "@type": type.googleapis.com/envoy.extensions.transport_sockets.tls.v3.DownstreamTlsContext
              common_tls_context:
                tls_certificates:
                  - certificate_chain:
                      filename: /etc/certs/tls.crt
                    private_key:
                      filename: /etc/certs/tls.key
          filters:
            - name: envoy.filters.network.http_connection_manager
              typed_config:
                "@type": type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
                stat_prefix: ingress_http
                route_config:
                  name: local_route
                  virtual_hosts:
                    - name: jetty_service
                      domains: ["*"]
                      routes:
                        - match:
                            prefix: "/"
                          route:
                            cluster: jetty_backend
                http_filters:
                  - name: envoy.filters.http.router

  clusters:
    - name: jetty_backend
      connect_timeout: 5s
      type: logical_dns
      lb_policy: round_robin
      load_assignment:
        cluster_name: jetty_backend
        endpoints:
          - lb_endpoints:
              - endpoint:
                  address:
                    socket_address:
                      address: 127.0.0.1
                      port_value: 8080
```

**What this shows**

* Envoy terminates TLS
* Envoy forwards plain HTTP to Jetty
* No retries, no app logic, no protocol translation

---

## 2. Envoy – Rate Limiting (Local, Example)

```yaml
http_filters:
  - name: envoy.filters.http.local_ratelimit
    typed_config:
      "@type": type.googleapis.com/envoy.extensions.filters.http.local_ratelimit.v3.LocalRateLimit
      stat_prefix: local_rate_limiter
      token_bucket:
        max_tokens: 100
        tokens_per_fill: 100
        fill_interval: 1s
```

**Key point**
This replaces *any* rate limiting logic in Jetty filters.

---

## 3. Jetty – Plain HTTP, No TLS

Jetty is intentionally boring.

```java
Server server = new Server();

ServerConnector connector = new ServerConnector(server);
connector.setPort(8080);
server.addConnector(connector);

ServletContextHandler context = new ServletContextHandler();
context.setContextPath("/");
context.addServlet(RequestHandlerServlet.class, "/*");

server.setHandler(context);
server.start();
```

**Deliberate omissions**

* No TLS
* No HTTP/2 config
* No retries
* No rate limiting

Jetty is an execution host, not an edge proxy.

---

## 4. Request Handler → Runtime Session (Singleton)

```java
public final class RuntimeSession {

    private static final RuntimeSession INSTANCE = new RuntimeSession();

    private final PipelineRegistry registry;
    private final ExecutorService workers;

    private RuntimeSession() {
        this.registry = new PipelineRegistry();
        this.workers = Executors.newVirtualThreadPerTaskExecutor();
    }

    public static RuntimeSession get() {
        return INSTANCE;
    }

    public TaskResult submit(Task task) {
        return registry.dispatch(task, workers);
    }
}
```

**Architectural signal**

* Singleton
* Long-lived
* Entry point to backend execution
* Independent of Envoy lifecycle

---

## 5. Request Handler Using the Session

```java
public class RequestHandlerServlet extends HttpServlet {

    private final RuntimeSession session = RuntimeSession.get();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        Task task = Task.fromHttp(req);
        session.submit(task);

        resp.setStatus(HttpServletResponse.SC_ACCEPTED);
    }
}
```

Envoy may retry **before** this point.
Jetty never retries **after** this point.

---

## 6. Tracing Header Pass-Through (Optional but Realistic)

Envoy injects:

```
x-request-id
traceparent
x-envoy-attempt-count
```

Jetty just propagates:

```java
String requestId = req.getHeader("x-request-id");
context.put("requestId", requestId);
```

No tracing SDK required to start.

---

## One-Line Architectural Rule (Encoded in Code)

* Envoy: **how traffic arrives**
* Jetty: **what the traffic means**
* Session: **how work lives and completes**