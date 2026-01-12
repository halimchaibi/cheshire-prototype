# Exploration: Handling Large / Nested Data in the Canonical Pipeline

## Problem Statement

The current **Canonical Pipeline** flow:

```
Client → TransportServer → ProtocolAdapter → RequestHandler → Dispatcher → RuntimeSession → PipelineExecutor → QE / Source Providers → PostProcessors → RuntimeSession → Dispatcher → RequestHandler → TransportServer → Client
```

* `CanonicalInput` / `PipelineInput` carries request data.
* `PipelineOutput` / `CanonicalOutput` carries processed results.
* Currently, `Map<String, Object>` is used for both input and output data.
* Functional methods (`withData`, `copy`) assume **fully materialized in-memory objects**.

**Problem:**

* Large datasets (e.g., millions of rows) or large blobs (images, audio) cannot be safely stored in memory inside
  `PipelineOutput`.
* Deeply nested structures require repeated type-checking and defensive copying.
* Pre/PostProcessors expect full materialization.

**Goal:**

* Enable pipeline to handle large objects, nested structures, or byte arrays without breaking immutability or the
  canonical architecture.
* Maintain functional, copy-on-write behavior where feasible.
* Support lazy or streaming access for large data.

---

## 2. Options Considered

| Option                                        | Pros                                                          | Cons                                                | Fit with current pipeline                                                       |
|-----------------------------------------------|---------------------------------------------------------------|-----------------------------------------------------|---------------------------------------------------------------------------------|
| **Direct Map Storage (Current)**              | Simple, compatible with functional methods                    | Memory-heavy for large objects, deep nesting costly | ✅ Works for small objects / lists                                               |
| **Lazy Wrappers (Supplier / LazyValue)**      | Can store streams / large blobs, avoids eager materialization | Processors must handle lazy values                  | ⚡ Requires changes in Pre/PostProcessors                                        |
| **External References (File, S3, DB Cursor)** | Keeps canonical maps small, safe for very large datasets      | Need extra I/O layer, serialization/deserialization | ⚡ Can coexist with canonical pipeline; functional updates must carry references |
| **Immutable Nested Objects / POJOs**          | Strong typing, avoids repeated casting                        | More boilerplate                                    | ✅ Fits for structured nested data                                               |

**Decision:** Use **lazy wrapper approach** combined with **external references** for very large objects. Maintain
canonical maps for small/moderate data.

---

## 3. Implementation Logic (Plumbing Perspective)

**Overview:** The flow from request to response is preserved. Changes affect mainly `PipelineOutput` and processors.

### a. TransportServer

```java
service(HttpServletRequest req, HttpServletResponse resp) {
    RequestEnvelope envelope = adapter.toRequestEnvelope(req);
    ResponseEntity response = handler.handle(envelope);
    writeResponse(resp, adapter.fromProcessingResult(response));
}
```

* Handles HTTP transport and protocol-agnostic request forwarding.

---

### b. ProtocolAdapter

```java
RequestEnvelope toRequestEnvelope(Request request) {
    String capability = identifyCapability(request);
    String action = identifyAction(request);
    Object payload = extractPayload(request);
    Object parameters = extractParameters(request);
    return RequestEnvelope.of(capability, action, payload, parameters);
}
```

* Converts protocol-specific requests into canonical request envelopes.

---

### c. RequestHandler

```java
ResponseEntity handle(RequestEnvelope request) {
    RequestContext ctx = buildRequestContext(request);
    return dispatcher.apply(request, ctx);
}
```

* Builds context and dispatches to `Dispatcher`.

---

### d. Dispatcher

```java
ResponseEntity dispatch(RequestEnvelope envelope, RequestContext ctx) {
    SessionTask task = TaskBuilder.from(envelope).build();
    TaskResult result = session.execute(task, ctx);
    return toResponseEntity(result);
}
```

* Builds `SessionTask` and forwards to `RuntimeSession`.

---

### e. RuntimeSession

