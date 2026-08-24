# Cheshire SQL Template DSL Documentation

## Introduction

The Cheshire SQL Template DSL (Domain-Specific Language) is a JSON-based language designed to dynamically generate SQL queries, the samples are based on the Chinook Music Store database. This DSL allows developers to define database operations in a structured, safe, and reusable format that can be easily parsed by the `SqlTemplateQueryBuilder` engine.

### Why Use This DSL?

1. **Security**: Automatic parameter binding prevents SQL injection attacks
2. **Reusability**: Templates can be shared and reused across multiple operations
3. **Type Safety**: Input validation matches database schema constraints
4. **Database Agnostic**: Abstracts SQL dialect differences
5. **Dynamic Query Building**: Supports runtime parameter injection and filtering
6. **Maintainability**: Centralizes SQL logic in readable JSON templates

## Basic Structure

All SQL templates follow this JSON structure:

```json
{
  "operation": "SELECT|INSERT|UPDATE|DELETE",
  "source": { ... },
  "projection": [ ... ],
  "aggregates": [ ... ],
  "filters": { ... },
  "joins": [ ... ],
  "groupBy": [ ... ],
  "having": [ ... ],
  "sort": [ ... ],
  "limit": { ... },
  "offset": { ... },
  "columns": [ ... ],
  "set": [ ... ],
  "returning": [ ... ]
}
```

## Operations

### SELECT Operation

```json
{
  "operation": "SELECT",
  "source": { "table": "artists", "alias": "a" },
  "projection": [
    { "field": "a.artistId", "alias": "id" },
    { "field": "a.name", "alias": "artist_name" }
  ],
  "filters": {
    "op": "AND",
    "conditions": [
      { "field": "a.name", "op": "LIKE", "param": "artist_name" }
    ]
  },
  "sort": [
    { "field": "a.name", "direction": "ASC" }
  ],
  "limit": 10,
  "offset": 0
}
```

### INSERT Operation

```json
{
  "operation": "INSERT",
  "source": { "table": "artists" },
  "columns": [
    { "field": "name", "param": "artist_name" },
    { "field": "createdAt", "function": "CURRENT_TIMESTAMP" }
  ],
  "returning": ["artistId", "name", "createdAt"]
}
```

### UPDATE Operation

```json
{
  "operation": "UPDATE",
  "source": { "table": "tracks" },
  "set": [
    { "field": "unitPrice", "param": "new_price" },
    { "field": "updatedAt", "function": "CURRENT_TIMESTAMP" }
  ],
  "filters": {
    "op": "AND",
    "conditions": [
      { "field": "albumId", "op": "=", "param": "album_id" }
    ]
  },
  "returning": ["trackId", "name", "unitPrice"]
}
```

### DELETE Operation

```json
{
  "operation": "DELETE",
  "source": { "table": "playlist_track" },
  "filters": {
    "op": "AND",
    "conditions": [
      { "field": "playlistId", "op": "=", "param": "playlist_id" }
    ]
  },
  "returning": ["playlistId", "trackId", "addedAt"]
}
```

## Core Components

### Source Definition

Defines the primary table for the operation:

```json
{
  "table": "artists",           // Required: Table name
  "alias": "a"                  // Optional: Table alias
}
```

### Projection (Field Selection)

Selects which columns to return:

```json
"projection": [
  {
    "field": "a.artistId",      // Required: Column or expression
    "alias": "id"               // Optional: Output column name
  },
  {
    "field": "UPPER(a.name)",   // Supports SQL functions
    "alias": "uppercase_name"
  },
  {
    "field": "CONCAT(a.firstName, ' ', a.lastName)",
    "alias": "full_name"
  }
]
```

### Aggregates

Define aggregate functions:

