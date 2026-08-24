# Cheshire OS Observability Use Case

This sample validates the Calcite OS adapter as a Cheshire capability exposed through REST and MCP
stdio. It follows the blog sample shape: one application, shared actions and pipelines, and
separate runtime profiles per protocol.

## Run REST

```bash
mvn -pl cheshire-usecase-os-observability -am package
java --enable-preview \
  -jar cheshire-usecase-os-observability/target/cheshire-usecase-os-observability-1.0-SNAPSHOT.jar \
  --config os-rest.yaml
```

REST actions are exposed below `/api/v1/os`:

```bash
curl http://localhost:9010/api/v1/os/list_system_info
curl http://localhost:9010/api/v1/os/list_java_info
curl http://localhost:9010/api/v1/os/list_processes
```

## Run MCP Stdio

```bash
mvn -pl cheshire-usecase-os-observability -am package
java --enable-preview \
  -jar cheshire-usecase-os-observability/target/cheshire-usecase-os-observability-1.0-SNAPSHOT.jar \
  --config os-mcp-stdio.yaml
```

Available tools:

- `list_system_info`
- `list_java_info`
- `list_processes`

The sample is read-only and uses the local Calcite `os` source with no external source provider.
