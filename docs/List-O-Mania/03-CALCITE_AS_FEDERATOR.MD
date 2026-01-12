# Exploration: Apache Calcite Query Federation

## Overview

### Introduction to Apache Calcite

Apache Calcite is a dynamic data management framework that provides query optimization, query execution, and federated
query capabilities without being tied to a specific storage layer or execution engine.

#### Core Components

**1. SQL Parser**

- Parses SQL into an Abstract Syntax Tree (AST)
- Uses JavaCC-based parser
- Extensible grammar supporting SQL:2011+ and custom extensions
- Converts AST to relational algebra (RelNode tree)

**2. Validator**

- Validates the parsed SQL against schema metadata
- Resolves table/column references
- Type checking and inference
- Handles namespace resolution across multiple schemas

**3. Relational Algebra (RelNode)**
The core representation of queries as a tree of relational operators:

```
LogicalProject (name, age)
  └─ LogicalJoin (condition: mp.name = sp.name)
      ├─ LogicalFilter (born > '2000-01-01')
      │   └─ TableScan (mysql.person)
      └─ TableScan (spark.personMoreInformation)
```

Common RelNode types:

- `TableScan`: Read from a table
- `Filter`: WHERE clauses
- `Project`: Column selection/computation
- `Join`: Join operations
- `Aggregate`: GROUP BY operations
- `Sort`: ORDER BY operations
- `Union/Intersect/Minus`: Set operations

**4. Query Optimizer (Volcano/Cascades Planner)**

Calcite uses a cost-based optimizer with two planning phases:

**HepPlanner (Heuristic)**

- Rule-based transformations
- Applied in fixed order
- Fast, predictable
- Used for normalization and pushdown rules

**VolcanoPlanner (Cost-based)**

- Explores multiple execution plans
- Assigns costs to each plan
- Chooses lowest-cost plan
- Uses RelOptRules to transform plans

**5. Planner Rules (RelOptRule)**

Rules transform RelNode trees. Examples:

```java
// Push filter below join
FilterJoinRule.FilterIntoJoinRule.FILTER_ON_JOIN

// Push project through join
ProjectJoinTransposeRule.INSTANCE

// Convert logical to physical
EnumerableRules.ENUMERABLE_JOIN_RULE
```

**6. Adapters (Schema and Table Implementations)**

Adapters connect Calcite to actual data sources:

```java
public interface SchemaPlus {
  void add(String name, Table table);
  void add(String name, Schema schema);
}

public interface Table {
  RelDataType getRowType(RelDataTypeFactory typeFactory);
  Statistic getStatistic();
  Schema.TableType getJdbcTableType();
}

public interface ScannableTable extends Table {
  Enumerable<Object[]> scan(DataContext root);
}
```

**7. Execution Layer (Enumerable Convention)**

Calcite generates Java code for query execution:

```java
// Calcite generates code like:
public Enumerable<Object[]> execute() {
  final Enumerable<Object[]> left = leftTable.scan(context);
  final Enumerable<Object[]> right = rightTable.scan(context);

  return left.hashJoin(
    right,
    leftKey -> leftKey[0],
    rightKey -> rightKey[0],
    (leftRow, rightRow) -> combine(leftRow, rightRow)
  );
}
```

#### Query Execution Flow

```
SQL Query
  │
  ├─> [Parser] ───> SqlNode (AST)
  │
  ├─> [Validator] ───> Validated SqlNode
  │
  ├─> [SqlToRelConverter] ───> RelNode (Logical Plan)
  │
  ├─> [HepPlanner] ───> Normalized RelNode
  │      │
  │      └─> Apply rules: FilterJoinRule, ProjectMergeRule, etc.
  │
  ├─> [VolcanoPlanner] ───> Optimized Physical Plan
  │      │
  │      ├─> Generate alternative plans
  │      ├─> Calculate costs (I/O, CPU, Network)
  │      └─> Select best plan
  │
  ├─> [Code Generation] ───> Java Bytecode
  │
  └─> [Execution] ───> Results (Enumerable)
```

#### Federation Architecture

**Schema Federation**

```json
{
  "version": "1.0",
  "defaultSchema": "federated",
  "schemas": [
    {
      "name": "mysql",
      "type": "custom",
      "factory": "org.apache.calcite.adapter.jdbc.JdbcSchema$Factory",
      "operand": {
        "jdbcDriver": "com.mysql.jdbc.Driver",
        "jdbcUrl": "jdbc:mysql://host/db"
      }
    },
    {
      "name": "spark",
      "type": "custom",
      "factory": "com.example.SparkSchemaFactory",
      "operand": {
        "sparkMaster": "spark://host:7077"
      }
    }
  ]
}
```

**Cross-Engine Query Processing**

When you execute:

```sql
SELECT * FROM mysql.person mp
JOIN spark.personMoreInformation sp
  ON mp.name = sp.name
WHERE mp.born > '2000-01-01';
```

Default Calcite behavior:

1. **Parse and validate** across both schemas
2. **Push filters** where possible:
    - `WHERE born > '2000-01-01'` → pushed to MySQL adapter
3. **Pull data** into Calcite's memory:
    - MySQL adapter executes: `SELECT * FROM person WHERE born > '2000-01-01'`
    - Spark adapter executes: `SELECT * FROM personMoreInformation`
4. **Join in Calcite** using EnumerableHashJoin or EnumerableNestedLoopJoin
5. **Return results** to client