```json
"aggregates": [
  {
    "func": "COUNT",            // Aggregate function: COUNT, SUM, AVG, MIN, MAX
    "field": "t.trackId",       // Field to aggregate
    "alias": "total_tracks"     // Output alias
  },
  {
    "func": "SUM",
    "field": "t.milliseconds",
    "alias": "total_duration"
  }
]
```

### Filters (WHERE Clause)

Complex filtering with nested conditions:

```json
"filters": {
  "op": "AND",                  // Logical operator: AND, OR
  "conditions": [
    // Simple condition
    {
      "field": "a.name",
      "op": "LIKE",
      "param": "artist_name",   // Parameter name (binds to :artist_name)
      "optional": true          // Optional: Skip if param is null/empty
    },
    // Direct value
    {
      "field": "t.unitPrice",
      "op": ">",
      "value": "0.99"           // Static value (not parameterized)
    },
    // Nested condition
    {
      "op": "OR",
      "conditions": [
        { "field": "t.name", "op": "LIKE", "param": "search" },
        { "field": "t.composer", "op": "LIKE", "param": "search" }
      ],
      "optional": true
    },
    // IN operator
    {
      "field": "c.country",
      "op": "IN",
      "param": "countries"      // Expects array parameter
    },
    // BETWEEN operator
    {
      "field": "i.invoiceDate",
      "op": "BETWEEN",
      "param": "start_date",
      "param2": "end_date"
    },
    // NULL check
    {
      "field": "t.composer",
      "op": "IS NULL"
    },
    // NOT operator
    {
      "field": "i.paid",
      "op": "!=",
      "value": "true"
    }
  ]
}
```

**Supported Operators:**

- Comparison: `=`, `!=`, `<>`, `<`, `<=`, `>`, `>=`
- Pattern: `LIKE`, `NOT LIKE`, `ILIKE` (case-insensitive)
- Range: `BETWEEN`, `NOT BETWEEN`
- Set: `IN`, `NOT IN`
- Null: `IS NULL`, `IS NOT NULL`
- String: `~` (regex), `!~` (not regex)

### Joins

Define table relationships:

```json
"joins": [
  {
    "type": "INNER",            // INNER, LEFT, RIGHT, FULL, CROSS
    "table": "albums",
    "alias": "alb",             // Optional: Alias for joined table
    "on": [                     // Join conditions
      {
        "left": "art.artistId",  // Left side field
        "op": "=",               // Comparison operator
        "right": "alb.artistId"  // Right side field
      },
      // Multiple conditions are ANDed
      {
        "left": "alb.releaseYear",
        "op": ">",
        "value": "2000"
      }
    ]
  },
  {
    "type": "LEFT",
    "table": "tracks",
    "alias": "t",
    "on": [
      { "left": "alb.albumId", "op": "=", "right": "t.albumId" }
    ]
  }
]
```

### Grouping and Aggregation

```json
"groupBy": [
  "art.artistId",              // Simple field name
  "art.name",                  // Another field
  "EXTRACT(YEAR FROM i.invoiceDate)"  // Expression
],

"having": [                    // Post-aggregation filtering
  {
    "field": "COUNT(t.trackId)",
    "op": ">",
    "param": "min_tracks"
  },
  {
    "field": "SUM(ii.quantity * ii.unitPrice)",
    "op": ">=",
    "param": "min_revenue"
  }
]
```

### Sorting

```json
"sort": [
  {
    "field": "total_revenue",   // Field or alias to sort by
    "direction": "DESC"         // ASC or DESC
  },
  {
    "field": "artist_name",
    "direction": "ASC"
  },
  {
    "field": "RANDOM()",        // Random ordering
    "direction": ""             // Direction optional for functions
  }
]
```

### Pagination

```json
"limit": {                     // Can be number or parameter reference
  "param": "page_size",        // Use parameter value
  "default": 50                // Fallback if parameter is null
},
"offset": {                    // For pagination
  "param": "page",             // Page number parameter
  "default": 0,
  "transform": "(page - 1) * limit"  // Calculate offset from page number
}
```

