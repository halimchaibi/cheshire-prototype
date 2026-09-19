# Using Reactor in Cheshire for Observability & Monitoring

## 1. Purpose

Reactor provides a **reactive, non-blocking programming model** in Cheshire for:

* Monitoring **pipeline execution** (pre/post-processing, executor)
* Observing **Capabilities and Actions** in real-time
* Collecting **metrics, traces, and logs** for observability
* Supporting **async/reactive streams** (HTTP, MCP, SSE)

---

## 2. Where to Integrate

### Pipeline Integration

Wrap existing **PipelineExecutor** stages in `Mono` or `Flux` to:

* PreProcessors → Flux<StepInput>
* Executor → Mono<QueryResult>
* PostProcessors → Flux<StepOutput>

```java
Flux.fromIterable(stepInputs)
    .flatMap(input -> preProcessors.apply(input, ctx))
    .flatMap(input -> executor.executeQuery(input, ctx))
    .flatMap(result -> postProcessors.apply(result, ctx))
    .doOnNext(stepOutput -> monitor.publish(stepOutput))
    .subscribe();
```

### RuntimeSession

Use Reactor to handle **SessionTasks** asynchronously:

```java
Mono<TaskResult> resultMono = Mono.fromCallable(() ->
    pipeline.apply(canonicalInput, ctx)
).doOnSuccess(taskResult -> monitor.publish(taskResult))
 .subscribeOn(Schedulers.boundedElastic());
```

### Dispatcher / RequestHandler

Wrap **dispatch and handling logic** in `Mono` for reactive responses:

```java
Mono<ResponseEntity> response = Mono.fromCallable(() ->
    dispatcher.dispatch(requestEnvelope, ctx)
).doOnNext(resp -> monitor.logResponse(resp));
```

---

## 3. Observability & Monitoring Patterns

### Metrics

* Record **execution time**, success/failure counts
* Track **pipeline stage durations**
* Integrate with Micrometer or Prometheus

```java
monitor.timer("pipeline.executor.duration")
       .record(() -> executor.executeQuery(input, ctx));
```

### Logging

* Use `doOnNext`, `doOnError`, and `doFinally` hooks
* Log **principal, capability, action, resource, decision**

```java
flux.doOnNext(step -> log.info("Processed step: {}", step))
    .doOnError(err -> log.error("Pipeline error", err));
```

### Tracing

* Generate **trace/span per pipeline stage**
* Propagate **SecurityContext** in reactive streams
* Combine with OpenTelemetry if needed

```java
Mono.deferContextual(ctx -> {
    Principal principal = ctx.get(SecurityContext.KEY);
    return executor.executeQuery(input, ctx);
});
```

### Async Context Propagation

* Use `Context` to carry **SecurityContext** and observability metadata
* Ensure thread-local values are accessible across reactive threads

```java
Mono.just(input)
    .contextWrite(Context.of(SecurityContext.KEY, principal))
    .flatMap(i -> executor.executeQuery(i, ctx));
```

---

## 4. SPI-Based Observability

* Implement **pluggable monitoring modules** using SPI
* Modules can subscribe to Reactor streams to collect metrics, logs, traces
* Dynamically load at runtime using `ServiceLoader`

```java
ServiceLoader<MonitorPlugin> plugins = ServiceLoader.load(MonitorPlugin.class);
plugins.forEach(plugin -> flux.doOnNext(plugin::observe));
```

---

## 5. Benefits

* **Non-blocking pipelines** → higher throughput for MCP/HTTP/Streaming
* **Real-time monitoring** of Capabilities and Actions
* **Centralized observability** across protocol adapters
* **Extensible** via SPI modules for custom metrics/logging/tracing

---

## 6. Implementation Steps

1. Wrap **PipelineExecutor** and session execution in `Mono`/`Flux`.
2. Add **doOnNext/doOnError/doFinally hooks** for logging, metrics, and auditing.
3. Propagate **SecurityContext** using Reactor’s `Context`.
4. Expose **pipeline metrics and traces** to Micrometer/Prometheus/OpenTelemetry.
5. Implement **SPI monitoring modules** and attach to reactive streams.
6. Test with **real-time workloads** (SSE, streaming, MCP) to ensure async propagation.