**The Problem**: This approach can be inefficient for large datasets because:

- Full table scan on Spark (no join pushdown)
- All data transferred over network
- Join performed in Calcite's memory (not in native engines)
- No optimization for selective filters

---

## Problem Statement

### Current Challenge

We need to execute federated queries across MySQL and Spark with efficient cross-engine joins. The default Calcite
behavior performs joins in-memory after full table scans on both sides, which is inefficient when:

1. **Left side filter is highly selective** (e.g., `born > '2000-01-01'` returns 100 rows)
2. **Right side table is large** (millions of rows)
3. **Join key is indexed** on the right side
4. **Network bandwidth is limited** between engines

### Desired Behavior: Dependent Join Pattern

```
1. Execute filtered query on MySQL
   → Returns 100 rows with names: ['Alice', 'Bob', 'Charlie', ...]

2. Batch the join keys (MAX_KEYS = 1000)
   → Batch 1: ['Alice', 'Bob', ..., 'Charlie'] (100 keys)

3. Query Spark with batched keys
   → SELECT * FROM personMoreInformation
     WHERE name IN ('Alice', 'Bob', 'Charlie', ...)

4. Join results in memory (100 × n rows instead of 100 × millions)

5. Return combined results
```

**Benefits**:

- Reduces data transfer: Only matching rows from Spark
- Leverages Spark indexes: IN clause can use indexes
- Scales with filter selectivity: More selective = better performance
- Avoids OOM: Doesn't materialize full right table

**Current Gap**: Calcite doesn't provide this optimization out of the box.

---

## Decision Drivers

1. **Performance**: Minimize data transfer and execution time for federated queries
2. **Scalability**: Handle tables with millions/billions of rows
3. **Maintainability**: Solution should be maintainable and upgrade-safe
4. **Flexibility**: Support various data sources (MySQL, Spark, PostgreSQL, etc.)
5. **Transparency**: Minimal changes to SQL syntax
6. **Cost-awareness**: Optimizer should choose best strategy based on statistics

---

## Solutions Considered

### Solution 1: Custom Planner Rule with Dependent Join

**Approach**: Implement a custom `RelOptRule` that transforms standard joins into dependent join patterns when
beneficial.

#### Implementation Architecture

```java
/**
 * Rule that transforms a Join into a DependentJoin when:
 * 1. Left side has selective filter
 * 2. Right side is large
 * 3. Join is equi-join
 */
public class DependentJoinRule extends RelOptRule {

  private static final int DEFAULT_MAX_KEYS = 1000;
  private static final double SELECTIVITY_THRESHOLD = 0.1;

  public DependentJoinRule() {
    super(
      operand(LogicalJoin.class,
        operand(RelNode.class, any()),
        operand(RelNode.class, any())
      ),
      "DependentJoinRule"
    );
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    LogicalJoin join = call.rel(0);
    RelNode left = call.rel(1);
    RelNode right = call.rel(2);

    // Check if suitable for dependent join
    if (!isSuitableForDependentJoin(join, left, right)) {
      return;
    }

    // Extract join condition
    RexNode condition = join.getCondition();
    List<RexNode> conjunctions = RelOptUtil.conjunctions(condition);

    // Find equi-join conditions
    List<Pair<Integer, Integer>> joinKeys =
        extractJoinKeys(conjunctions, left, right);

    if (joinKeys.isEmpty()) {
      return; // Not an equi-join
    }

    // Create DependentJoin
    DependentJoin dependentJoin = new DependentJoin(
      join.getCluster(),
      join.getTraitSet(),
      left,
      right,
      condition,
      join.getJoinType(),
      joinKeys,
      DEFAULT_MAX_KEYS
    );

    call.transformTo(dependentJoin);
  }

  private boolean isSuitableForDependentJoin(
      LogicalJoin join, RelNode left, RelNode right) {

    // Only for inner and left joins
    if (join.getJoinType() != JoinRelType.INNER &&
        join.getJoinType() != JoinRelType.LEFT) {
      return false;
    }

    // Estimate selectivity
    RelMetadataQuery mq = join.getCluster().getMetadataQuery();
    Double leftRowCount = mq.getRowCount(left);
    Double rightRowCount = mq.getRowCount(right);

    if (leftRowCount == null || rightRowCount == null) {
      return false;
    }

    // Left should be significantly smaller
    double selectivity = leftRowCount / rightRowCount;

    return selectivity < SELECTIVITY_THRESHOLD &&
           leftRowCount < DEFAULT_MAX_KEYS * 10;
  }

  private List<Pair<Integer, Integer>> extractJoinKeys(
      List<RexNode> conjunctions, RelNode left, RelNode right) {

    List<Pair<Integer, Integer>> keys = new ArrayList<>();
    int leftFieldCount = left.getRowType().getFieldCount();

    for (RexNode conj : conjunctions) {
      if (conj.getKind() == SqlKind.EQUALS) {
        RexCall call = (RexCall) conj;
        RexNode op0 = call.getOperands().get(0);
        RexNode op1 = call.getOperands().get(1);

        if (op0 instanceof RexInputRef && op1 instanceof RexInputRef) {
          int ref0 = ((RexInputRef) op0).getIndex();
          int ref1 = ((RexInputRef) op1).getIndex();

          if (ref0 < leftFieldCount && ref1 >= leftFieldCount) {
            keys.add(Pair.of(ref0, ref1 - leftFieldCount));
          } else if (ref1 < leftFieldCount && ref0 >= leftFieldCount) {
            keys.add(Pair.of(ref1, ref0 - leftFieldCount));
          }
        }
      }
    }

    return keys;
  }
}
```

