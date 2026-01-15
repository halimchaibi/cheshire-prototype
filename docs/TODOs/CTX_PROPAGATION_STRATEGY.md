# Context Propagation Strategy for REST / MCP Framework

## 1. Purpose

This document explores an approach for **request context propagation** across the framework, from ingress (Jetty / stdio) to data sources and back to response assembly.

The goal is to:

* Avoid ThreadLocal and ad‑hoc context passing
* Support REST and MCP uniformly
* Be compatible with virtual threads and structured concurrency
* Keep business and pipeline APIs clean
* Support logging, tracing, and metrics without leaking abstractions

---

## 2. Problem Statement

The framework processes requests through multiple layers:

* Transport (Jetty / stdio)
* Protocol adapters (REST / MCP)
* Dispatcher and capability routing
* Three‑stage pipelines (Pre / Exec / Post)
* Data access (JDBC / Calcite / APIs)

Across this flow, several cross‑cutting concerns require shared context:

* Request identification (requestId, correlationId)
* Protocol information (REST vs MCP)
* Authentication / principal
* Tenant or namespace
* Deadlines / timeouts
* Observability (logging, tracing, metrics)

Constraints:

* Context must not leak across requests
* Async and parallel execution must be safe
* Context should not pollute method signatures
* REST and MCP must share the same internal model

---

## 3. Context Taxonomy

Not all “context” is the same. Mixing these leads to fragile designs.

### 3.1 Execution Context (In‑Process)

Scope:

* JVM‑local
* Valid for the lifetime of a request
* Visible across synchronous calls and child threads

Examples:

* requestId
* protocol
* principal
* capability / action name
* deadline

**This is the primary target of this design.**

---

### 3.2 Transport Context (Cross‑Process)

Scope:

* HTTP headers
* MCP envelopes
* Message payloads

Characteristics:

* Must be explicitly serialized
* Must survive process boundaries

Examples:

* traceparent header
* auth tokens
* MCP tool metadata

This context is *input* to execution context, not a replacement for it.

---

### 3.3 Observability Context

Scope:

* Logging
* Tracing
* Metrics

Examples:

* MDC
* OpenTelemetry spans
* Metric tags

This context is *derived* from execution context.

---

## 4. Proposed Approach: Scoped Values

### 4.1 Why Scoped Values

Scoped Values (JEP 506, Java 25):

* Immutable
* Lexically scoped
* Automatically propagated to child threads
* Designed for structured concurrency
* No cleanup or leak risks

They solve the exact class of problems caused by ThreadLocal and MDC misuse.

---

### 4.2 Authoritative RequestContext

Define a single immutable context object:

```java
record RequestContext(
    String requestId,
    Protocol protocol,   // REST | MCP
    Principal principal,
    Instant deadline
) {}
```

This object:

* Is created once
* Never mutated
* Is the single source of truth

---

### 4.3 ScopedValue Definition

```java
public final class Contexts {
    public static final ScopedValue<RequestContext> REQUEST =
        ScopedValue.newInstance();
}
```

---

## 5. Context Lifecycle

### 5.1 Ingress (Jetty / stdio)

At the earliest possible point:

1. Extract protocol‑specific metadata
2. Normalize into RequestContext
3. Bind the context

```java
RequestContext ctx = RequestContextFactory.from(rawRequest);

ScopedValue.where(Contexts.REQUEST, ctx)
           .run(() -> server.handle(rawRequest));
```

This is the **only binding site**.

---

### 5.2 Downstream Consumption

All layers read context implicitly:

* Adapter
* Dispatcher
* Capability
* Pipeline
* Pre / Exec / Post
* Query engine
* Source provider

```java
RequestContext ctx = Contexts.REQUEST.get();
```

No method signature pollution.

---

### 5.3 Parallelism and Fan‑Out

When parallel execution is required:

```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var a = scope.fork(() -> queryA());
    var b = scope.fork(() -> queryB());
    scope.join();
}
```

The RequestContext is automatically available in all child threads.

---

## 6. REST and MCP Unification

Protocol adapters:

* REST Adapter
* MCP Adapter

Responsibilities:

* Parse protocol‑specific inputs
* Extract headers / metadata
* Create RequestContext

From Dispatcher downward:

* No protocol branching
* No knowledge of REST vs MCP

This enforces a clean separation between transport and execution.

---

## 7. Logging and Observability

### 7.1 MDC as a Projection

MDC is still required by logging frameworks, but:

* MDC is **not authoritative**
* MDC is populated from RequestContext
* MDC is cleared deterministically

Pattern:

```java
try (var ignored = MdcBridge.installFrom(Contexts.REQUEST)) {
    log.info("Executing capability");
}
```

---

### 7.2 Tracing and Metrics

* Tracing spans are created from RequestContext
* Metrics tags are derived from RequestContext

No component reads MDC to make decisions.

---

## 8. Error Handling

Errors at any stage:

* Context remains intact
* Error mappers read RequestContext
* Response adapters format protocol‑specific errors

No special cleanup logic required.

---

## 9. Testing Strategy

Tests can bind context explicitly:

```java
ScopedValue.where(Contexts.REQUEST, testContext)
           .run(() -> capability.execute(task));
```

Benefits:

* Deterministic tests
* No mocking of ThreadLocal
* No hidden state

---

## 10. Non‑Goals and Explicit Exclusions

This approach does NOT attempt to:

* Propagate context across Kafka, queues, or persistence
* Replace transport headers
* Replace OpenTelemetry
* Store mutable or accumulative state

Those concerns remain explicit by design.

---

## 11. Risks and Mitigations

### Risk: Ecosystem still MDC‑centric

Mitigation:

* Centralized MDC bridge
* Strict rule: MDC is read‑only for logging

### Risk: Accidental context reads outside scope

Mitigation:

* Fail fast when Contexts.REQUEST is not bound

---

## 12. Conclusion

Using Scoped Values as the execution‑context backbone provides:

* Strong isolation
* Clean APIs
* Compatibility with virtual threads
* Unified REST and MCP handling
* Predictable observability integration

This approach is modern, explicit where needed, implicit where safe, and avoids the historical pitfalls of ThreadLocal‑based designs.
