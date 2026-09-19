# Adding ODap Protocol Support in Cheshire

## 1. Purpose

ODap (Observability Data Access Protocol) is a **protocol-first integration pattern** designed for **high-throughput,
observability-driven data access**. Supporting ODap allows Cheshire to:

* Stream telemetry and metrics from Capabilities/Actions
* Expose observability data via a standard interface
* Integrate seamlessly with reactive pipelines and monitoring modules
* Reuse existing pipeline stages without modifying business logic

---

## 2. Adapter Design

### ODapProtocolAdapter

* Implement a **new protocol adapter** extending `ProtocolAdapter`.
* Responsible for:

    * Translating ODap requests → `RequestEnvelope`
    * Translating `ResponseEntity` → ODap responses
* Supports **streaming and batch modes**.

```java
public class ODapProtocolAdapter implements ProtocolAdapter {
    @Override
    public RequestEnvelope toRequestEnvelope(Request req) {
        // Parse ODap request payload
    }

    @Override
    public Response fromResponseEntity(ResponseEntity entity) {
        // Map to ODap response format
    }
}
```

---

### ODapServer

* Integrate with existing **TransportServer** layer (HTTP/MCP)
* Handles **ODap connections**, optionally streaming via SSE or WebSocket
* Non-blocking/reactive (use Reactor `Flux`/`Mono` for pipeline execution)

---

## 3. Path & Base Design

ODap follows the **existing capability-based path structure**:

```
/{capability}/odap/{version}/{action}
```

Example:

```
GET /chinook/odap/v1/metrics
```

* **/{capability}** – Reuse existing capability model
* **odap** – Protocol pattern identifier
* **/{version}** – API versioning
* **/{action}** – Action or metric to fetch (optional)

---

## 4. Pipeline Integration

ODap requests flow through **the same PipelineExecutor**:

1. PreProcessors → validation, transformation
2. Executor → query/aggregate observability data
3. PostProcessors → format response for ODap
4. Reactive streams (`Flux`/`Mono`) for real-time ODap data

```java
Flux<StepOutput> odapFlux = Flux.fromIterable(stepInputs)
    .flatMap(input -> preProcessors.apply(input, ctx))
    .flatMap(input -> executor.executeQuery(input, ctx))
    .flatMap(result -> postProcessors.apply(result, ctx))
    .map(stepOutput -> ODapResponse.from(stepOutput));
```

---

## 5. Security Integration

* ODap requests leverage **existing SecurityContext**.
* Authorization applied at **Capability/Action level**.
* Supports token-based auth or Cedar Policy checks like REST/MCP.

```java
SecurityContext.withPrincipal(principal, () -> {
    odapAdapter.handle(request);
});
```

---

## 6. SPI Support

* ODap adapters are **pluggable modules**.
* Load dynamically via `ServiceLoader<ProtocolAdapter>`.
* Can register **monitoring or observability plugins** to Reactor streams.

```java
ServiceLoader<ProtocolAdapter> adapters = ServiceLoader.load(ProtocolAdapter.class);
adapters.forEach(adapter -> transport.register(adapter));
```

---

## 7. Benefits

* **Protocol-Agnostic Architecture** – No change to business logic or capabilities
* **Reactive & Streaming Ready** – Integrates with Reactor pipelines
* **Security & Multi-Tenancy** – Reuses existing RBAC/ABAC
* **Pluggable & Extensible** – Load dynamically via SPI
* **Unified Observability** – Expose metrics/events alongside standard actions

---

## 8. Implementation Steps

1. Implement `ODapProtocolAdapter` extending `ProtocolAdapter`.
2. Add ODap support in `TransportServer` for HTTP/SSE/WebSocket.
3. Map ODap requests → `RequestEnvelope` and responses → ODap format.
4. Integrate reactive pipelines (`Mono`/`Flux`) for streaming observability data.
5. Ensure **SecurityContext propagation** and authorization checks.
6. Register adapter via **ServiceLoader SPI** for runtime discovery.
7. Test with existing capabilities and pipeline stages for functional correctness.