#### DependentJoin RelNode

```java
public class DependentJoin extends BiRel {

  private final RexNode condition;
  private final JoinRelType joinType;
  private final List<Pair<Integer, Integer>> joinKeys;
  private final int maxKeys;

  public DependentJoin(
      RelOptCluster cluster,
      RelTraitSet traits,
      RelNode left,
      RelNode right,
      RexNode condition,
      JoinRelType joinType,
      List<Pair<Integer, Integer>> joinKeys,
      int maxKeys) {

    super(cluster, traits, left, right);
    this.condition = condition;
    this.joinType = joinType;
    this.joinKeys = joinKeys;
    this.maxKeys = maxKeys;
  }

  @Override
  public RelOptCost computeSelfCost(
      RelOptPlanner planner, RelMetadataQuery mq) {

    double leftRows = mq.getRowCount(left);
    double rightRows = mq.getRowCount(right);

    // Cost = left scan + (leftRows / maxKeys) * right lookups
    int batches = (int) Math.ceil(leftRows / maxKeys);

    // Assume indexed lookup cost << full scan
    double lookupCost = rightRows * 0.01; // 1% of full scan per lookup

    double cpu = leftRows + (batches * lookupCost);
    double io = leftRows + (batches * leftRows); // Data transferred

    return planner.getCostFactory().makeCost(rightRows, cpu, io);
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return new DependentJoin(
      getCluster(),
      traitSet,
      inputs.get(0),
      inputs.get(1),
      condition,
      joinType,
      joinKeys,
      maxKeys
    );
  }
}
```

#### Enumerable Implementation

```java
public class EnumerableDependentJoin extends DependentJoin
    implements EnumerableRel {

  public EnumerableDependentJoin(/* ... */) {
    super(/* ... */);
  }

  @Override
  public Result implement(EnumerableRelImplementor implementor, Prefer pref) {

    BlockBuilder builder = new BlockBuilder();

    // Implement left side
    Result leftResult = implementor.visitChild(
      this, 0, (EnumerableRel) left, pref);

    Expression leftEnum = builder.append("left", leftResult.block);

    // Generate dependent join code
    Expression dependentJoinExpr = builder.append(
      "dependentJoin",
      Expressions.call(
        BuiltInMethod.DEPENDENT_JOIN.method,
        leftEnum,
        implementor.getRootExpression(),
        Expressions.constant(/* right table metadata */),
        Expressions.constant(joinKeys),
        Expressions.constant(maxKeys),
        generateJoinCondition()
      )
    );

    builder.add(Expressions.return_(null, dependentJoinExpr));

    return implementor.result(physType, builder.toBlock());
  }

  private Expression generateJoinCondition() {
    // Generate lambda: (leftRow, rightRow) -> leftRow.name == rightRow.name
    ParameterExpression leftRow =
        Expressions.parameter(Object[].class, "leftRow");
    ParameterExpression rightRow =
        Expressions.parameter(Object[].class, "rightRow");

    Expression condition = null;
    for (Pair<Integer, Integer> key : joinKeys) {
      Expression leftKey = Expressions.arrayIndex(leftRow,
          Expressions.constant(key.left));
      Expression rightKey = Expressions.arrayIndex(rightRow,
          Expressions.constant(key.right));

      Expression equals = Expressions.equal(leftKey, rightKey);
      condition = condition == null ? equals :
          Expressions.andAlso(condition, equals);
    }

    return Expressions.lambda(
      Predicate2.class,
      condition,
      leftRow,
      rightRow
    );
  }
}
```

#### Runtime Execution

