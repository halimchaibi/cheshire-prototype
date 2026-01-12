# Chinook Application: Configuration & Bootstrap Guide

This guide explains where to place configuration files for the Chinook application, how `CheshireBootstrap` uses them, and how to run the app.

---

## 1. Configuration Files

### Recommended Structure

For clarity and separation between source, resources, and config:

```

your-project/
├─ src/main/java/...           # Application source code
├─ src/main/resources/...      # Classpath resources
└─ config/
├─ chinook-application.yaml       # Main app config (used by CheshireBootstrap)
├─ pipelines.yaml                  # Pipeline definitions per capability
├─ actions.yaml                    # Actions definitions per capability
└─ logging.yaml (optional)         # Logging config if needed

````

### Notes

- The `config/` folder can be **outside the JAR** for runtime externalization.
- For testing or default deployment, you can also place configuration inside `src/main/resources` and access via classpath.
- The `CheshireBootstrap` will resolve configuration first from the filesystem (relative to `cheshire.home`), then fall back to the classpath if not found.

---

## 2. Using CheshireBootstrap in ChinookApp 

### Load Configuration

```java
private static final String CONFIG = "chinook-application.yaml";

CheshireSession session = CheshireBootstrap.fromConfig(CONFIG).build();
````

* `CONFIG` is the **relative path to your main configuration file**.
* `fromConfig` reads the file, initializes `ConfigurationManager`, loads capabilities, pipelines, and actions.
* If `cheshire.home` system property or `CHESHIRE_HOME` environment variable is set, `fromConfig` will resolve config relative to that directory.

### Optional: Skip Auto-Start

```java
CheshireBootstrap.fromConfig(CONFIG)
                .skipSessionAutoStart(true)
                .build();
```

* Use `skipSessionAutoStart(true)` to prevent automatic starting of the session, useful for integration tests or pre-boot tasks.

---

## 3. Runtime Initialization

Once `CheshireSession` is built:

```java
CheshireRuntime runtime = CheshireRuntime.expose(session).start();
runtime.awaitTermination();
```

* `CheshireRuntime` starts all capabilities, pipelines, and query engines.
* `awaitTermination()` blocks the main thread until shutdown.
* Metrics, logging, and observers can run in separate virtual threads.

Example: metrics observer

```java
Thread.ofVirtual().name("metrics-observer").start(() -> {
    while (runtime.isRunning()) {
        Thread.sleep(Duration.ofSeconds(20));
        log.debug("Runtime Health: {}", runtime.getMetrics().getSnapshot().toJson());
    }
});
```

---

## 4. Pipeline and Actions Resolution

* `ConfigurationManager` automatically loads **pipelines.yaml** and **actions.yaml** per capability.
* Paths are **relative to the base config directory** (filesystem or classpath).
* Example:

```yaml
# chinook-application.yaml
capabilities:
  music:
    pipelinesDefinitionFile: "pipelines.yaml"
    actionsSpecificationFile: "actions.yaml"
```

* `ConfigurationManager` then loads each file via `ConfigLoader`:

    * Pipelines → `Map<String, PipelineConfig>`
    * Actions → `ActionsConfig`

---

## 5. Best Practices

1. **Externalize config for production**:

    * Set `cheshire.home` or `CHESHIRE_HOME` to a directory containing your config folder.
    * Keep `chinook-application.yaml`, `pipelines.yaml`, and `actions.yaml` there.

2. **Embed default config in resources for testing**:

    * Place a copy under `src/main/resources` or `src/test/resources`.
    * Classpath fallback ensures tests can run without filesystem config.

3. **Avoid hardcoding paths in code**:

    * Let `CheshireBootstrap` + `ConfigurationManager` resolve relative to `baseDir`.
    * Only the relative file name (`"chinook-application.yaml"`) is needed.

4. **Use logging**:

    * Avoid `System.out` calls; rely on SLF4J logging for runtime info, startup banners, and errors.

---

## 6. Full Bootstrap Flow

```
ChinookApp main()
 └─ CheshireBootstrap.fromConfig("chinook-application.yaml")
       └─ ConfigurationManager
             └─ ConfigLoader loads:
                    ├─ chinook-application.yaml → CheshireConfig
                    ├─ pipelines.yaml → PipelineConfig
                    └─ actions.yaml → ActionsConfig
 └─ LifecycleManager initializes engines, sources, capabilities
 └─ CheshireSession built
 └─ CheshireRuntime starts session → capabilities, pipelines, actions active
```

---

This setup allows:

* Flexible external configuration
* Safe defaults via classpath
* Automatic loading of pipelines/actions per capability
* Clear separation between bootstrap, configuration, and runtime