Or simpler:

```json
"limit": 10,                   // Static value
"offset": 20                   // Static offset
```

### Column Definitions (for INSERT/UPDATE)

```json
"columns": [                   // For INSERT
  {
    "field": "name",           // Column name
    "param": "artist_name",    // Parameter binding
    "nullable": true           // Allow null values
  },
  {
    "field": "createdAt",
    "function": "CURRENT_TIMESTAMP"  // SQL function
  },
  {
    "field": "status",
    "value": "active"          // Static value
  }
],

"set": [                       // For UPDATE (same structure as columns)
  {
    "field": "unitPrice",
    "param": "new_price"
  }
]
```

### Returning Clause

Specify which columns to return after INSERT/UPDATE/DELETE:

```json
"returning": [
  "artistId",                  // Simple column
  "name",
  "createdAt",
  "ROW_COUNT() as affected"    // Function with alias
]
```

## Advanced Features

### Parameter Transformation

```json
{
  "field": "i.invoiceDate",
  "op": ">=",
  "param": "start_date",
  "transform": "DATE_TRUNC('month', :start_date)"  // Transform parameter before use
}
```

### Conditional Inclusion

```json
{
  "field": "a.name",
  "op": "LIKE",
  "param": "search_term",
  "optional": true,            // Condition is skipped if param is null/empty
  "includeIf": "search_type == 'artist'"  // Only include if condition is met
}
```

### Dynamic Field Selection

```json
"projection": [
  {
    "field": "a.artistId",
    "alias": "id",
    "includeIf": "{param:fields} contains 'id'"  // Include based on parameter
  }
]
```

### Subqueries

```json
{
  "field": "artistId",
  "op": "IN",
  "subquery": {
    "operation": "SELECT",
    "source": { "table": "top_artists" },
    "projection": [{ "field": "id" }]
  }
}
```

### Common Table Expressions (CTEs)

```json
{
  "operation": "WITH",
  "ctes": [
    {
      "name": "top_sellers",
      "query": {
        "operation": "SELECT",
        "source": { "table": "artists" },
        "projection": [{ "field": "artistId" }],
        "limit": 10
      }
    }
  ],
  "mainQuery": {
    "operation": "SELECT",
    "source": { "table": "top_sellers" }
  }
}
```

## Parameter Binding

### Parameter Types

The DSL supports various parameter types:

```json
{
  // String parameters
  "param": "artist_name",      // Binds as string
  
  // Numeric parameters
  "param": "min_price",        // Binds as decimal
  "param": "limit_value",      // Binds as integer
  
  // Array parameters
  "param": "genre_ids",        // Binds as array for IN clause
  
  // Date/time parameters
  "param": "start_date",       // Automatically converted to TIMESTAMP
  
  // Boolean parameters
  "param": "active_only"       // Binds as boolean
}
```

### Parameter Sources

Parameters can come from:

- HTTP query parameters
- Request body (JSON)
- Session variables
- Environment variables
- Default values in template

## Template Variables

### Built-in Variables

```json
{
  "field": "userId",
  "op": "=",
  "value": "{session.userId}"  // Session variable
}
```

### Context Variables

```json
{
  "field": "updatedBy",
  "op": "=",
  "value": "{context.userId}"  // Execution context
}
```

## Usage Examples

### Complete SELECT with Joins and Aggregates

