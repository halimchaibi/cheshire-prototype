# Cheshire Bootstrap Architecture

This document explains the responsibilities of each component in the Cheshire core system and how configuration is loaded, resolved, and used at runtime, with a focus on pipelines and actions.

---

## 1. CheshireBootstrap

**Responsibility:**

- Entry point to initialize Cheshire Core.
- Orchestrates **ConfigurationManager** and **LifecycleManager**.
- Builds a **CheshireSession**, optionally starting it.
- Provides fluent API for bootstrapping (`fromConfig`, `skipSessionAutoStart`).

**Key Points:**

- Delegates configuration loading to `ConfigurationManager`.
- Does not know details about pipelines or actions.
- Handles exception wrapping and logging during bootstrap.

---

## 2. ConfigurationManager

**Responsibility:**

- Central manager for all Cheshire configuration.
- Loads, validates, resolves references, and exposes **CheshireConfig** safely.
- Owns the **ConfigLoader**, which actually reads YAML files from filesystem or classpath.

**Key Points:**

- Accepts a `baseDir` (e.g., `cheshire.home/configFolder`) for locating configuration files.
- Loads main `cheshire.yaml`, containing top-level configuration.
- Resolves capability references to **pipelines YAML** and **actions YAML**.
- Validates required fields and references to sources and protocols.

---

## 3. ConfigLoader

**Responsibility:**

- Low-level YAML loader supporting:
    - Filesystem paths (relative to a `baseDir`)
    - Classpath fallback
- Generic: can load into a `Class<T>` or `TypeReference<T>` for maps/lists.

**Key Points:**

- BaseDir provided by `ConfigurationManager`.
- Handles path traversal security.
- The only component that directly reads files.

---

## 4. CheshireConfig

**Responsibility:**

- POJO representing:
    - Core config
    - Capabilities
    - Sources
    - Pipelines
    - Actions
- Does not contain logic; only data.
- Can be deep-copied to ensure immutability at runtime.

---

## 5. Pipelines and Actions

**Responsibility:**

- **PipelineConfig**: Defines the processing pipeline for a capability.
- **ActionsConfig**: Defines executable actions or triggers for a capability.
- Loaded per capability by `ConfigurationManager` using the paths in the capability definition.

**Resolution Flow:**

```

Bootstrap
└─ ConfigurationManager (owns ConfigLoader)
└─ Load cheshire.yaml → CheshireConfig
└─ For each capability:
├─ Load pipelines YAML → Map<String, PipelineConfig>
└─ Load actions YAML → ActionsConfig

```

---

## 6. LifecycleManager

**Responsibility:**

- Initializes runtime components using resolved configuration:
    - Source providers
    - Query engines
    - Capabilities
- Handles lifecycle (init/shutdown hooks).

**Key Point:**

- Relies on `ConfigurationManager` for validated configuration.

---

## 7. CheshireSession

**Responsibility:**

- Runtime session holding:
    - Config
    - Capabilities
    - Sources
    - Query engines
- Supports starting and stopping the runtime.
- Provides shutdown hooks.

**Key Point:**

- Built by `CheshireBootstrap` after configuration is resolved and lifecycle initialized.

---

## 8. Filesystem vs Classpath Resolution

- **Filesystem (baseDir)**: For real deployments, allows externalized configuration.
- **Classpath fallback**: For defaults inside JARs or for tests.
- **Pipeline & Action resolution**: Always relative to baseDir; classpath used only if file is missing.  
  Prevents path traversal outside baseDir.

---

## Responsibilities Summary

| Component               | Responsibility                                                    |
|-------------------------|-------------------------------------------------------------------|
| CheshireBootstrap       | Orchestrates bootstrapping, builds CheshireSession                |
| ConfigurationManager    | Load, validate, resolve, expose CheshireConfig; owns ConfigLoader |
| ConfigLoader            | Low-level YAML reader; handles filesystem & classpath             |
| CheshireConfig          | POJO: core config, capabilities, pipelines, actions               |
| PipelineConfig          | Defines processing pipeline for a capability                      |
| ActionsConfig           | Defines executable actions/triggers for a capability              |
| LifecycleManager        | Initialize runtime engines, sources, capabilities                 |
| CheshireSession         | Holds runtime state; start/stop session                           |
| Filesystem vs Classpath | Filesystem for deployment, classpath fallback for defaults/tests  |

---

