## Exploration: Integration of Bloom Filters for Join Optimization

### **Problem Statement**

Our current system processes large-scale joins between "Dimension" tables (small) and "Fact" tables (large). Currently,
all data from the Fact table is scanned and sent to the join operator, leading to high I/O and network overhead. We need
a mechanism to discard non-matching rows from the Fact table as early as possible (at the source scan) before they reach
the join stage.

### **Proposed Approach**

Consider implementing **Sideways Information Passing (SIP)** using Bloom filters within the Calcite framework. This would involve
capturing join keys from the "build" side of a join and pushing them down as a dynamic filter to the "probe" side scan.

---

### **Detailed Architecture**

#### **1. Relational Algebra Transformation**

We will introduce a new Calcite `RelOptRule` (e.g., `BloomFilterJoinRule`).

* **Pattern Match:** The rule will look for a `LogicalJoin`.
* **Cost Evaluation:** The rule only triggers if the build side is estimated to be significantly smaller than the probe
  side.
* **Transformation:** It will inject a `LogicalFilter` above the probe-side `TableScan`. This filter will contain a
  custom `SqlOperator` (e.g., `IN_BLOOM_FILTER`) referencing the build-side keys.

#### **2. Execution Flow (The "Two-Phase" Scan)**

The execution engine must be modified to support a non-linear flow:

1. **Phase 1 (Build):** Execute the right-hand side of the join. Collect the join keys and populate a bitset (Bloom
   filter).
2. **Phase 2 (Pushdown):** Serialize the Bloom filter and inject it into the `DataContext` or the scan parameters of the
   left-hand side.
3. **Phase 3 (Probe):** The `TableScan` uses the bitset to skip rows.

#### **3. Components**

| Component                 | Responsibility                                                                                                                                 |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| **`BloomFilterOperator`** | A custom `SqlFunction` that evaluates `membership(key, bitset)`.                                                                               |
| **`BloomFilterRule`**     | A `RelOptRule` that transforms `Join(A, B)` into `Join(Filter(A, BF), B)`.                                                                     |
| **`BloomFilterProvider`** | A utility class (likely using Apache Commons Collections) to manage bitset size based on cardinality and desired false-positive rate (e.g., ). |

---

### **Consequences**

#### **Positive**

* **Reduced I/O:** Significantly fewer rows are read/processed from the large table.
* **Lower Network Latency:** In distributed environments, the "shuffled" data size is drastically reduced.
* **Memory Efficiency:** Bloom filters are compact compared to Hash Sets.

#### **Negative / Risks**

* **False Positives:** Bloom filters are probabilistic. We **must** keep the actual join condition in place to filter
  out the small percentage of false hits.
* **Planning Complexity:** The optimizer needs accurate cardinality estimates to decide if the overhead of building the
  filter is worth the scan savings.
* **CPU Overhead:** Every row in the Fact table scan now requires a hashing operation.

---

### **Alternatives Considered**

* **Semi-Joins:** Calcite already supports `SemiJoinRule`. However, standard Semi-Joins often require passing all keys,
  which can be memory-intensive. Bloom filters provide a more memory-efficient "sketch" of those keys.
* **Zone Maps/Min-Max:** Already in use, but ineffective for high-cardinality join keys that aren't sorted.
