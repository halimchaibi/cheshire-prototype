# Pipeline Configuration Guide

## Table of Contents

1. [Introduction](#introduction)
2. [Pipeline Architecture](#pipeline-architecture)
3. [Pipeline Structure](#pipeline-structure)
4. [Step-by-Step Configuration](#step-by-step-configuration)
5. [DSL Reference](#dsl-reference)
6. [Complete Examples](#complete-examples)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)

---

## Introduction

### What is a Pipeline?

A **pipeline** in the Cheshire framework is a declarative configuration that defines how data flows through your application. Each pipeline represents an operation (like creating an author, listing articles, etc.) and consists of three stages:

1. **PreProcessor** (Input) - Validates and transforms incoming data
2. **Executor** (Process) - Executes the core operation (typically database queries)
3. **PostProcessor** (Output) - Formats and enriches the response

### Why Use Pipelines?

✅ **Declarative Configuration**: Define operations in YAML without writing Java code  
✅ **Type Safety**: Strong typing through `CanonicalInput` and `CanonicalOutput` interfaces  
✅ **Reusability**: Share common processors across multiple pipelines  
✅ **Maintainability**: Changes to business logic require only configuration updates  
✅ **Testability**: Easy to test each stage independently

---

## Pipeline Architecture

### Three-Stage Architecture

```
┌─────────────┐       ┌──────────┐       ┌──────────────┐       ┌─────────────┐
│   Client    │──────▶│  Input   │──────▶│   Pipeline   │──────▶│   Output    │
│  Request    │       │ Canonical│       │   Stages     │       │  Canonical  │
└─────────────┘       └──────────┘       └──────────────┘       └─────────────┘
                                                │
                                                ├─ PreProcessor(s)
                                                │  └─ Validation
                                                │  └─ Sanitization
                                                │  └─ Transformation
                                                │
                                                ├─ Executor
                                                │  └─ Database Query
                                                │  └─ Business Logic
                                                │
                                                └─ PostProcessor(s)
                                                   └─ Formatting
                                                   └─ Enrichment
                                                   └─ Response Building
```

### Core Interfaces

The Cheshire framework provides type-safe interfaces for pipeline components:

```java
// Base interface for all canonical data
public sealed interface Canonical<S extends Canonical<S>> 
    extends Iterable<Map.Entry<String, Object>> 
    permits CanonicalInput, CanonicalOutput

// Input data structure
public non-sealed interface CanonicalInput<S extends CanonicalInput<S>>

// Output data structure
public non-sealed interface CanonicalOutput<S extends CanonicalOutput<S>>

// Pipeline step interface
public sealed interface Step<I, O> 
    permits PreProcessor, Executor, PostProcessor

// PreProcessor: transforms and validates input
public non-sealed interface PreProcessor<I extends CanonicalInput<?>> 
    extends Step<I, I>

// Executor: performs the main operation
public non-sealed interface Executor<I extends CanonicalInput<?>, O extends CanonicalOutput<?>> 
    extends Step<I, O>

// PostProcessor: formats and enriches output
public non-sealed interface PostProcessor<O extends CanonicalOutput<?>> 
    extends Step<O, O>
```

---

## Pipeline Structure

### Basic YAML Structure

Every pipeline definition follows this structure:

```yaml
pipeline_name:
  uri: domain://action_name
  description: >
    Human-readable description of what this pipeline does
  
  input: io.cheshire.core.pipeline.MaterializedInput
  
  pipeline:
    preprocess:
      - name: step_name
        type: transformer
        template: |
          { /* DSL configuration */ }
        implementation: io.package.PreProcessorImpl
        description: >
          What this preprocessor does
    
    process:
      name: executor
      type: executor
      template: |
        { /* DSL configuration */ }
      implementation: io.package.ExecutorImpl
      description: >
        What this executor does
    
    postprocess:
      - name: step_name
        type: transformer
        template: |
          { /* DSL configuration */ }
        implementation: io.package.PostProcessorImpl
        description: >
          What this postprocessor does
  
  output: io.cheshire.core.pipeline.MaterializedOutput
```

### Field Reference

| Field                  | Type   | Required | Description                                      |
|------------------------|--------|----------|--------------------------------------------------|
| `pipeline_name`        | String | ✅ Yes    | Unique identifier for this pipeline              |
| `uri`                  | String | ✅ Yes    | URI pattern (e.g., `blog://authors/create`)      |
| `description`          | String | ✅ Yes    | Human-readable description                       |
| `input`                | String | ✅ Yes    | Fully qualified class name of input type         |
| `pipeline.preprocess`  | List   | ❌ No     | List of preprocessing steps (executed in order)  |
| `pipeline.process`     | Object | ✅ Yes    | Single executor step                             |
| `pipeline.postprocess` | List   | ❌ No     | List of postprocessing steps (executed in order) |
| `output`               | String | ✅ Yes    | Fully qualified class name of output type        |

### Step Configuration

Each step (PreProcessor, Executor, PostProcessor) has:

| Field            | Type   | Required | Description                                  |
|------------------|--------|----------|----------------------------------------------|
| `name`           | String | ✅ Yes    | Unique name for this step                    |
| `type`           | String | ✅ Yes    | Step type: `transformer` or `executor`       |
| `template`       | String | ✅ Yes    | DSL configuration (JSON/YAML)                |
| `implementation` | String | ✅ Yes    | Fully qualified class name of implementation |
| `description`    | String | ❌ No     | Human-readable description of the step       |

---

## Step-by-Step Configuration

### Step 1: Define the Pipeline Header

Start with basic metadata:

```yaml
create_author:
  uri: blog://authors/create
  description: >
    Create a new author record in the blog system. Authors are users who can
    write articles. Each author must have a unique username and email address.
    Returns the created author with auto-generated UUID.
  
  input: io.cheshire.core.pipeline.MaterializedInput
  output: io.cheshire.core.pipeline.MaterializedOutput
```

**Best Practices**:

- Use descriptive names in `snake_case`
- URI should follow pattern: `domain://resource/action`
- Description should explain what, why, and what is returned

---

### Step 2: Configure PreProcessor(s)

PreProcessors validate and transform input data:

```yaml
  pipeline:
    preprocess:
      - name: inputProcessor
        type: transformer
        template: |
          {
            "validation": {
              "required": ["username", "email"],
              "rules": {
                "username": {
                  "type": "string",
                  "minLength": 3,
                  "maxLength": 255,
                  "pattern": "^[a-zA-Z0-9_]+$"
                },
                "email": {
                  "type": "string",
                  "format": "email",
                  "maxLength": 255
                }
              }
            },
            "sanitization": {
              "username": "trim",
              "email": "trim,lowercase"
            }
          }
        implementation: io.blog.pipeline.BlogInputProcessor
        description: >
          Validates the username and email are provided with proper format
          and length constraints. Ensures username contains only alphanumeric
          characters and underscores. Sanitizes inputs by trimming whitespace.
```

**Common Validation Rules**:

```yaml
validation:
  required: ["field1", "field2"]  # Required fields
  rules:
    field_name:
      type: string|number|boolean|array|object
      minLength: 3               # Min string length
      maxLength: 255             # Max string length
      pattern: "^regex$"         # Regex pattern
      format: email|url|uuid     # Predefined formats
      minimum: 0                 # Min number value
      maximum: 100               # Max number value
      enum: ["value1", "value2"] # Allowed values
```

**Common Sanitization Operations**:

```yaml
sanitization:
  field_name: "trim"                    # Remove whitespace
  field_name: "trim,lowercase"          # Chain operations
  field_name: "uppercase"               # Convert to uppercase
  field_name: "escape_html"             # Escape HTML entities
  field_name: "strip_tags"              # Remove HTML tags
```

---

### Step 3: Configure Executor

The executor performs the main operation (typically database queries):

#### INSERT Operation

```yaml
    process:
      name: executor
      type: executor
      template: |
        {
          "operation": "INSERT",
          "source": { "table": "authors" },
          "columns": [
            { "field": "id", "function": "gen_random_uuid()" },
            { "field": "username", "param": "username" },
            { "field": "email", "param": "email" },
            { "field": "created_at", "function": "CURRENT_TIMESTAMP" }
          ],
          "constraints": {
            "checkDuplicates": true,
            "conflictStrategy": "error"
          },
          "returning": ["id", "username", "email", "created_at"]
        }
      implementation: io.blog.pipeline.BlogExecutor
      description: >
        Inserts a new author record with auto-generated UUID and timestamp,
        ensuring uniqueness constraints for username and email are maintained.
        Returns the complete created record.
```

#### SELECT Operation (Simple)

```yaml
    process:
      name: executor
      type: executor
      template: |
        {
          "operation": "SELECT",
          "source": { "table": "authors", "alias": "a" },
          "projection": [
            { "field": "a.id", "alias": "id" },
            { "field": "a.username", "alias": "username" },
            { "field": "a.email", "alias": "email" },
            { "field": "a.created_at", "alias": "created_at" }
          ],
          "filters": {
            "op": "AND",
            "conditions": [
              {
                "field": "a.id",
                "op": "=",
                "param": "id"
              }
            ]
          }
        }
      implementation: io.blog.pipeline.BlogExecutor
```

#### SELECT Operation (Complex with Joins and Aggregations)

```yaml
    process:
      name: executor
      type: executor
      template: |
        {
          "operation": "SELECT",
          "source": { "table": "authors", "alias": "a" },
          "projection": [
            { "field": "a.id", "alias": "id" },
            { "field": "a.username", "alias": "username" },
            { "field": "a.email", "alias": "email" },
            { "field": "a.created_at", "alias": "created_at" },
            { "field": "COUNT(DISTINCT art.id)", "alias": "total_articles" },
            { "field": "COUNT(DISTINCT art.id) FILTER (WHERE art.is_published = true)", "alias": "published_articles" },
            { "field": "COUNT(DISTINCT c.id)", "alias": "total_comments" },
            { "field": "MAX(art.publish_date)", "alias": "latest_publication" }
          ],
          "windowFunctions": [
            { "expression": "COUNT(*) OVER()", "alias": "total_found" }
          ],
          "joins": [
            {
              "type": "LEFT",
              "table": "articles",
              "alias": "art",
              "on": [
                { "left": "a.id", "op": "=", "right": "art.author_id" }
              ]
            },
            {
              "type": "LEFT",
              "table": "comments",
              "alias": "c",
              "on": [
                { "left": "art.id", "op": "=", "right": "c.article_id" }
              ]
            }
          ],
          "filters": {
            "op": "AND",
            "conditions": [
              {
                "field": "a.id",
                "op": "=",
                "param": "id"
              }
            ]
          },
          "groupBy": ["a.id", "a.username", "a.email", "a.created_at"],
          "sort": { "a.created_at": "DESC" },
          "limit": { "default": 1 }
        }
      implementation: io.blog.pipeline.BlogExecutor
```

#### UPDATE Operation

```yaml
    process:
      name: executor
      type: executor
      template: |
        {
          "operation": "UPDATE",
          "source": { "table": "authors" },
          "columns": [
            { "field": "username", "param": "username", "optional": true },
            { "field": "email", "param": "email", "optional": true },
            { "field": "updated_at", "function": "CURRENT_TIMESTAMP" }
          ],
          "filters": {
            "op": "AND",
            "conditions": [
              {
                "field": "id",
                "op": "=",
                "param": "id"
              }
            ]
          },
          "returning": ["id", "username", "email", "updated_at"]
        }
      implementation: io.blog.pipeline.BlogExecutor
```

#### DELETE Operation

```yaml
    process:
      name: executor
      type: executor
      template: |
        {
          "operation": "DELETE",
          "source": { "table": "authors" },
          "filters": {
            "op": "AND",
            "conditions": [
              {
                "field": "id",
                "op": "=",
                "param": "id"
              }
            ]
          },
          "returning": ["id"]
        }
      implementation: io.blog.pipeline.BlogExecutor
```

---

### Step 4: Configure PostProcessor(s)

PostProcessors format and enrich the output:

```yaml
    postprocess:
      - name: outputProcessor
        type: transformer
        template: |
          {
            "format": "json",
            "envelope": {
              "success": true,
              "resource": "author",
              "operation": "create",
              "timestamp": "CURRENT_TIMESTAMP"
            },
            "status": {
              "code": 201,
              "message": "Author created successfully"
            }
          }
        implementation: io.blog.pipeline.BlogOutputProcessor
        description: >
          Formats the newly created author record into a standardized JSON
          response structure with metadata and appropriate HTTP status.
```

**Common Response Formats**:

```yaml
# Success response with data
{
  "format": "json",
  "envelope": {
    "success": true,
    "resource": "resource_name",
    "operation": "operation_name",
    "timestamp": "CURRENT_TIMESTAMP"
  },
  "status": {
    "code": 200,
    "message": "Success message"
  }
}

# Error response
{
  "format": "json",
  "envelope": {
    "success": false,
    "error": {
      "code": "ERROR_CODE",
      "message": "Error message",
      "details": "${error_details}"
    }
  },
  "status": {
    "code": 400,
    "message": "Bad Request"
  }
}

# Paginated response
{
  "format": "json",
  "envelope": {
    "success": true,
    "pagination": {
      "page": "${param:page}",
      "limit": "${param:limit}",
      "total": "${total_count}",
      "pages": "${calculated:total_pages}"
    }
  }
}
```

---

## DSL Reference

### Database Operations

#### Operation Types

| Operation | Description            | Returns                              |
|-----------|------------------------|--------------------------------------|
| `SELECT`  | Query data             | Result set                           |
| `INSERT`  | Create new record      | Inserted record (with `returning`)   |
| `UPDATE`  | Modify existing record | Updated record (with `returning`)    |
| `DELETE`  | Remove record          | Deleted record ID (with `returning`) |

---

### SELECT DSL

#### Basic Structure

```json
{
  "operation": "SELECT",
  "source": { "table": "table_name", "alias": "t" },
  "projection": [...],
  "joins": [...],
  "filters": {...},
  "groupBy": [...],
  "having": [...],
  "sort": {...},
  "limit": {...},
  "offset": {...},
  "windowFunctions": [...]
}
```

#### Projection (Columns)

```json
"projection": [
  // Simple field
  { "field": "t.column_name", "alias": "output_name" },
  
  // Aggregate function
  { "field": "COUNT(t.id)", "alias": "count" },
  { "field": "COUNT(DISTINCT t.category_id)", "alias": "unique_categories" },
  { "field": "SUM(t.amount)", "alias": "total_amount" },
  { "field": "AVG(t.rating)", "alias": "average_rating" },
  { "field": "MIN(t.created_at)", "alias": "earliest" },
  { "field": "MAX(t.updated_at)", "alias": "latest" },
  
  // Conditional aggregation (PostgreSQL FILTER)
  { 
    "field": "COUNT(*) FILTER (WHERE t.status = 'active')", 
    "alias": "active_count" 
  },
  
  // String functions
  { "field": "CONCAT(t.first_name, ' ', t.last_name)", "alias": "full_name" },
  { "field": "LOWER(t.email)", "alias": "email_lowercase" },
  { "field": "SUBSTRING(t.content, 1, 100)", "alias": "excerpt" }
]
```

#### Window Functions

```json
"windowFunctions": [
  // Total count
  { "expression": "COUNT(*) OVER()", "alias": "total_found" },
  
  // Running total
  { "expression": "SUM(amount) OVER(ORDER BY date)", "alias": "running_total" },
  
  // Rank
  { "expression": "ROW_NUMBER() OVER(ORDER BY score DESC)", "alias": "rank" },
  { "expression": "RANK() OVER(PARTITION BY category ORDER BY score DESC)", "alias": "category_rank" },
  
  // Moving average
  { 
    "expression": "AVG(value) OVER(ORDER BY date ROWS BETWEEN 6 PRECEDING AND CURRENT ROW)", 
    "alias": "moving_avg_7_days" 
  }
]
```

#### Joins

```json
"joins": [
  {
    "type": "INNER|LEFT|RIGHT|FULL",
    "table": "target_table",
    "alias": "tt",
    "on": [
      { "left": "t.id", "op": "=", "right": "tt.foreign_id" }
    ]
  },
  // Multiple join conditions
  {
    "type": "LEFT",
    "table": "another_table",
    "alias": "at",
    "on": [
      { "left": "t.id", "op": "=", "right": "at.primary_id" },
      { "left": "t.status", "op": "=", "right": "at.status" }
    ]
  }
]
```

#### Filters

```json
"filters": {
  "op": "AND|OR",
  "conditions": [
    // Simple condition
    {
      "field": "t.status",
      "op": "=|!=|>|<|>=|<=|LIKE|ILIKE|IN|NOT IN",
      "param": "status",
      "optional": true  // Filter is skipped if param not provided
    },
    
    // Parameter with transformation
    {
      "field": "t.username",
      "op": "ILIKE",
      "param": "search",
      "transform": {
        "type": "concat",
        "prefix": "%",
        "suffix": "%"
      },
      "optional": true
    },
    
    // Raw expression (for complex conditions)
    {
      "expression": "t.created_at >= CAST(:start_date AS TIMESTAMP)",
      "optional": true
    },
    
    // Nested conditions
    {
      "op": "OR",
      "conditions": [
        { "field": "t.title", "op": "ILIKE", "param": "search", "optional": true },
        { "field": "t.content", "op": "ILIKE", "param": "search", "optional": true }
      ],
      "optional": true
    },
    
    // Full-text search (PostgreSQL)
    {
      "expression": "to_tsvector('english', t.content) @@ plainto_tsquery(:search_text)",
      "optional": true
    }
  ]
}
```

#### Filter Operators

| Operator      | Description                      | Example                              |
|---------------|----------------------------------|--------------------------------------|
| `=`           | Equals                           | `status = 'active'`                  |
| `!=`          | Not equals                       | `status != 'deleted'`                |
| `>`           | Greater than                     | `age > 18`                           |
| `<`           | Less than                        | `price < 100`                        |
| `>=`          | Greater or equal                 | `score >= 50`                        |
| `<=`          | Less or equal                    | `quantity <= 10`                     |
| `LIKE`        | Pattern match (case-sensitive)   | `name LIKE '%John%'`                 |
| `ILIKE`       | Pattern match (case-insensitive) | `email ILIKE '%@example.com'`        |
| `IN`          | In list                          | `category IN ('A', 'B', 'C')`        |
| `NOT IN`      | Not in list                      | `status NOT IN ('draft', 'deleted')` |
| `IS NULL`     | Is null check                    | `deleted_at IS NULL`                 |
| `IS NOT NULL` | Not null check                   | `published_at IS NOT NULL`           |

#### GroupBy and Having

```json
"groupBy": ["t.id", "t.name", "t.category"],

"having": [
  {
    "expression": "COUNT(*) > :min_count",
    "optional": true
  },
  {
    "expression": "SUM(amount) >= :min_total",
    "optional": true
  }
]
```

#### Sorting

```json
// Simple sort
"sort": { "t.created_at": "DESC" }

// Multiple columns
"sort": {
  "t.priority": "ASC",
  "t.created_at": "DESC"
}

// Dynamic sort with default
"sort": "{param:sort,default:{'t.created_at':'DESC'}}"

// Sort by aggregate
"sort": {
  "total_count": "DESC",
  "t.name": "ASC"
}
```

#### Pagination

```json
// Fixed limit
"limit": { "default": 50 }

// Parameter-based limit
"limit": { "param": "limit", "default": 50 }

// Limit with bounds
"limit": { "param": "limit", "default": 50, "max": 100 }

// Offset
"offset": { "param": "offset", "default": 0 }

// Calculated offset (page-based)
"offset": { "calculated": "offset", "default": 0 }
// offset = (page - 1) * limit
```

---

### INSERT DSL

```json
{
  "operation": "INSERT",
  "source": { "table": "table_name" },
  "columns": [
    // Auto-generated field
    { "field": "id", "function": "gen_random_uuid()" },
    
    // Parameter from input
    { "field": "username", "param": "username" },
    
    // Optional parameter
    { "field": "bio", "param": "bio", "optional": true },
    
    // Function value
    { "field": "created_at", "function": "CURRENT_TIMESTAMP" },
    
    // Static value
    { "field": "status", "value": "active" },
    
    // Conditional value
    { 
      "field": "is_verified",
      "value": "${param:is_verified}",
      "default": false
    }
  ],
  "constraints": {
    "checkDuplicates": true,
    "conflictStrategy": "error|update|ignore"
  },
  "returning": ["id", "username", "created_at"]
}
```

---

### UPDATE DSL

```json
{
  "operation": "UPDATE",
  "source": { "table": "table_name" },
  "columns": [
    // Required update
    { "field": "username", "param": "username" },
    
    // Optional update
    { "field": "email", "param": "email", "optional": true },
    { "field": "bio", "param": "bio", "optional": true },
    
    // Auto-update timestamp
    { "field": "updated_at", "function": "CURRENT_TIMESTAMP" }
  ],
  "filters": {
    "op": "AND",
    "conditions": [
      { "field": "id", "op": "=", "param": "id" }
    ]
  },
  "returning": ["id", "username", "email", "updated_at"]
}
```

---

### DELETE DSL

```json
{
  "operation": "DELETE",
  "source": { "table": "table_name" },
  "filters": {
    "op": "AND",
    "conditions": [
      { "field": "id", "op": "=", "param": "id" }
    ]
  },
  "returning": ["id", "deleted_at"]
}
```

---

## Complete Examples

### Example 1: Simple CRUD Operations

#### Create Author

```yaml
create_author:
  uri: blog://authors/create
  description: Create a new blog author
  input: io.cheshire.core.pipeline.MaterializedInput
  
  pipeline:
    preprocess:
      - name: inputProcessor
        type: transformer
        template: |
          {
            "validation": {
              "required": ["username", "email"],
              "rules": {
                "username": {
                  "type": "string",
                  "minLength": 3,
                  "maxLength": 255,
                  "pattern": "^[a-zA-Z0-9_]+$"
                },
                "email": {
                  "type": "string",
                  "format": "email"
                }
              }
            },
            "sanitization": {
              "username": "trim",
              "email": "trim,lowercase"
            }
          }
        implementation: io.blog.pipeline.BlogInputProcessor
    
    process:
      name: executor
      type: executor
      template: |
        {
          "operation": "INSERT",
          "source": { "table": "authors" },
          "columns": [
            { "field": "id", "function": "gen_random_uuid()" },
            { "field": "username", "param": "username" },
            { "field": "email", "param": "email" },
            { "field": "created_at", "function": "CURRENT_TIMESTAMP" }
          ],
          "returning": ["id", "username", "email", "created_at"]
        }
      implementation: io.blog.pipeline.BlogExecutor
    
    postprocess:
      - name: outputProcessor
        type: transformer
        template: |
          {
            "status": { "code": 201 },
            "envelope": {
              "success": true,
              "resource": "author"
            }
          }
        implementation: io.blog.pipeline.BlogOutputProcessor
  
  output: io.cheshire.core.pipeline.MaterializedOutput
```

#### Get Author by ID

```yaml
get_author:
  uri: blog://authors/get
  description: Retrieve a single author by ID
  input: io.cheshire.core.pipeline.MaterializedInput
  
  pipeline:
    preprocess:
      - name: inputProcessor
        type: transformer
        template: |
          {
            "validation": {
              "required": ["id"],
              "rules": {
                "id": {
                  "type": "string",
                  "format": "uuid"
                }
              }
            }
          }
        implementation: io.blog.pipeline.BlogInputProcessor
    
    process:
      name: executor
      type: executor
      template: |
        {
          "operation": "SELECT",
          "source": { "table": "authors", "alias": "a" },
          "projection": [
            { "field": "a.id", "alias": "id" },
            { "field": "a.username", "alias": "username" },
            { "field": "a.email", "alias": "email" },
            { "field": "a.created_at", "alias": "created_at" }
          ],
          "filters": {
            "op": "AND",
            "conditions": [
              { "field": "a.id", "op": "=", "param": "id" }
            ]
          },
          "limit": { "default": 1 }
        }
      implementation: io.blog.pipeline.BlogExecutor
    
    postprocess:
      - name: outputProcessor
        type: transformer
        template: |
          {
            "status": { "code": 200 },
            "envelope": { "success": true }
          }
        implementation: io.blog.pipeline.BlogOutputProcessor
  
  output: io.cheshire.core.pipeline.MaterializedOutput
```

---

### Example 2: Advanced Query with Joins and Aggregations

```yaml
list_authors:
  uri: blog://authors/list
  description: >
    Retrieve paginated list of authors with optional filtering and statistics.
    Includes article counts, published article counts, comment counts, and latest publication date.
  
  input: io.cheshire.core.pipeline.MaterializedInput
  
  pipeline:
    preprocess:
      - name: inputProcessor
        type: transformer
        template: |
          {
            "validation": {
              "rules": {
                "page": {
                  "type": "integer",
                  "minimum": 1,
                  "default": 1
                },
                "limit": {
                  "type": "integer",
                  "minimum": 1,
                  "maximum": 100,
                  "default": 50
                },
                "search_author": {
                  "type": "string",
                  "minLength": 2,
                  "optional": true
                },
                "has_published": {
                  "type": "boolean",
                  "optional": true
                },
                "created_after": {
                  "type": "string",
                  "format": "date-time",
                  "optional": true
                },
                "created_before": {
                  "type": "string",
                  "format": "date-time",
                  "optional": true
                },
                "min_articles": {
                  "type": "integer",
                  "minimum": 0,
                  "optional": true
                },
                "min_comments": {
                  "type": "integer",
                  "minimum": 0,
                  "optional": true
                }
              }
            },
            "sanitization": {
              "search_author": "trim"
            },
            "calculated": {
              "offset": "(page - 1) * limit"
            }
          }
        implementation: io.blog.pipeline.BlogInputProcessor
    
    process:
      name: executor
      type: executor
      template: |
        {
          "operation": "SELECT",
          "source": { "table": "authors", "alias": "a" },
          "projection": [
            { "field": "a.id", "alias": "id" },
            { "field": "a.username", "alias": "username" },
            { "field": "a.email", "alias": "email" },
            { "field": "a.created_at", "alias": "created_at" },
            { "field": "COUNT(DISTINCT art.id)", "alias": "total_articles" },
            { "field": "COUNT(DISTINCT art.id) FILTER (WHERE art.is_published = true)", "alias": "published_articles" },
            { "field": "COUNT(DISTINCT c.id)", "alias": "total_comments" },
            { "field": "MAX(art.publish_date)", "alias": "latest_publication" }
          ],
          "windowFunctions": [
            { "expression": "COUNT(*) OVER()", "alias": "total_found" }
          ],
          "joins": [
            {
              "type": "LEFT",
              "table": "articles",
              "alias": "art",
              "on": [
                { "left": "a.id", "op": "=", "right": "art.author_id" }
              ]
            },
            {
              "type": "LEFT",
              "table": "comments",
              "alias": "c",
              "on": [
                { "left": "art.id", "op": "=", "right": "c.article_id" }
              ]
            }
          ],
          "filters": {
            "op": "AND",
            "conditions": [
              {
                "op": "OR",
                "conditions": [
                  {
                    "field": "a.username",
                    "op": "ILIKE",
                    "param": "search_author",
                    "transform": {
                      "type": "concat",
                      "prefix": "%",
                      "suffix": "%"
                    },
                    "optional": true
                  },
                  {
                    "field": "a.email",
                    "op": "ILIKE",
                    "param": "search_author",
                    "transform": {
                      "type": "concat",
                      "prefix": "%",
                      "suffix": "%"
                    },
                    "optional": true
                  }
                ],
                "optional": true
              },
              {
                "field": "art.is_published",
                "op": "=",
                "param": "has_published",
                "optional": true
              },
              {
                "expression": "a.created_at >= CAST(:created_after AS TIMESTAMP)",
                "optional": true
              },
              {
                "expression": "a.created_at <= CAST(:created_before AS TIMESTAMP)",
                "optional": true
              }
            ]
          },
          "groupBy": ["a.id", "a.username", "a.email", "a.created_at"],
          "having": [
            {
              "expression": "COUNT(DISTINCT art.id) >= :min_articles",
              "optional": true
            },
            {
              "expression": "COUNT(DISTINCT c.id) >= :min_comments",
              "optional": true
            }
          ],
          "sort": "{param:sort,default:{'a.created_at':'DESC'}}",
          "limit": { "param": "limit", "default": 50 },
          "offset": { "calculated": "offset", "default": 0 }
        }
      implementation: io.blog.pipeline.BlogExecutor
      description: >
        Queries authors with left joins to articles and comments to calculate statistics.
        Supports filtering by username/email search, publication status, date range,
        minimum article count, and minimum comment count. Results are paginated
        with window function for total count.
    
    postprocess:
      - name: outputProcessor
        type: transformer
        template: |
          {
            "status": { "code": 200 },
            "envelope": {
              "success": true,
              "pagination": {
                "page": "${param:page}",
                "limit": "${param:limit}",
                "total": "${window:total_found}",
                "pages": "${calculated:total_pages}"
              }
            }
          }
        implementation: io.blog.pipeline.BlogOutputProcessor
  
  output: io.cheshire.core.pipeline.MaterializedOutput
```

---

### Example 3: Full-Text Search with Ranking

```yaml
search_articles:
  uri: blog://articles/search
  description: >
    Full-text search across article titles and content with relevance ranking.
    Uses PostgreSQL full-text search capabilities (tsvector and tsquery).
  
  input: io.cheshire.core.pipeline.MaterializedInput
  
  pipeline:
    preprocess:
      - name: inputProcessor
        type: transformer
        template: |
          {
            "validation": {
              "required": ["query"],
              "rules": {
                "query": {
                  "type": "string",
                  "minLength": 2,
                  "maxLength": 200
                },
                "limit": {
                  "type": "integer",
                  "minimum": 1,
                  "maximum": 50,
                  "default": 10
                }
              }
            },
            "sanitization": {
              "query": "trim"
            }
          }
        implementation: io.blog.pipeline.BlogInputProcessor
    
    process:
      name: executor
      type: executor
      template: |
        {
          "operation": "SELECT",
          "source": { "table": "articles", "alias": "art" },
          "projection": [
            { "field": "art.id", "alias": "id" },
            { "field": "art.title", "alias": "title" },
            { "field": "LEFT(art.content, 200)", "alias": "excerpt" },
            { "field": "art.publish_date", "alias": "publish_date" },
            { "field": "a.username", "alias": "author_username" },
            {
              "field": "ts_rank(to_tsvector('english', art.title || ' ' || art.content), plainto_tsquery('english', :query))",
              "alias": "relevance_score"
            }
          ],
          "joins": [
            {
              "type": "INNER",
              "table": "authors",
              "alias": "a",
              "on": [
                { "left": "art.author_id", "op": "=", "right": "a.id" }
              ]
            }
          ],
          "filters": {
            "op": "AND",
            "conditions": [
              {
                "field": "art.is_published",
                "op": "=",
                "value": true
              },
              {
                "expression": "to_tsvector('english', art.title || ' ' || art.content) @@ plainto_tsquery('english', :query)"
              }
            ]
          },
          "sort": {
            "relevance_score": "DESC",
            "art.publish_date": "DESC"
          },
          "limit": { "param": "limit", "default": 10 }
        }
      implementation: io.blog.pipeline.BlogExecutor
    
    postprocess:
      - name: outputProcessor
        type: transformer
        template: |
          {
            "status": { "code": 200 },
            "envelope": {
              "success": true,
              "query": "${param:query}"
            }
          }
        implementation: io.blog.pipeline.BlogOutputProcessor
  
  output: io.cheshire.core.pipeline.MaterializedOutput
```

---

## Best Practices

### 1. Naming Conventions

✅ **DO**:

- Use `snake_case` for pipeline names
- Use descriptive, action-oriented names: `create_author`, `list_articles`, `update_comment`
- Follow pattern: `action_resource` (e.g., `get_author`, `delete_article`)
- Use consistent prefixes for related operations

❌ **DON'T**:

- Use camelCase or PascalCase
- Use generic names: `process`, `handle`, `do_something`
- Mix naming patterns

### 2. URI Design

✅ **DO**:

- Follow pattern: `domain://resource/action`
- Use plural for collections: `blog://articles/list`
- Use singular for single items: `blog://article/get`
- Be consistent across your domain

❌ **DON'T**:

- Mix singular and plural inconsistently
- Use verbs in resource names
- Include implementation details in URIs

### 3. Validation

✅ **DO**:

- Validate all required fields
- Use appropriate data types
- Set reasonable min/max limits
- Mark optional fields explicitly
- Sanitize user input (trim, lowercase, escape)

❌ **DON'T**:

- Skip validation for "trusted" sources
- Allow unbounded string lengths
- Accept unvalidated email addresses or URLs

### 4. Database Queries

✅ **DO**:

- Use table aliases consistently
- Specify field aliases for clarity
- Use optional filters for flexible queries
- Include appropriate indexes (in schema)
- Use window functions for counts (avoids second query)
- Group by all non-aggregated columns

❌ **DON'T**:

- Use `SELECT *` in production
- Create N+1 query problems
- Forget to add pagination
- Skip indexes on foreign keys

### 5. Error Handling

✅ **DO**:

- Return meaningful error messages
- Include error codes for programmatic handling
- Log errors with context
- Handle constraint violations gracefully

❌ **DON'T**:

- Expose database error messages to clients
- Return stack traces in production
- Use generic error messages

### 6. Performance

✅ **DO**:

- Set reasonable default limits (e.g., 50)
- Enforce maximum limits (e.g., 100)
- Use indexes on filter columns
- Use `LEFT JOIN` only when needed (use `INNER JOIN` for required relationships)
- Consider denormalization for read-heavy operations

❌ **DON'T**:

- Allow unlimited result sets
- Perform expensive operations in loops
- Fetch more data than needed

### 7. Documentation

✅ **DO**:

- Write clear, descriptive summaries
- Document all parameters
- Explain business logic
- Include examples in comments

❌ **DON'T**:

- Leave descriptions empty
- Assume parameters are self-explanatory

---

## Troubleshooting

### Common Issues

#### Issue 1: Validation Errors Not Appearing

**Problem**: Validation rules defined but not enforced

**Cause**: PreProcessor not properly implementing validation logic

**Solution**:

1. Verify `inputProcessor` implementation class exists
2. Check that validation template is valid JSON
3. Ensure required fields are listed correctly
4. Test validation rules in isolation

#### Issue 2: Query Returns Empty Results

**Problem**: SELECT query returns no data despite data existing

**Causes & Solutions**:

1. **Incorrect alias usage**:
   ```json
   // ❌ Wrong - missing table alias
   { "field": "id", "alias": "id" }
   
   // ✅ Correct - include table alias
   { "field": "a.id", "alias": "id" }
   ```

2. **Optional filter blocking results**:
   ```json
   // If param not provided but optional=false, query may fail
   {
     "field": "a.status",
     "op": "=",
     "param": "status",
     "optional": false  // ❌ Change to true if param is optional
   }
   ```

3. **Wrong join type**:
   ```json
   // ❌ INNER JOIN excludes authors with no articles
   { "type": "INNER", "table": "articles", ...}
   
   // ✅ LEFT JOIN includes all authors
   { "type": "LEFT", "table": "articles", ...}
   ```

#### Issue 3: Aggregation Errors

**Problem**: `ERROR: column "a.id" must appear in the GROUP BY clause`

**Solution**: Include all non-aggregated columns in `groupBy`:

```json
"projection": [
  { "field": "a.id", "alias": "id" },
  { "field": "a.username", "alias": "username" },  // ← Non-aggregated
  { "field": "a.email", "alias": "email" },        // ← Non-aggregated
  { "field": "COUNT(art.id)", "alias": "count" }   // ← Aggregated
],
"groupBy": ["a.id", "a.username", "a.email"]  // ✅ All non-aggregated fields
```

#### Issue 4: Window Function Not Working

**Problem**: Total count (window function) returns wrong value

**Cause**: Window function executed before HAVING clause

**Solution**: Window functions are calculated before HAVING. Use subquery if needed:

```json
// If you need total AFTER HAVING, use a subquery wrapper
// or calculate total separately
```

#### Issue 5: Parameters Not Binding

**Problem**: Query fails with "parameter not found: param_name"

**Causes & Solutions**:

1. **Typo in parameter name**:
   ```json
   // ❌ Mismatch
   { "field": "a.id", "op": "=", "param": "author_id" }
   // But input provides: { "id": "123" }
   
   // ✅ Match parameter names
   { "field": "a.id", "op": "=", "param": "id" }
   ```

2. **Missing optional flag**:
   ```json
   {
     "field": "a.status",
     "op": "=",
     "param": "status",
     "optional": true  // ✅ Required if param may not be provided
   }
   ```

#### Issue 6: Sort Not Applied

**Problem**: Results not sorted as expected

**Causes & Solutions**:

1. **Wrong syntax**:
   ```json
   // ❌ Wrong
   "sort": "created_at DESC"
   
   // ✅ Correct
   "sort": { "created_at": "DESC" }
   ```

2. **Missing alias**:
   ```json
   "projection": [
     { "field": "a.created_at", "alias": "created_at" }
   ],
   // ❌ Wrong - uses table.column in sort
   "sort": { "a.created_at": "DESC" }
   
   // ✅ Correct - uses alias
   "sort": { "created_at": "DESC" }
   ```

#### Issue 7: Cascade Delete Not Working

**Problem**: Deleting parent doesn't delete children

**Cause**: Foreign key constraint not configured with `ON DELETE CASCADE`

**Solution**: Update database schema:

```sql
-- Add cascade to existing foreign key
ALTER TABLE articles DROP CONSTRAINT articles_author_id_fkey;
ALTER TABLE articles ADD CONSTRAINT articles_author_id_fkey 
  FOREIGN KEY (author_id) REFERENCES authors(id) ON DELETE CASCADE;
```

---

## Advanced Patterns

### Pattern 1: Soft Delete

```yaml
soft_delete_author:
  uri: blog://authors/soft_delete
  description: Mark author as deleted without removing from database
  input: io.cheshire.core.pipeline.MaterializedInput
  
  pipeline:
    process:
      name: executor
      type: executor
      template: |
        {
          "operation": "UPDATE",
          "source": { "table": "authors" },
          "columns": [
            { "field": "deleted_at", "function": "CURRENT_TIMESTAMP" },
            { "field": "status", "value": "deleted" }
          ],
          "filters": {
            "op": "AND",
            "conditions": [
              { "field": "id", "op": "=", "param": "id" },
              { "field": "deleted_at", "op": "IS NULL" }
            ]
          },
          "returning": ["id", "deleted_at"]
        }
      implementation: io.blog.pipeline.BlogExecutor
  
  output: io.cheshire.core.pipeline.MaterializedOutput
```

### Pattern 2: Upsert (Insert or Update)

```yaml
upsert_author:
  uri: blog://authors/upsert
  description: Insert new author or update if username already exists
  input: io.cheshire.core.pipeline.MaterializedInput
  
  pipeline:
    process:
      name: executor
      type: executor
      template: |
        {
          "operation": "INSERT",
          "source": { "table": "authors" },
          "columns": [
            { "field": "id", "function": "gen_random_uuid()" },
            { "field": "username", "param": "username" },
            { "field": "email", "param": "email" },
            { "field": "created_at", "function": "CURRENT_TIMESTAMP" }
          ],
          "constraints": {
            "checkDuplicates": true,
            "conflictStrategy": "update",
            "conflictFields": ["username"],
            "updateFields": ["email", "updated_at"]
          },
          "returning": ["id", "username", "email", "created_at", "updated_at"]
        }
      implementation: io.blog.pipeline.BlogExecutor
  
  output: io.cheshire.core.pipeline.MaterializedOutput
```

### Pattern 3: Batch Operations

```yaml
batch_create_authors:
  uri: blog://authors/batch_create
  description: Create multiple authors in a single transaction
  input: io.cheshire.core.pipeline.MaterializedInput
  
  pipeline:
    preprocess:
      - name: inputProcessor
        type: transformer
        template: |
          {
            "validation": {
              "required": ["authors"],
              "rules": {
                "authors": {
                  "type": "array",
                  "minItems": 1,
                  "maxItems": 100,
                  "items": {
                    "type": "object",
                    "required": ["username", "email"]
                  }
                }
              }
            }
          }
        implementation: io.blog.pipeline.BlogInputProcessor
    
    process:
      name: executor
      type: executor
      template: |
        {
          "operation": "INSERT",
          "source": { "table": "authors" },
          "batch": true,
          "batchKey": "authors",
          "columns": [
            { "field": "id", "function": "gen_random_uuid()" },
            { "field": "username", "param": "username" },
            { "field": "email", "param": "email" },
            { "field": "created_at", "function": "CURRENT_TIMESTAMP" }
          ],
          "returning": ["id", "username", "email"]
        }
      implementation: io.blog.pipeline.BlogExecutor
  
  output: io.cheshire.core.pipeline.MaterializedOutput
```

---

## Summary

This guide covered:

✅ **Pipeline Architecture**: Three-stage processing (PreProcessor → Executor → PostProcessor)  
✅ **YAML Structure**: Complete pipeline definition format  
✅ **DSL Reference**: Database operations (SELECT, INSERT, UPDATE, DELETE)  
✅ **Query Features**: Joins, filters, aggregations, window functions, full-text search  
✅ **Validation**: Input validation and sanitization  
✅ **Best Practices**: Naming, URIs, performance, security  
✅ **Examples**: Simple CRUD to complex queries with statistics  
✅ **Troubleshooting**: Common issues and solutions  
✅ **Advanced Patterns**: Soft delete, upsert, batch operations

### Next Steps

1. **Start Simple**: Create basic CRUD pipelines first
2. **Add Complexity**: Gradually add joins, filters, and aggregations
3. **Test Thoroughly**: Validate all edge cases
4. **Monitor Performance**: Use database query analysis tools
5. **Document Well**: Add clear descriptions for team members

### Additional Resources

- **Blog App Example**: `blog-pipelines.yaml` - Complete working example with 12 pipelines
- **Framework Docs**: Cheshire framework documentation
- **Database Reference**: PostgreSQL documentation for advanced SQL features

---

**Need Help?**

- Check existing pipeline definitions in `src/main/resources/config/`
- Review test cases for processor implementations
- Consult framework documentation for SPI details