```java
public class DependentJoinExecutor {

  public static <T> Enumerable<T> dependentJoin(
      Enumerable<Object[]> leftEnum,
      DataContext root,
      TableMetadata rightTable,
      List<Pair<Integer, Integer>> joinKeys,
      int maxKeys,
      Predicate2<Object[], Object[]> joinCondition) {

    return new AbstractEnumerable<T>() {
      @Override
      public Enumerator<T> enumerator() {
        return new DependentJoinEnumerator(
          leftEnum.enumerator(),
          root,
          rightTable,
          joinKeys,
          maxKeys,
          joinCondition
        );
      }
    };
  }

  private static class DependentJoinEnumerator implements Enumerator<Object[]> {

    private final Enumerator<Object[]> leftEnum;
    private final DataContext root;
    private final TableMetadata rightTable;
    private final List<Pair<Integer, Integer>> joinKeys;
    private final int maxKeys;
    private final Predicate2<Object[], Object[]> joinCondition;

    private List<Object[]> currentBatch;
    private Iterator<Object[]> currentResults;
    private Object[] current;

    @Override
    public boolean moveNext() {
      while (true) {
        // Return next result from current batch
        if (currentResults != null && currentResults.hasNext()) {
          current = currentResults.next();
          return true;
        }

        // Collect next batch of keys
        currentBatch = collectNextBatch();
        if (currentBatch.isEmpty()) {
          return false; // No more data
        }

        // Extract join keys
        Set<Object> keys = extractJoinKeys(currentBatch);

        // Query right side with keys
        List<Object[]> rightRows = queryRightSide(keys);

        // Perform in-memory hash join
        currentResults = hashJoin(currentBatch, rightRows).iterator();
      }
    }

    private List<Object[]> collectNextBatch() {
      List<Object[]> batch = new ArrayList<>();

      while (batch.size() < maxKeys && leftEnum.moveNext()) {
        batch.add(leftEnum.current());
      }

      return batch;
    }

    private Set<Object> extractJoinKeys(List<Object[]> rows) {
      Set<Object> keys = new HashSet<>();

      for (Object[] row : rows) {
        for (Pair<Integer, Integer> joinKey : joinKeys) {
          keys.add(row[joinKey.left]);
        }
      }

      return keys;
    }

    private List<Object[]> queryRightSide(Set<Object> keys) {
      // Build IN clause filter
      String inClause = keys.stream()
        .map(k -> "'" + k.toString() + "'")
        .collect(Collectors.joining(","));

      String query = String.format(
        "SELECT * FROM %s WHERE %s IN (%s)",
        rightTable.tableName,
        rightTable.joinColumn,
        inClause
      );

      // Execute through appropriate adapter
      return executeAdapterQuery(root, rightTable.schema, query);
    }

    private List<Object[]> hashJoin(
        List<Object[]> leftRows, List<Object[]> rightRows) {

      // Build hash table on right side
      Map<Object, List<Object[]>> rightHash = new HashMap<>();
      for (Object[] rightRow : rightRows) {
        Object key = rightRow[joinKeys.get(0).right];
        rightHash.computeIfAbsent(key, k -> new ArrayList<>()).add(rightRow);
      }

      // Probe with left side
      List<Object[]> results = new ArrayList<>();
      for (Object[] leftRow : leftRows) {
        Object key = leftRow[joinKeys.get(0).left];
        List<Object[]> matches = rightHash.get(key);

        if (matches != null) {
          for (Object[] rightRow : matches) {
            if (joinCondition.apply(leftRow, rightRow)) {
              results.add(combineRows(leftRow, rightRow));
            }
          }
        }
      }

      return results;
    }

    private Object[] combineRows(Object[] left, Object[] right) {
      Object[] combined = new Object[left.length + right.length];
      System.arraycopy(left, 0, combined, 0, left.length);
      System.arraycopy(right, 0, combined, left.length, right.length);
      return combined;
    }

    @Override
    public Object[] current() {
      return current;
    }

    @Override
    public void reset() {
      leftEnum.reset();
      currentResults = null;
      currentBatch = null;
    }

    @Override
    public void close() {
      leftEnum.close();
    }
  }
}
```

#### Registration and Configuration

```java
public class CustomCalciteConfig {

  public static FrameworkConfig createConfig(SchemaPlus schema) {

    // Create program with custom rules
    HepProgramBuilder hepBuilder = new HepProgramBuilder();
    hepBuilder.addRuleInstance(new DependentJoinRule());
    hepBuilder.addRuleInstance(FilterJoinRule.FILTER_ON_JOIN);
    hepBuilder.addRuleInstance(ProjectJoinTransposeRule.INSTANCE);

    // Create volcano planner
    VolcanoPlanner volcanoPlanner = new VolcanoPlanner();
    volcanoPlanner.addRelTraitDef(ConventionTraitDef.INSTANCE);

    // Add enumerable rules
    volcanoPlanner.addRule(EnumerableRules.ENUMERABLE_JOIN_RULE);
    volcanoPlanner.addRule(EnumerableRules.ENUMERABLE_FILTER_RULE);
    volcanoPlanner.addRule(EnumerableRules.ENUMERABLE_PROJECT_RULE);
    volcanoPlanner.addRule(EnumerableRules.ENUMERABLE_TABLE_SCAN_RULE);

    // Add custom rule
    volcanoPlanner.addRule(new EnumerableDependentJoinRule());

    return Frameworks.newConfigBuilder()
      .defaultSchema(schema)
      .parserConfig(SqlParser.config())
      .programs(
        Programs.hep(hepBuilder.build(), true, DefaultRelMetadataProvider.INSTANCE),
        Programs.ofRules(volcanoPlanner.getRules())
      )
      .build();
  }
}
```

**Pros**:

- Native Calcite integration
- Cost-based optimization decides when to use dependent join
- Supports multiple data sources
- Transparent to users (no SQL changes needed)

**Cons**:

- Complex implementation (1000+ lines of code)
- Requires deep Calcite knowledge
- Maintenance burden with Calcite upgrades
- Testing complexity

---

### Solution 2: Custom Hint with Rule Matching

**Approach**: Implement custom SQL hint parser and matching rule.

#### Implementation