```json
{
  "operation": "SELECT",
  "source": { "table": "artists", "alias": "art" },
  "projection": [
    { "field": "art.name", "alias": "artist" },
    { "field": "COUNT(DISTINCT alb.albumId)", "alias": "album_count" }
  ],
  "aggregates": [
    { "func": "SUM", "field": "t.milliseconds", "alias": "total_duration" },
    { "func": "AVG", "field": "t.unitPrice", "alias": "avg_price" }
  ],
  "joins": [
    {
      "type": "LEFT",
      "table": "albums",
      "alias": "alb",
      "on": [{ "left": "art.artistId", "op": "=", "right": "alb.artistId" }]
    },
    {
      "type": "LEFT",
      "table": "tracks",
      "alias": "t",
      "on": [{ "left": "alb.albumId", "op": "=", "right": "t.albumId" }]
    }
  ],
  "filters": {
    "op": "AND",
    "conditions": [
      { "field": "art.name", "op": "LIKE", "param": "search", "optional": true },
      { "field": "alb.releaseYear", "op": ">=", "param": "min_year", "optional": true },
      {
        "op": "OR",
        "conditions": [
          { "field": "t.genreId", "op": "=", "param": "genre1" },
          { "field": "t.genreId", "op": "=", "param": "genre2" }
        ],
        "optional": true
      }
    ]
  },
  "groupBy": ["art.artistId", "art.name"],
  "having": [
    { "field": "COUNT(DISTINCT alb.albumId)", "op": ">", "param": "min_albums" }
  ],
  "sort": [
    { "field": "album_count", "direction": "DESC" },
    { "field": "art.name", "direction": "ASC" }
  ],
  "limit": { "param": "limit", "default": 20 },
  "offset": { "param": "page", "default": 0, "transform": "(:page - 1) * :limit" }
}
```

### Batch INSERT with Multiple Rows

```json
{
  "operation": "BATCH_INSERT",
  "source": { "table": "tracks" },
  "batchParam": "tracks",  // Expects array of objects
  "columns": [
    { "field": "name", "param": "name" },
    { "field": "albumId", "param": "albumId" },
    { "field": "unitPrice", "param": "unitPrice" },
    { "field": "createdAt", "function": "CURRENT_TIMESTAMP" }
  ],
  "returning": ["trackId"]
}
```

## Template Inheritance and Composition

### Base Templates

```json
{
  "name": "base_pagination",
  "limit": { "param": "limit", "default": 50 },
  "offset": { "param": "offset", "default": 0 }
}
```

### Template Extension

```json
{
  "extends": "base_pagination",
  "operation": "SELECT",
  "source": { "table": "artists" },
  "projection": [
    { "field": "artistId", "alias": "id" },
    { "field": "name", "alias": "artist_name" }
  ]
}
```

## Error Handling and Validation

### Required Parameters

```json
{
  "field": "artistId",
  "op": "=",
  "param": "id",
  "required": true,           // Throws error if parameter is missing
  "validate": {               // Validation rules
    "type": "integer",
    "min": 1,
    "message": "Artist ID must be a positive integer"
  }
}
```

### Default Values

```json
{
  "field": "status",
  "op": "=",
  "param": "filter_status",
  "default": "active"        // Use if parameter is null
}
```

## Performance Optimization

### Query Hints

```json
{
  "operation": "SELECT",
  "hints": [
    "/*+ INDEX(artists idx_artist_name) */"  // Database-specific hints
  ],
  "source": { "table": "artists" }
}
```

### Caching Directives

```json
{
  "operation": "SELECT",
  "cache": {
    "ttl": 300,              // Cache for 5 minutes
    "key": "artists:{params.name}:{params.page}"  // Cache key template
  },
  "source": { "table": "artists" }
}
```

## Integration with MCP Server

### Mapping to MCP Operations

Each SQL template maps to an MCP operation:

```yaml
# MCP Manifest
resourceTemplates:
  - uriTemplate: "chinook://artists{?page,limit,name}"
    metadata:
      operationId: "getArtists"
      
# SQL Template
{
  "name": "getArtists",
  "operation": "SELECT",
  "source": { "table": "artists" },
  "filters": {
    "conditions": [
      { "field": "name", "op": "LIKE", "param": "name", "optional": true }
    ]
  }
}
```

### Input Schema Validation

