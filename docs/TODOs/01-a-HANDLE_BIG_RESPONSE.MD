# Exploration: Off-Heap Streaming Payloads in the Canonical Pipeline

## Problem Statement

The Cheshire framework defines a **canonical, protocol-agnostic execution pipeline**:

* CanonicalInput / CanonicalOutput
* Immutable, functional, copy-on-write semantics
* Designed for orchestration, not data materialization

The framework must support:

* Large result sets
* Federated queries
* Streaming responses
* Minimal GC pressure
* Predictable latency

Storing large payloads (rows, blobs, deep object graphs) **on-heap** inside `CanonicalOutput` is not viable due to:

* GC pressure
* Allocation rate spikes
* Memory fragmentation
* Unbounded object graphs

---

## Proposed Approach

Consider handling **large payloads off-heap and streaming end-to-end as opaque binary data (`ByteBuffer`)**.

The canonical pipeline could:

* Not parse payload data
* Not traverse payload data
* Not materialize payload data

Instead, it could carry **references to off-heap streaming producers**, resolved only at the transport boundary.

---

## 3. Design Principles

1. **Control plane vs Data plane**

    * Canonical pipeline = control plane
    * Payload streaming = data plane

2. **Opaque payloads**

    * The pipeline does not interpret payloads
    * Payloads are binary and format-agnostic

3. **Lazy resolution**

    * Payload streams are opened only when consumed
    * Canonical structures remain small and immutable

4. **Explicit lifecycle**

    * Off-heap resources have clear ownership
    * Cleanup is deterministic

---

## 4. Off-Heap Payload Model

### 4.1 CanonicalOutput Structure

```java
PipelineOutput.ofData(
    Map.of(
        "payload", new LazyValue<Stream<ByteBuffer>>(
            () -> queryEngine.executeStreaming(query)
        ),
        "contentType", "application/arrow",
        "encoding", "binary"
    )
)
```

* `PipelineOutput` remains small
* No `byte[]`
* No `List<Row>`
* No nested object graphs

---

### 4.2 Payload Types (Non-Exhaustive)

| Payload Type      | Handling                  |
|-------------------|---------------------------|
| Large result sets | `Stream<ByteBuffer>`      |
| Binary blobs      | `Stream<ByteBuffer>`      |
| Columnar data     | Arrow / Parquet buffers   |
| JSON streaming    | Pre-encoded UTF-8 buffers |

---

## 5. Query Engine Responsibilities

The Query Engine (QE):

* Retrieves data incrementally from source providers
* Encodes rows directly into binary buffers
* Emits `ByteBuffer` chunks
* Never returns materialized domain objects for large results

### Example

```java
Stream<ByteBuffer> executeStreaming(QueryRequest request) {
    return sourceProvider.stream(query)
        .map(row -> encoder.encode(row)); // off-heap
}
```

This allows:

* Push-based streaming
* Backpressure
* Minimal memory footprint

---

## 6. PipelineExecutor Responsibilities

The PipelineExecutor:

* Treats payloads as opaque
* Passes streaming references unchanged
* May enrich metadata (headers, limits, stats)
* Must not consume payload streams

```java
CanonicalOutput output = PipelineOutput.ofData(
    Map.of(
        "payload", lazyPayload,
        "rowCount", estimate,
        "format", "arrow"
    )
);
```

---

## 7. PreProcessors and PostProcessors Constraints

### Allowed

* Modify metadata
* Add headers
* Set pagination hints
* Attach execution statistics

### Forbidden

* Reading `ByteBuffer`
* Parsing payloads
* Re-encoding binary data
* Buffer aggregation

This rule is **non-negotiable**.

---

## 8. Transport Layer Integration

### Responsibility Shift

Only the **TransportServer** consumes the payload stream.

```java
Stream<ByteBuffer> payload =
    output.getData("payload", LazyValue.class).get();

payload.forEach(buffer -> writeChunk(buffer));
```

This allows:

* Chunked HTTP responses
* Streaming gRPC
* WebSocket frames
* MCP streaming

---

## 9. Off-Heap Storage Strategies

| Strategy              | Use Case           | Notes                   |
|-----------------------|--------------------|-------------------------|
| File-backed streaming | Default            | Simple, OS-managed      |
| Memory-mapped files   | Large scans        | Fast, careful lifecycle |
| Direct ByteBuffer     | Binary protocols   | Cleaner-dependent       |
| Arrow buffers         | Analytical queries | Columnar, zero-copy     |

**v1 Recommendation:**
Start with **file-backed or streaming direct buffers**.

---

## 10. Lifecycle Management

### Ownership Rules

| Component      | Responsibility            |
|----------------|---------------------------|
| QE             | Allocate off-heap payload |
| RuntimeSession | Own lifecycle             |
| Transport      | Consume only              |
| Dispatcher     | Trigger cleanup           |

### Cleanup

```java
try {
    CanonicalOutput out = pipeline.apply(...);
    return TaskResult.from(out);
} finally {
    offHeapRegistry.cleanup(sessionId);
}
```

No GC reliance. No guessing.

---

## 11. Failure & Cancellation Semantics

* If client disconnects:

    * Transport signals cancellation
    * QE stops streaming
    * Off-heap resources released
* If pipeline fails before streaming:

    * Payload never opened
    * No cleanup required

---

## 12. Consequences

### Positive

* Constant memory usage
* Near-zero GC impact
* Scales to large datasets
* Clear architectural boundaries
* Aligns with industry-grade systems

### Negative

* Debugging binary streams is harder
* Requires discipline in processors
* Lifecycle bugs are more visible

---

## 13. Decision Summary

**We adopt off-heap, streaming payloads as a first-class mechanism in the Cheshire canonical pipeline.**

* Canonical structures remain small
* Payloads are opaque
* Streaming is the default for large data
* The JVM acts as orchestration and control plane

This decision enables:

* Large-scale queries
* Federated execution
* Predictable performance
* Future evolution toward Arrow / Flight / gRPC

---