```java
/**
 * Parse custom hints from SQL comments
 */
public class HintParser {

  private static final Pattern DEPENDENT_JOIN_PATTERN =
    Pattern.compile("/\\*\\+\\s*DEPENDENT_JOIN\\(MAX_KEYS=(\\d+)\\)\\s*\\*/");

  public static Map<String, Object> parseHints(String sql) {
    Map<String, Object> hints = new HashMap<>();

    Matcher matcher = DEPENDENT_JOIN_PATTERN.matcher(sql);
    if (matcher.find()) {
      hints.put("dependent_join", true);
      hints.put("max_keys", Integer.parseInt(matcher.group(1)));
    }

    return hints;
  }
}

/**
 * Rule that uses hints to transform joins
 */
public class HintDrivenDependentJoinRule extends RelOptRule {

  private final Map<String, Object> hints;

  public HintDrivenDependentJoinRule(Map<String, Object> hints) {
    super(operand(LogicalJoin.class, any()), "HintDrivenDependentJoinRule");
    this.hints = hints;
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    if (!hints.containsKey("dependent_join")) {
      return; // No hint, skip
    }

    LogicalJoin join = call.rel(0);
    int maxKeys = (Integer) hints.get("max_keys");

    // Transform to DependentJoin
    DependentJoin dependentJoin = new DependentJoin(
      /* ... */,
      maxKeys
    );

    call.transformTo(dependentJoin);
  }
}

/**
 * Query execution with hint parsing
 */
public class HintAwareQueryExecutor {

  public ResultSet execute(String sql, Connection connection) {
    // Parse hints
    Map<String, Object> hints = HintParser.parseHints(sql);

    // Remove hints from SQL
    String cleanSql = sql.replaceAll("/\\*\\+[^*]*\\*/", "");

    // Create config with hint-driven rules
    FrameworkConfig config = createConfigWithHints(hints);

    // Execute
    return executeWithConfig(cleanSql, config);
  }
}
```

**Pros**:

- User control via SQL hints
- Simpler than full cost-based approach
- Easy to enable/disable per query

**Cons**:

- Still requires custom rule implementation
- User must know when to use hints
- Not automatic/transparent

---

### Solution 3: Adapter-Level Optimization

**Approach**: Implement pushdown capabilities in adapters themselves.

#### Spark Adapter with Lookup Support

```java
public class SmartSparkAdapter implements FilterableTable, ProjectableFilterableTable {

  @Override
  public Enumerable<Object[]> scan(
      DataContext root,
      List<RexNode> filters,
      int[] projects) {

    // Check if filters contain IN clause with many values
    Optional<InFilter> inFilter = extractInFilter(filters);

    if (inFilter.isPresent() && inFilter.get().values.size() > 100) {
      // Optimize for large IN clause
      return executeBatchedLookup(inFilter.get(), projects);
    }

    // Standard pushdown
    return executeStandardQuery(filters, projects);
  }

  private Enumerable<Object[]> executeBatchedLookup(
      InFilter inFilter, int[] projects) {

    // Split IN values into batches
    List<List<Object>> batches = Lists.partition(
      inFilter.values, 1000
    );

    // Execute batches in parallel
    List<Object[]> allResults = batches.parallelStream()
      .flatMap(batch -> {
        String query = buildQuery(inFilter.column, batch, projects);
        return executeSparkQuery(query).stream();
      })
      .collect(Collectors.toList());

    return Linq4j.asEnumerable(allResults);
  }
}

/**
 * Join rule that leverages adapter capabilities
 */
public class AdapterPushdownJoinRule extends RelOptRule {

  @Override
  public void onMatch(RelOptRuleCall call) {
    LogicalJoin join = call.rel(0);
    RelNode left = call.rel(1);
    RelNode right = call.rel(2);

    // Check if right adapter supports IN pushdown
    if (!(right instanceof TableScan)) {
      return;
    }

    TableScan rightScan = (TableScan) right;
    Table table = rightScan.getTable().unwrap(Table.class);

    if (!(table instanceof FilterableTable)) {
      return;
    }

    // Transform join to:
    // 1. Scan left
    // 2. Extract keys
    // 3. Push IN filter to right
    RelNode newJoin = createPushdownJoin(join, left, right);
    call.transformTo(newJoin);
  }
}
```

**Pros**:

