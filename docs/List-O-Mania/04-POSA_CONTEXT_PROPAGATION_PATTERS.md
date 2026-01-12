# Exploration: Context Object for Implicit Request Context Propagation

## Related POSA Patterns

- Context Object (POSA Vol. 2 – Patterns for Concurrent and Networked Objects)
- Execution Context
- Request Context
- Thread-Specific Storage (implementation technique)

## Problem Statement

The system processes requests through a layered call chain:

RequestHandler → QueryEngine → Pipeline → Executor → Providers

Multiple components need access to **request-scoped metadata**, such as:

- request identifiers
- user identifiers
- execution hints
- diagnostics and tracing data
- load, cost, or size estimates

This information:

- Is cross-cutting
- Evolves during request processing
- Must be available without polluting method signatures
- Must work across synchronous and asynchronous execution paths

The system is **not based on an ActorSystem or message-passing model**.  
Calls are direct (method calls, futures, executors).

---

## Problem

How can request-scoped information be:

- Shared across multiple layers
- Enriched incrementally by downstream components
- Without forcing every method to return an updated context
- While remaining concurrency-safe and operationally practical

Passing context explicitly through all APIs:

- Pollutes interfaces
- Couples unrelated components
- Reduces readability and evolvability

Strict immutability with rebinding:

- Requires explicit propagation everywhere
- Introduces boilerplate
- Becomes fragile across async boundaries

Global mutable state is unacceptable due to concurrency and isolation concerns.

---

## Proposed Approach

Consider adopting the **Context Object pattern** as described in POSA.

Introduce a **request-scoped Context Object** that:

- Encapsulates immutable identity fields
- Provides a controlled, mutable metadata store (“bag”) for contextual enrichment
- Is shared by reference across the request lifetime
- Is explicitly scoped to a single request

Downstream components may **add metadata** to the context without returning it.

---

## Design

### Context Object

```java
public record Context(
    String requestId,
    String userId,
    ConcurrentHashMap<ContextKey<?>, Object> bag
) {
    public Context(String requestId, String userId) {
        this(requestId, userId, new ConcurrentHashMap<>());
    }

    public <T> void put(ContextKey<T> key, T value) {
        bag.putIfAbsent(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(ContextKey<T> key) {
        return (T) bag.get(key);
    }
}
````

### Typed Keys (to preserve type safety)

```java
public final class ContextKey<T> {
    private final String name;

    private ContextKey(String name) {
        this.name = name;
    }

    public static <T> ContextKey<T> of(String name) {
        return new ContextKey<>(name);
    }

    public String name() {
        return name;
    }
}
```

---

## Scope Management

The Context Object is:

* Created by the RequestHandler
* Owned by the request scope
* Passed by reference to downstream components

Example:

```java
Context ctx = new Context(requestId, userId);
queryEngine.execute(query, ctx);
```

No global singleton context is used.

---

## Mutation Rules (POSA-aligned constraints)

To avoid uncontrolled shared state, the following rules apply:

1. **Metadata only**

    * Context is not used for business logic or decision-making
    * Only diagnostics, tracing, hints, or execution metadata are allowed

2. **Write-once semantics**

    * `putIfAbsent` is preferred
    * Overwriting existing values is discouraged

3. **Namespaced keys**

    * Keys must be component-scoped (e.g. `qe.querySize`, `pipeline.stage`)

4. **Request lifetime**

    * Context must not escape the request boundary
    * No caching or reuse across requests

---

## Consequences

### Positive

* Clean APIs without context plumbing
* Simple enrichment model
* Async-safe by default
* Aligns with real-world systems (ServletRequest, Netty attributes, MDC)
* POSA-endorsed pragmatic solution

### Negative

* Introduces shared mutable state
* Hidden dependencies between components
* Requires discipline to avoid misuse
* Harder to reason about than pure immutability

These risks are accepted as a trade-off for ergonomics and operational simplicity.

---

## Alternatives Considered

### Immutable Context with Rebinding

Rejected due to verbosity and async propagation complexity.

### ThreadLocal / Ambient Context

Rejected as primary mechanism due to propagation fragility and debugging cost.

### Returning Updated Context

Rejected due to API pollution and tight coupling.

---

## Rationale (POSA Perspective)

POSA explicitly acknowledges that:

* Context information evolves during execution
* Pragmatic systems require controlled mutation
* The Context Object pattern is intended to balance modularity with practicality

This decision prioritizes:

* Architectural clarity
* Operational reliability
* Developer ergonomics

over theoretical purity.

---

## Notes

This Context Object is **not**:

* A domain model
* A command carrier
* A global state container

It is a **cross-cutting infrastructure concern**, aligned with POSA guidance.


```
