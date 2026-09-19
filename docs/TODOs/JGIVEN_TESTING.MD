# **TODO reference document** to integrate **JGiven

** into Cheshire framework for behavior-driven testing and structured test documentation.

Kept modular so developers can pick tasks easily.

---

# Cheshire Framework – JGiven Integration TODO Reference

## 1. Objective

Leverage [JGiven](https://jgiven.org/) to:

* Enable **behavior-driven development (BDD)** style testing.
* Provide **structured test scenarios** that map to capabilities, actions, pipelines, and query engine logic.
* Generate **rich HTML / Markdown / PDF reports** for QA, monitoring, and auditing.
* Support **capability-level, action-level, and end-to-end testing**.

---

## 2. Project Setup

### 2.1 Add JGiven Dependencies

**TODO:**

* Add JGiven to Cheshire build system (Maven / Gradle).

**Maven Example:**

```xml
<dependency>
    <groupId>com.tngtech.jgiven</groupId>
    <artifactId>jgiven-core</artifactId>
    <version>1.3.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.tngtech.jgiven</groupId>
    <artifactId>jgiven-junit5</artifactId>
    <version>1.3.3</version>
    <scope>test</scope>
</dependency>
```

**Gradle Example:**

```gradle
testImplementation "com.tngtech.jgiven:jgiven-core:1.3.3"
testImplementation "com.tngtech.jgiven:jgiven-junit5:1.3.3"
```

### 2.2 Configure Output Reports

**TODO:**

* Set up JGiven to generate HTML and Markdown reports in `target/reports/jgiven/` (or `build/reports/jgiven`).
* Optional: integrate with CI/CD (GitHub Actions, Jenkins).

---

## 3. Test Structure

### 3.1 Define Stage Classes

**TODO:**

* Create three stage types aligned with Cheshire concepts:

1. **Given Stage** – Setup preconditions:

    * Capability exists
    * Actions are registered
    * PipelineExecutor / QueryEngine is initialized
    * Data sources are populated

2. **When Stage** – Perform action:

    * Execute capability action
    * Send requests via MCP, REST, or GraphQL adapters
    * Trigger Scheduler / RuntimeSession if needed

3. **Then Stage** – Assert outcomes:

    * Response correctness
    * PipelineExecutor output
    * QueryEngine results
    * Scheduler notifications
    * Security context validation

---

### 3.2 Example Stage Skeleton

```java
public class GivenCapabilityStage extends Stage<GivenCapabilityStage> {
    public GivenCapabilityStage a_registered_capability(String capabilityName) {
        // Setup capability in runtime
        return self();
    }
}

public class WhenActionStage extends Stage<WhenActionStage> {
    public WhenActionStage the_action_is_executed(String actionName) {
        // Execute RuntimeSession.apply or Adapter call
        return self();
    }
}

public class ThenOutcomeStage extends Stage<ThenOutcomeStage> {
    public ThenOutcomeStage the_response_contains(String expected) {
        // Assert response / PipelineExecutor output
        return self();
    }
}
```

---

### 3.3 Scenario Classes

**TODO:**

* Create test scenario classes that compose stages:

```java
@JGivenTest
public class CapabilityActionTest extends ScenarioTest<GivenCapabilityStage, WhenActionStage, ThenOutcomeStage> {
    @Test
    public void execute_artist_query() {
        given().a_registered_capability("chinook");
        when().the_action_is_executed("list_artists");
        then().the_response_contains("Aerosmith");
    }
}
```

---

## 4. Integration with Cheshire Components

### 4.1 PipelineExecutor Testing

**TODO:**

* Mock SourceProviders or use test data.
* Execute PreProcessors, Executor, PostProcessors.
* Assert output matches expected canonical output.

### 4.2 QueryEngine Testing

**TODO:**

* Test multi-source query federation.
* Validate parallel fetch and result merging.
* Test DAG / complex action orchestration logic.

### 4.3 RuntimeSession Testing

**TODO:**

* Validate session lifecycle (start, execute, finish).
* Test integration with Scheduler / gRPC-invoked executors.
* Assert thread-local SecurityContext propagation.

### 4.4 Adapter Testing

**TODO:**

* Test MCP / REST / GraphQL adapters.
* Validate protocol transformation (`RequestEnvelope` ↔ `ResponseEntity`).
* Test error handling and authorization failures.

---

## 5. Scheduler Testing

**TODO:**

* Test both pull- and push-based scheduling modes.
* Validate multi-format outputs: PDF, CSV, HTML.
* Validate delivery channels: FTP, S3, Kafka.
* Test notification URLs for completed jobs.

---

## 6. Security Testing

**TODO:**

* Use stages to setup authentication tokens and roles.
* Validate RBAC / ABAC policies using test principals.
* Test authorization failures and edge cases.

---

## 7. Observability

**TODO:**

* Integrate JGiven reports with Cheshire dashboard for:

    * Test scenario execution overview
    * Action-level execution success/failure
    * PipelineExecutor / QueryEngine metrics
* Optional: hook into Reactor for runtime monitoring of BDD execution.

---

## 8. Advanced

### 8.1 Dynamic Stage Generation

**TODO:**

* Generate Given/When/Then stages dynamically based on capabilities and actions.
* Enable scenario discovery from metadata or JSON/YAML definitions.

### 8.2 CI/CD Integration

**TODO:**

* Generate JGiven HTML/Markdown reports during CI builds.
* Attach reports as artifacts.
* Optionally fail builds for failed critical scenarios.

---