- Leverages native adapter capabilities
- Can use engine-specific optimizations (Spark's broadcast, etc.)
- Simpler Calcite integration

**Cons**:

- Requires modifying each adapter
- Limited to what adapters can support
- Less portable across adapters

---

### Solution 4: Post-Processing with MaterializingJoin

**Approach**: Use Calcite's existing capabilities creatively.

#### Two-Phase Execution

```java
public class TwoPhaseJoinExecutor {

  public ResultSet execute(String originalQuery, Connection connection) {

    // Phase 1: Execute left side and collect keys
    String leftQuery = extractLeftSideQuery(originalQuery);
    ResultSet leftResults = connection.createStatement().executeQuery(leftQuery);

    Set<Object> joinKeys = new HashSet<>();
    List<Object[]> leftRows = new ArrayList<>();

    while (leftResults.next()) {
      Object[] row = extractRow(leftResults);
      leftRows.add(row);
      joinKeys.add(row[joinKeyIndex]);
    }

    // Phase 2: Rewrite query with IN clause
    String rewrittenQuery = rewriteWithInClause(originalQuery, joinKeys);

    // Execute rewritten query
    return connection.createStatement().executeQuery(rewrittenQuery);
  }

  private String rewriteWithInClause(String query, Set<Object> keys) {
    // Transform:
    // SELECT * FROM mysql.person mp JOIN spark.person sp ON mp.id = sp.id
    // WHERE mp.born > '2000'
    //
    // To:
    // SELECT * FROM mysql.person mp JOIN spark.person sp ON mp.id = sp.id
    // WHERE mp.born > '2000' AND sp.id IN ('key1', 'key2', ...)

    String inClause = keys.stream()
      .map(k -> "'" + k + "'")
      .collect(Collectors.joining(","));

    return query.replace(
      "ON mp.id = sp.id",
      "ON mp.id = sp.id AND sp.id IN (" + inClause + ")"
    );
  }
}
```

**Pros**:

- No Calcite modification needed
- Simple to implement
- Works with any adapter

**Cons**:

- Requires two round trips
- Limited optimization opportunities
- Query rewriting complexity
- Doesn't integrate with cost-based optimizer

---

### Solution 5: Use External Query Federation Tool

**Approach**: Use Presto/Trino or Dremio instead of Calcite.

#### Presto/Trino Example

```sql
-- Presto supports dynamic filtering automatically
SELECT *
FROM mysql.db.person mp
JOIN spark.catalog.person_info sp
  ON mp.name = sp.name
WHERE mp.born > '2000-01-01';

-- Presto will:
-- 1. Scan mysql.person with filter
-- 2. Build bloom filter from join keys
-- 3. Apply filter to spark scan (dynamic filtering)
-- 4. Perform distributed join
```

#### Dremio Example

```sql
-- Dremio with reflections
CREATE REFLECTION person_info_by_name
ON spark.person_info
DIMENSIONS (name)
PARTITION BY (name);

-- Query automatically uses reflection
SELECT *
FROM mysql.person mp
JOIN spark.person_info sp
  ON mp.name = sp.name
WHERE mp.born > '2000-01-01';
```

**Pros**:

- Production-ready
- Well-tested and optimized
- Active development and support
- Rich ecosystem

**Cons**:

- External dependency
- Less customization
- Operational overhead
- Licensing considerations (Dremio)

---

## Decision

### Recommended Approach: **Solution 1 + Solution 3 Hybrid**

Implement a **phased approach**:

**Phase 1: Adapter Enhancement (2-4 weeks)**

- Enhance Spark and other adapters to support efficient IN clause pushdown
- Add statistics to adapters for better cost estimation
- Implement `FilterableTable` interface properly

**Phase 2: Custom Rule Implementation (4-6 weeks)**

- Implement `DependentJoinRule` with cost-based decision making
- Create `DependentJoin` RelNode
- Implement `EnumerableDependentJoin` with batched execution
- Add comprehensive testing

**Phase 3: Optimization (2-4 weeks)**

- Add dynamic batch sizing based on statistics
- Implement parallel batch execution
- Add monitoring and metrics
- Performance tuning

### Decision Rationale

1. **Performance**: Provides 10-100x improvement for selective joins
2. **Flexibility**: Works across all adapters once implemented
3. **Maintainability**: Clean integration with Calcite's architecture
4. **Transparency**: Automatic optimization, no user changes needed
5. **Extensibility**: Easy to add more optimization patterns

---

## Implementation Roadmap

### Phase 1: Foundation (Weeks 1-4)

**Week 1-2: Adapter Enhancement**

```java
// Task 1.1: Enhance SparkAdapter
public class EnhancedSparkAdapter implements FilterableTable {

  @Override
  public Enumerable<Object[]> scan(
      DataContext root, List<RexNode> filters, int[] projects) {

    // Analyze filters for optimization opportunities
    FilterAnalysis analysis = analyzeFilters(filters);

    if (analysis.hasLargeInClause()) {
      return executeBatchedInQuery(analysis, projects);
    }

    return executeStandardQuery(filters, projects);
  }

  // Add statistics
  @Override
  public Statistic getStatistic() {
    return new StatisticImpl(
      getRowCount(),
      getUniqueKeys(),
      getSelectivity(),
      getDistribution()
    );
  }
}

// Task 1.2: Add comprehensive statistics
public class SparkStatisticsProvider {

  public Statistics gather(String tableName) {
    // Execute Spark ANALYZE TABLE
    sparkSession.sql("ANALYZE TABLE " + tableName + " COMPUTE STATISTICS");

    // Collect column statistics
    return new Statistics(
      rowCount,
      columnStats,
      histograms,
      ndv // number of distinct values
    );
  }
}
```

**Week 3-4: Testing Framework**

```java
// Task 1.3: Create test harness
public class FederatedQueryTester {

  @Test
  public void testBasicJoin() {
    String sql = "SELECT * FROM mysql.person mp " +
                 "JOIN spark.person_info sp ON mp.id = sp.id";

    ResultSet results = execute(sql);

    assertThat(results.getRowCount()).isGreaterThan(0);
    assertThat(getExecutionMetrics().dataTransferred)
      .isLessThan(expectedThreshold);
  }

  @Test
  public void testSelectiveFilter() {
    // Test the optimization case
    String sql = "SELECT * FROM mysql.person mp " +
                 "JOIN spark.person_info sp ON mp.id = sp.id " +
                 "WHERE mp.born > '2020-01-01'";

    // Should use dependent join pattern
    RelNode plan = getPlan(sql);
    assertThat(plan).containsNode(DependentJoin.class);
  }
}
```

### Phase 2: Core Implementation (Weeks 5-10)

**Week 5-6: DependentJoin RelNode**

```java
// Task 2.1: Implement core RelNode
public class DependentJoin extends BiRel {

  private final RexNode condition;
  private final JoinRelType joinType;
  private final ImmutableList<Pair<Integer, Integer>> joinKeys;
  private final int maxKeys;
  private final DependentJoinStrategy strategy;

  // Cost estimation
  @Override
  public RelOptCost computeSelfCost(
      RelOptPlanner planner, RelMetadataQuery mq) {

    double leftRows = mq.getRowCount(left);
    double rightRows = mq.getRowCount(right);

    // Factor in selectivity
    double selectivity = mq.getSelectivity(left, null);

    // Calculate batches
    int batches = (int) Math.ceil(leftRows / maxKeys);

    // Network cost
    double networkCost = calculateNetworkCost(leftRows, batches);

    // Lookup cost (assume indexed)
    double lookupCost = batches * Math.log(rightRows) * 100;

    // Total cost
    double cpu = leftRows + lookupCost;
    double io = networkCost;
    double memory = leftRows * rowWidth;

    return planner.getCostFactory().makeCost(io, cpu, memory);
  }

  // Validation
  @Override
  public RelNode accept(RelShuttle shuttle) {
    validate();
    return super.accept(shuttle);
  }

  private void validate() {
    // Ensure join keys are valid
    // Ensure max keys is reasonable
    // Ensure adapters support required operations
  }
}
```

**Week 7-8: Planner Rule**

```java
// Task 2.2: Implement transformation rule
public class DependentJoinRule extends RelOptRule {

  @Override
  public void onMatch(RelOptRuleCall call) {
    LogicalJoin join = call.rel(0);
    RelNode left = call.rel(1);
    RelNode right = call.rel(2);

    // Detailed suitability analysis
    SuitabilityAnalysis analysis = analyzeSuitability(join, left, right);

    if (!analysis.isSuitable()) {
      return;
    }

    // Extract configuration
    JoinConfiguration config = extractConfiguration(join);

    // Create dependent join
    DependentJoin dependentJoin = createDependentJoin(
      join, left, right, config, analysis
    );

    // Register transformation
    call.transformTo(dependentJoin);
  }

  private SuitabilityAnalysis analyzeSuitability(
      LogicalJoin join, RelNode left, RelNode right) {

    RelMetadataQuery mq = join.getCluster().getMetadataQuery();

    // Collect statistics
    double leftRows = mq.getRowCount(left);
    double rightRows = mq.getRowCount(right);
    double leftSelectivity = estimateSelectivity(left);

    // Check join type
    boolean supportedJoinType =
      join.getJoinType() == JoinRelType.INNER ||
      join.getJoinType() == JoinRelType.LEFT;

    // Check for equi-join
    List<Pair<Integer, Integer>> joinKeys =
      extractEquiJoinKeys(join.getCondition());
    boolean isEquiJoin = !joinKeys.isEmpty();

    // Check adapter capabilities
    boolean rightSupportsLookup = checkLookupSupport(right);

    // Decision criteria
    boolean isSuitable =
      supportedJoinType &&
      isEquiJoin &&
      rightSupportsLookup &&
      leftRows < rightRows * 0.1 && // Left is < 10% of right
      leftRows < 10000; // Reasonable batch size

    return new SuitabilityAnalysis(
      isSuitable,
      leftRows,
      rightRows,
      leftSelectivity,
      joinKeys,
      estimatedCostImprovement()
    );
  }
}
```

**Week 9-10: Enumerable Implementation**

```java
// Task 2.3: Implement execution
public class EnumerableDependentJoin extends DependentJoin
    implements EnumerableRel {

  @Override
  public Result implement(EnumerableRelImplementor implementor, Prefer pref) {

    // Generate code for dependent join execution
    BlockBuilder builder = new BlockBuilder();

    // Implement left child
    Result leftResult = implementor.visitChild(
      this, 0, (EnumerableRel) left, pref);

    // Generate execution code
    Expression dependentJoinExpr = generateExecutionCode(
      builder, leftResult, implementor
    );

    return implementor.result(physType, builder.toBlock());
  }

  private Expression generateExecutionCode(
      BlockBuilder builder,
      Result leftResult,
      EnumerableRelImplementor implementor) {

    // Reference to runtime executor
    Expression executorClass =
      Expressions.constant(DependentJoinExecutor.class);

    // Parameters
    Expression leftEnum = builder.append("left", leftResult.block);
    Expression rightMetadata = generateRightMetadata();
    Expression joinKeys = Expressions.constant(this.joinKeys);
    Expression maxKeys = Expressions.constant(this.maxKeys);
    Expression joinCondition = generateJoinConditionLambda();

    // Call runtime executor
    return Expressions.call(
      executorClass,
      "execute",
      leftEnum,
      implementor.getRootExpression(),
      rightMetadata,
      joinKeys,
      maxKeys,
      joinCondition
    );
  }
}
```

### Phase 3: Optimization & Production (Weeks 11-14)

**Week 11-12: Advanced Features**

```java
// Task 3.1: Dynamic batch sizing
public class AdaptiveBatchSizer {

  public int calculateOptimalBatchSize(
      double estimatedRows,
      double networkLatency,
      double rightTableSize) {

    // Balance between:
    // - Fewer batches (less overhead)
    // - Smaller batches (less memory)

    double optimalBatches = Math.sqrt(estimatedRows / networkLatency);
    int batchSize = (int) (estimatedRows / optimalBatches);

    // Constraints
    return Math.max(100, Math.min(batchSize, 5000));
  }
}

// Task 3.2: Parallel execution
public class ParallelDependentJoinExecutor {

  public Enumerable<Object[]> executeParallel(
      List<List<Object>> keyBatches,
      TableMetadata rightTable,
      int parallelism) {

    ExecutorService executor = Executors.newFixedThreadPool(parallelism);

    List<Future<List<Object[]>>> futures = keyBatches.stream()
      .map(batch -> executor.submit(() -> queryBatch(batch, rightTable)))
      .collect(Collectors.toList());

    // Collect results as they complete
    return Linq4j.asEnumerable(
      futures.stream()
        .flatMap(f -> {
          try {
            return f.get().stream();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        })
        .iterator()
    );
  }
}
```

**Week 13-14: Monitoring & Tuning**

```java
// Task 3.3: Add metrics
public class DependentJoinMetrics {

  private final Counter batchesExecuted;
  private final Histogram batchSize;
  private final Timer executionTime;
  private final Gauge<Double> hitRate;

  public void recordExecution(ExecutionStats stats) {
    batchesExecuted.inc(stats.batches);
    batchSize.update(stats.avgBatchSize);
    executionTime.update(stats.duration, TimeUnit.MILLISECONDS);

    // Calculate hit rate (rows returned / rows requested)
    double hitRate = (double) stats.rowsReturned / stats.rowsRequested;
    this.hitRate.setValue(hitRate);
  }
}

// Task 3.4: Add explain plan details
public class DependentJoinExplainWriter {

  @Override
  public void explainTerms(RelWriter pw) {
    super.explainTerms(pw);

    pw.item("strategy", "DEPENDENT_JOIN");
    pw.item("maxKeys", maxKeys);
    pw.item("estimatedBatches", estimatedBatches());
    pw.item("estimatedCost", estimateCost());
    pw.item("leftRows", estimateLeftRows());
    pw.item("rightRows", estimateRightRows());
  }
}
```

---

## Configuration

### Application Configuration

```yaml
# calcite-config.yaml
calcite:
  federation:
    dependent_join:
      enabled: true
      default_max_keys: 1000
      adaptive_batch_sizing: true
      parallel_execution: true
      max_parallelism: 4

    cost_model:
      network_latency_ms: 10
      lookup_cost_factor: 0.01  # 1% of full scan
      memory_cost_factor: 1.0

    thresholds:
      selectivity_threshold: 0.1  # Left < 10% of right
      min_right_rows: 10000       # Only for large right tables
      max_left_rows: 10000        # Reasonable batch limit

  schemas:
    mysql:
      type: jdbc
      driver: com.mysql.jdbc.Driver
      url: jdbc:mysql://localhost/db
      statistics:
        refresh_interval: 3600  # 1 hour

    spark:
      type: spark
      master: spark://localhost:7077
      statistics:
        refresh_interval: 1800  # 30 minutes
```

### Runtime Configuration

```java
public class CalciteConnectionFactory {

  public Connection createConnection(Config config) {

    Properties info = new Properties();
    info.setProperty("lex", "JAVA");
    info.setProperty("caseSensitive", "false");

    // Register custom rules
    info.setProperty("calcite.planner.rules",
      "DependentJoinRule,FilterJoinRule,ProjectJoinTransposeRule");

    // Set cost model parameters
    info.setProperty("calcite.cost.network_latency",
      String.valueOf(config.getNetworkLatency()));

    // Create connection
    Connection connection = DriverManager.getConnection(
      "jdbc:calcite:",
      info
    );

    // Register schemas
    registerSchemas(connection, config);

    return connection;
  }
}
```

---

## Testing Strategy

### Unit Tests

```java
@Test
public void testDependentJoinRuleFires() {
  // Given: A join with selective left side
  RelNode join = createSelectiveJoin();

  // When: Rule is applied
  HepPlanner planner = createPlannerWithRule(new DependentJoinRule());
  RelNode optimized = planner.findBestExp();

  // Then: Join is transformed to DependentJoin
  assertThat(optimized).isInstanceOf(DependentJoin.class);
}

@Test
public void testBatchingLogic() {
  // Given: 2500 rows with MAX_KEYS = 1000
  List<Object[]> rows = generateRows(2500);

  // When: Batching
  List<List<Object>> batches = batchKeys(rows, 1000);

  // Then: 3 batches created
  assertThat(batches).hasSize(3);
  assertThat(batches.get(0)).hasSize(1000);
  assertThat(batches.get(1)).hasSize(1000);
  assertThat(batches.get(2)).hasSize(500);
}
```

### Integration Tests

```java
@Test
public void testEndToEndDependentJoin() {
  // Setup: Insert test data
  mysqlConnection.execute(
    "INSERT INTO person VALUES (1, 'Alice', '2001-01-01')"
  );
  sparkConnection.execute(
    "INSERT INTO person_info VALUES ('Alice', 25, 'Engineer')"
  );

  // Execute: Federated query
  String sql = "SELECT * FROM mysql.person mp " +
               "JOIN spark.person_info sp ON mp.name = sp.name " +
               "WHERE mp.born > '2000-01-01'";

  ResultSet results = calciteConnection.execute(sql);

  // Verify: Correct results
  assertThat(results).hasSize(1);
  assertThat(results.getString("name")).isEqualTo("Alice");

  // Verify: Used dependent join
  String plan = calciteConnection.explain(sql);
  assertThat(plan).contains("DependentJoin");

  // Verify: Efficient execution
  assertThat(metrics.dataTransferred).isLessThan(1000); // bytes
}
```

### Performance Tests

```java
@Test
public void testPerformanceImprovement() {
  // Baseline: Standard join
  long baselineTime = measureExecutionTime(() ->
    executeStandardJoin(selectiveFilter)
  );

  // Optimized: Dependent join
  long optimizedTime = measureExecutionTime(() ->
    executeDependentJoin(selectiveFilter)
  );

  // Verify: Significant improvement
  double improvement = (baselineTime - optimizedTime) / (double) baselineTime;
  assertThat(improvement).isGreaterThan(0.5); // 50% faster
}
```
