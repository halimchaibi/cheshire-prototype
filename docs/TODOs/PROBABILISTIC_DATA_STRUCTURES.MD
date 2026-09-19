### Probabilistic Data Structures Exploration

#### Purpose

To explore and document various probabilistic data structures that can be leveraged to speed up queries and analytics in
data-intensive applications.

---

### 1. Bloom Filter

**Purpose:** Membership testing
**Key Advantages:** Very compact, fast insert and lookup
**Characteristics:**

* False positives possible
* No deletions (standard)
* Simple hash-based bit array
  **Use Cases:** Caches, network packet filters, database query acceleration

---

### 2. Counting Bloom Filter

**Purpose:** Membership with support for deletions
**Key Advantages:** Maintains count of inserted elements
**Characteristics:**

* Allows removal of items
* Slightly larger memory than standard Bloom filter
  **Use Cases:** Mutable sets, dynamic caches

---

### 3. Cuckoo Filter

**Purpose:** Membership + deletion
**Key Advantages:** Cache-efficient, supports deletions efficiently, low false positive rate
**Characteristics:**

* Uses Cuckoo hashing
* Fingerprint-based storage
  **Use Cases:** High-performance networking, key-value stores, in-memory filtering

---

### 4. Quotient Filter

**Purpose:** Membership testing with compact memory
**Key Advantages:** Cache-friendly, fast lookup and insert
**Characteristics:**

* Stores fingerprints instead of full keys
* Can support deletions
  **Use Cases:** Storage engines, in-memory compact sets

---

### 5. HyperLogLog (HLL)

**Purpose:** Approximate cardinality estimation
**Key Advantages:** Constant memory, fast updates
**Characteristics:**

* Estimates distinct elements
* Probabilistic error: tunable
  **Use Cases:** Unique visitor counting, analytics aggregation, big data streams

---

### 6. Count-Min Sketch

**Purpose:** Approximate frequency counts
**Key Advantages:** Sublinear memory, fast updates and queries
**Characteristics:**

* 2D array of hash functions × counters
* Bounded overestimation
  **Use Cases:** Heavy hitter detection, network telemetry, streaming analytics

---

### 7. MinHash

**Purpose:** Estimate set similarity (Jaccard similarity)
**Key Advantages:** Compact signatures, fast comparison
**Characteristics:**

* Hash-based signatures
* Probabilistic similarity measure
  **Use Cases:** Near-duplicate detection, recommendation systems, document clustering

---

### 8. Bloomier Filter

**Purpose:** Key-value lookup probabilistically
**Key Advantages:** Space-efficient mapping from key to value
**Characteristics:**

* Returns approximate value
* Compact storage
  **Use Cases:** Compact key-value stores, lookup tables

---

### 9. Top-K / Heavy Hitters Sketches

**Purpose:** Identify most frequent elements in a stream
**Key Advantages:** Sublinear memory usage, fast approximate top-K
**Characteristics:**

* Examples: Space-Saving, Lossy Counting, Sticky Sampling
* Provides approximate counts
  **Use Cases:** Telemetry, real-time analytics, trending detection

---

### Decision Guide

| Query Type              | Recommended Data Structure                      |
|-------------------------|-------------------------------------------------|
| Membership              | Bloom Filter / Cuckoo Filter / Quotient Filter  |
| Membership + Deletion   | Counting Bloom Filter / Cuckoo Filter           |
| Cardinality Estimation  | HyperLogLog                                     |
| Frequency Counting      | Count-Min Sketch                                |
| Set Similarity          | MinHash                                         |
| Key-Value Approximation | Bloomier Filter                                 |
| Heavy Hitters / Top-K   | Space-Saving / Lossy Counting / Sticky Sampling |

---

### Notes

* All data structures trade **accuracy vs memory**. False positives or approximate results are expected.
* Selection depends on **query type, memory constraints, update frequency, and performance needs**.