```json
{
  "operation": "INSERT",
  "source": { "table": "artists" },
  "inputSchema": {           // JSON Schema validation
    "type": "object",
    "properties": {
      "name": {
        "type": "string",
        "maxLength": 120,
        "minLength": 1
      }
    },
    "required": ["name"]
  },
  "columns": [
    { "field": "name", "param": "name" }
  ]
}
```

## Best Practices

### 1. Always Use Parameter Binding

❌ **Bad**: Concatenate values directly

```json
{
  "field": "name",
  "op": "=",
  "value": "'" + userInput + "'"  // SQL Injection risk!
}
```

✅ **Good**: Use parameter binding

```json
{
  "field": "name",
  "op": "=",
  "param": "artist_name"  // Safe parameter binding
}
```

### 2. Define Aliases for Clarity

```json
"projection": [
  {
    "field": "a.artistId",
    "alias": "id"  // Clear output name
  }
]
```

### 3. Use Optional Parameters for Flexible Queries

```json
{
  "field": "genreId",
  "op": "=",
  "param": "genre_filter",
  "optional": true  // Query works with or without this filter
}
```

### 4. Group Related Conditions

```json
"filters": {
  "op": "AND",
  "conditions": [
    {
      "op": "OR",  // Group alternative conditions
      "conditions": [
        { "field": "name", "op": "LIKE", "param": "search" },
        { "field": "composer", "op": "LIKE", "param": "search" }
      ]
    }
  ]
}
```

### 5. Use Functions for Complex Logic

```json
{
  "field": "createdAt",
  "op": ">=",
  "function": "DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month')"
}
```

### 6. Template Composition for Reusability

```json
// base_query.json
{
  "limit": { "param": "limit", "default": 50 },
  "offset": { "param": "offset", "default": 0 }
}

// artists_query.json
{
  "extends": "base_query",
  "operation": "SELECT",
  "source": { "table": "artists" }
}
```

## Common Patterns

### Search with Multiple Fields

```json
{
  "op": "OR",
  "conditions": [
    { "field": "title", "op": "LIKE", "param": "q" },
    { "field": "description", "op": "LIKE", "param": "q" },
    { "field": "tags", "op": "LIKE", "param": "q" }
  ],
  "optional": true
}
```

### Date Range Filtering

```json
{
  "op": "AND",
  "conditions": [
    {
      "field": "createdAt",
      "op": ">=",
      "param": "start_date",
      "transform": "DATE(:start_date)"
    },
    {
      "field": "createdAt",
      "op": "<",
      "param": "end_date",
      "transform": "DATE(:end_date) + INTERVAL '1 day'"
    }
  ]
}
```

### Pagination with Total Count

```json
{
  "operation": "SELECT_WITH_COUNT",
  "source": { "table": "artists" },
  "projection": [{ "field": "*" }],
  "limit": 10,
  "offset": 0,
  "includeTotal": true  // Adds SELECT COUNT(*) in separate query
}
```

## Troubleshooting

### Common Issues

1. **Missing Parameters**: Ensure all required parameters are provided
2. **Type Mismatches**: Check parameter types match database column types
3. **SQL Injection**: Never use `value` with user input, always use `param`
4. **Performance Issues**: Add indexes on filtered/sorted columns
5. **Null Handling**: Use `nullable: true` for optional columns

### Debugging Templates

Enable debug logging to see generated SQL:

```java
// In your application
SqlQueryRequest request = SqlTemplateQueryBuilder.buildQuery(template, params);
System.out.println("Generated SQL: " + request.sql());
System.out.println("Parameters: " + request.parameters());
```

## Conclusion

The Cheshire SQL Template DSL provides a powerful, safe, and maintainable way to define database operations. By separating SQL logic from application code, it enables:

- **Security**: Automatic parameter binding prevents SQL injection
- **Maintainability**: Centralized SQL management
- **Reusability**: Share templates across services
- **Type Safety**: Input validation against schemas
- **Performance**: Optimized query generation
