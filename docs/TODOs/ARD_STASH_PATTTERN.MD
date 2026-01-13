# Exploration: Admission Control with Priority-Aware Stash Pattern

## Problem Statement

The system processes client requests through an asynchronous pipeline:

Client → Transport → ProtocolAdapter → RequestHandler → Dispatcher → Session → Pipeline → Executor

During peak load or initialization phases, the Runtime Session may:
- Be temporarily not ready to execute new requests
- Be overloaded due to concurrency, memory, or downstream saturation

Additionally, the platform offers **tiered user plans** (e.g. Plus vs Standard), requiring:
- Preferential treatment for higher-tier users during overload
- Controlled degradation for lower-tier users

Dropping requests is undesirable.
Blocking client threads is unacceptable.
Backpressure must be explicit and observable.

## Proposed Approach

Consider introducing an **Admission Control layer** inside the `RequestHandler`, implementing a
**Priority-Aware Stash Pattern**.

The RequestHandler will:
- Query the Runtime Session for readiness and capacity signals
- Decide whether to ACCEPT, DEFER (stash), or REJECT a request
- Temporarily stash only *lightweight request envelopes*
- Drain stashed requests in priority order when capacity becomes available

This is a **bounded, short-lived, in-memory stash**, not a durable queue.

## Key Concepts

- Stash is treated as a **control-flow pattern**, not a framework object
- Priority is enforced **before dispatch**, never inside the execution pipeline
- Runtime Session provides signals, not policy
- RequestHandler owns admission, prioritization, and buffering

## RequestEnvelope (Lightweight)

Only metadata required for admission and scheduling is stored.

```java
public final class RequestEnvelope {

    public enum Plan {
        PLUS, STANDARD, FREE
    }

    private final String requestId;
    private final Plan plan;
    private final int estimatedCost; // abstract cost units
    private final long enqueueTime;
    private final long ttlMillis;

    public boolean isExpired(long now) {
        return now - enqueueTime > ttlMillis;
    }

    // getters omitted
}
````

## Runtime Session Contract

The Runtime Session exposes coarse-grained signals only.

```java
public interface RuntimeSession {

    enum AdmissionSignal {
        READY,
        OVERLOADED,
        NOT_READY
    }

    AdmissionSignal admissionSignal();

    int availableCapacity(); // abstract units
}
```

No internal queues or executor details are leaked.

## Admission Decision

```java
public enum AdmissionDecision {
    ACCEPT,
    DEFER,
    REJECT
}
```

Decision logic is centralized in the RequestHandler.

## RequestHandler Structure

```java
public class RequestHandler {

    private final RuntimeSession session;

    private final PriorityQueue<RequestEnvelope> stash =
        new PriorityQueue<>(Comparator
            .comparing(RequestEnvelope::getPlan)   // PLUS first
            .thenComparing(RequestEnvelope::getEnqueueTime));

    private final int maxStashSize = 10_000;

    public AdmissionDecision handle(RequestEnvelope env) {
        if (env.isExpired(System.currentTimeMillis())) {
            return AdmissionDecision.REJECT;
        }

        switch (session.admissionSignal()) {
            case READY:
                dispatch(env);
                return AdmissionDecision.ACCEPT;

            case OVERLOADED:
            case NOT_READY:
                return deferOrReject(env);
        }

        return AdmissionDecision.REJECT;
    }

    private AdmissionDecision deferOrReject(RequestEnvelope env) {
        if (stash.size() >= maxStashSize && env.getPlan() != RequestEnvelope.Plan.PLUS) {
            return AdmissionDecision.REJECT;
        }
        stash.offer(env);
        return AdmissionDecision.DEFER;
    }

    public void drainIfPossible() {
        while (!stash.isEmpty() && session.admissionSignal() == RuntimeSession.AdmissionSignal.READY) {
            RequestEnvelope env = stash.poll();
            if (!env.isExpired(System.currentTimeMillis())) {
                dispatch(env);
            }
        }
    }

    private void dispatch(RequestEnvelope env) {
        // forward to Dispatcher
    }
}
```

## Patterns Used

### 1. Stash Pattern

Temporarily buffer requests that cannot yet be processed.

### 2. Admission Control

Explicit accept / defer / reject decision at the system boundary.

### 3. Priority Queue

Deterministic preferential handling for higher-tier users.

### 4. State-Based Processing

Behavior changes based on Runtime Session state (READY / OVERLOADED).

### 5. Temporal Decoupling

Client acknowledgment is decoupled from actual execution.

## Consequences

### Positive

* Prevents overload propagation
* Enables tier-based prioritization
* Avoids thread blocking
* Centralizes backpressure logic
* Improves system predictability

### Negative

* Requires careful tuning of stash limits
* Risk of starvation without TTL enforcement
* Additional complexity in RequestHandler

## Operational Considerations

* Stash size must be monitored
* TTL expirations must be logged and traced
* Metrics required:

    * stash.size
    * rejected.count
    * deferred.count
    * drain.rate

## Alternatives Considered

### 1. Blocking at RequestHandler

Rejected due to thread starvation risk.

### 2. Let Executor Reject

Rejected due to late failure and wasted work.

### 3. External Queue (Kafka, MQ)

Rejected for short-term overload handling due to latency and complexity.

## Notes

This design intentionally separates:

* **Policy (RequestHandler)**
* **Execution (Runtime Session / Pipeline)**

The stash is not a substitute for proper capacity planning or backpressure mechanisms.

## Open Questions

* How to define cost estimation strategy?
* What starvation guarantees are needed?
* What HTTP-level semantics should be used (202 vs 429 vs 503)?

## Further Investigation Needed

- Performance testing under various load conditions
- Tuning stash size limits for different workloads
- Integration with existing RequestHandler architecture
- Metrics and observability requirements