```java
TaskResult execute(SessionTask task, RequestContext ctx) {
    CanonicalInput input = buildCanonicalInput(task);
    CanonicalOutput output = pipeline.apply(input, ctx);
    return TaskResult.from(output);
}
```

* Builds `CanonicalInput` and executes the pipeline.

---

### f. PipelineExecutor

```java
CanonicalOutput apply(CanonicalInput input, ExecutionContext ctx) {
    // 1. PreProcessors
    CanonicalInput preprocessedInput = preProcessors.apply(input, ctx);

    // 2. Executor
    QueryRequest queryRequest = executor.buildQueryRequest(preprocessedInput);
    QueryResult rawResult = executor.executeQuery(queryRequest, ctx);
    CanonicalOutput stepOutput = executor.mapQueryResult(rawResult, ctx);

    // 3. PostProcessors
    CanonicalOutput processedOutput = postProcessors.apply(stepOutput, ctx);

    return processedOutput;
}
```

* Handles the core processing pipeline.
* Executor can optionally return **streams or lazy values**.

---

### g. Lazy / Streaming Data in PipelineOutput

**New Wrapper:**

```java
public record LazyValue<T>(Supplier<T> supplier) {
    public T get() { return supplier.get(); }
}
```

**Example usage:**

```java
// Large catalog returned lazily
PipelineOutput output = PipelineOutput.ofData(
    Map.of("catalog", new LazyValue<>(() -> fetchRowsFromDBStream()))
);
```

* Processors can consume lazily:

```java
LazyValue<Stream<Row>> catalogLazy = output.getData("catalog", LazyValue.class).orElse(null);
catalogLazy.get().forEach(row -> processRow(row));
```

* Maintains **functional immutability** because the supplier is reused rather than copying large memory objects.

---

### h. Query Engine / Executor Streaming

```java
QueryResult executeQuery(QueryRequest request, ExecutionContext ctx) {
    Stream<Row> rows = sourceProvider.executeStream(request); // Lazy, row-by-row
    return new QueryResult(rows);
}
```

* Merges streams from multiple sources using `Stream.concat` or reactive streams.
* PostProcessors can map/enrich rows without materializing the entire dataset.

---

### i. PostProcessors

* Apply transformations lazily:

```java
CanonicalOutput apply(CanonicalOutput output, ExecutionContext ctx) {
    LazyValue<Stream<Row>> lazyRows = output.getData("rows", LazyValue.class);
    Stream<Row> enriched = lazyRows.get().map(row -> enrich(row));
    return output.withData("rows", new LazyValue<>(() -> enriched));
}
```

* Keeps pipeline memory-efficient.

---

### j. Returning Output to Client

```java
Map<String,Object> response = adapter.fromProcessingResult(TaskResult.from(output));
writeResponse(resp, response);
```

* Only materialize streams when serializing to HTTP response (e.g., chunked JSON).

---

## 4. Example Data Flow

**Request:**

```json
{
  "capability": "chinook",
  "action": "artists",
  "parameters": {"top": 10}
}
```

**CanonicalInput:**

```java
Map.of(
    "capability", "chinook",
    "action", "artists",
    "params", Map.of("top", 10)
)
```

**PipelineOutput (lazy catalog):**

```java
PipelineOutput.ofData(
    Map.of(
        "topArtists", new LazyValue<>(() -> fetchTopArtistsStream(10))
    )
);
```

**Serialized Response (materialized):**

```json
{
  "success": true,
  "data": {
    "topArtists": [
        {"name": "Artist 1", "albums": 5},
        {"name": "Artist 2", "albums": 3}
    ]
  }
}
```

---

## 5. Consequences

* **Pros:**

    * Can handle large datasets and blobs efficiently.
    * Maintains canonical, protocol-agnostic design.
    * Keeps functional, immutable behavior.
    * Supports incremental adoption: small objects remain unchanged, streams/lazy values optional.

* **Cons:**

    * Pre/PostProcessors must be lazy-aware.
    * Streams are one-time consumable, must be handled carefully.
    * Serialization layer must materialize streams for transport.

