# **Querying Custom/Homegrown Storage with Apache Calcite**

Apache Calcite allows querying **any structured data source** (SQL, NoSQL, JSON, CSV, proprietary formats) as long as
the data is exposed as **tables and columns** via a **Calcite adapter**.

This approach enables your pipeline to execute **SQL or domain-neutral queries** on data stored in custom formats.

---

## **1️⃣ Key Concepts**

| Concept               | Description                                                                        |
|-----------------------|------------------------------------------------------------------------------------|
| **Schema**            | Logical container for tables, like a database namespace.                           |
| **Table**             | Maps your data to rows and columns. Can be Scannable, Filterable, or Translatable. |
| **ScannableTable**    | Simple full-table scan. Returns rows as Java objects.                              |
| **FilterableTable**   | Supports SQL `WHERE` pushdown to filter data in your storage.                      |
| **TranslatableTable** | Supports relational algebra pushdown (joins, aggregates, groupings).               |
| **DataContext**       | Context of query execution, can carry runtime parameters.                          |

---

## **2️⃣ Implementation Steps**

### **Step 1: Implement a Table Adapter**

Map your storage to a `Table` interface. Example with a **ScannableTable**:

```java
public class MyCustomTable extends AbstractTable implements ScannableTable {

    private final List<Map<String,Object>> rows; // your storage

    public MyCustomTable(List<Map<String,Object>> rows) {
        this.rows = rows;
    }

    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory) {
        RelDataTypeFactory.Builder builder = new RelDataTypeFactory.Builder(typeFactory);
        builder.add("ID", typeFactory.createSqlType(SqlTypeName.INTEGER));
        builder.add("NAME", typeFactory.createSqlType(SqlTypeName.VARCHAR));
        return builder.build();
    }

    @Override
    public Enumerable<Object[]> scan(DataContext root) {
        return Linq4j.asEnumerable(rows)
                     .select(row -> new Object[]{row.get("ID"), row.get("NAME")});
    }
}
```

---

### **Step 2: Define a Schema**

Expose one or more tables through a Calcite schema:

```java
public class MyStorageSchema implements Schema {
    private final Map<String, Table> tables;

    public MyStorageSchema(Map<String, Table> tables) {
        this.tables = tables;
    }

    @Override
    public Map<String, Table> getTableMap() {
        return tables;
    }
}
```

---

### **Step 3: Register Schema and Table in Calcite**

```java
SchemaPlus rootSchema = calciteConnection.getRootSchema();
List<Map<String,Object>> rows = List.of(
    Map.of("ID", 1, "NAME", "Alice"),
    Map.of("ID", 2, "NAME", "Bob")
);

rootSchema.add("MYSCHEMA", new MyStorageSchema(
    Map.of("MYTABLE", new MyCustomTable(rows))
));
```

---

### **Step 4: Query with SQL or Domain-Neutral Templates**

```java
Statement stmt = calciteConnection.createStatement();
ResultSet rs = stmt.executeQuery("SELECT * FROM MYSCHEMA.MYTABLE WHERE ID = 1");

while(rs.next()) {
    System.out.println(rs.getInt("ID") + " " + rs.getString("NAME"));
}
```

* **Dynamic SQL:** You can generate SQL from your **domain-neutral JSON/YAML templates** and execute it against the
  custom adapter.
* **Joins / Aggregates:** If your Table implements `TranslatableTable`, Calcite can push complex queries into your
  storage engine.

---

## **3️⃣ Benefits**

* **Query any data source** without moving data to a database.
* **Integrate multiple sources**: SQL databases, NoSQL, CSV, JSON, custom formats.
* **Unified SQL interface**: your pipeline can use domain-neutral templates, jOOQ, or direct SQL.
* **Extensible**: add filtering, aggregation, and join pushdown in your Table adapters.

---

### **4️⃣ Notes / Best Practices**

* Start with `ScannableTable` for **read-only access**.
* Implement `FilterableTable` for **efficient WHERE filters**.
* Implement `TranslatableTable` for **full relational pushdown** (joins, groupings, aggregates).
* Use **Calcite’s RelBuilder** if you want to **construct queries programmatically**.
* Parameterize queries using `DataContext` for **dynamic runtime values**.

---

Here’s an enriched version of your document with **IoTDB adapter** added, showing how it can be plugged as a Calcite
`Table`/`Schema` source provider in your pipeline. I also added a **code snippet example** for clarity.

---

# **Querying Custom/Homegrown Storage with Apache Calcite (Including IoTDB Adapter)**

Apache Calcite allows querying **any structured data source** (SQL, NoSQL, JSON, CSV, IoT/Time-Series, proprietary
formats) as long as the data is exposed as **tables and columns** via a **Calcite adapter**.

This approach enables your pipeline to execute **SQL or domain-neutral queries** on data stored in custom formats or
time-series databases like **IoTDB**.

---

## **1️⃣ Key Concepts**

| Concept               | Description                                                                          |
|-----------------------|--------------------------------------------------------------------------------------|
| **Schema**            | Logical container for tables, like a database namespace.                             |
| **Table**             | Maps your data to rows and columns. Can be Scannable, Filterable, or Translatable.   |
| **ScannableTable**    | Simple full-table scan. Returns rows as Java objects.                                |
| **FilterableTable**   | Supports SQL `WHERE` pushdown to filter data in your storage.                        |
| **TranslatableTable** | Supports relational algebra pushdown (joins, aggregates, groupings).                 |
| **DataContext**       | Context of query execution, can carry runtime parameters.                            |
| **Source Provider**   | Cheshire SPI layer that exposes any backend (IoTDB, Spark, JDBC) as a Calcite table. |

---

## **2️⃣ Implementation Steps**

### **Step 1: Implement a Table Adapter**

Map your storage to a `Table` interface. Example with a **ScannableTable**:

```java
public class MyCustomTable extends AbstractTable implements ScannableTable {

    private final List<Map<String,Object>> rows;

    public MyCustomTable(List<Map<String,Object>> rows) {
        this.rows = rows;
    }

    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory) {
        RelDataTypeFactory.Builder builder = new RelDataTypeFactory.Builder(typeFactory);
        builder.add("ID", typeFactory.createSqlType(SqlTypeName.INTEGER));
        builder.add("NAME", typeFactory.createSqlType(SqlTypeName.VARCHAR));
        return builder.build();
    }

    @Override
    public Enumerable<Object[]> scan(DataContext root) {
        return Linq4j.asEnumerable(rows)
                     .select(row -> new Object[]{row.get("ID"), row.get("NAME")});
    }
}
```

---

# **Step 4: IoTDB Adapter Example**

IoTDB can be exposed as a **Calcite Table**, allowing time-series queries:

```java
public class IoTDBTable extends AbstractTable implements ScannableTable {

    private final String device;
    private final Session iotdbSession;

    public IoTDBTable(String device, Session iotdbSession) {
        this.device = device;
        this.iotdbSession = iotdbSession;
    }

    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory) {
        RelDataTypeFactory.Builder builder = new RelDataTypeFactory.Builder(typeFactory);
        builder.add("TIME", typeFactory.createSqlType(SqlTypeName.BIGINT));
        builder.add("VALUE", typeFactory.createSqlType(SqlTypeName.DOUBLE));
        return builder.build();
    }

    @Override
    public Enumerable<Object[]> scan(DataContext root) {
        List<Object[]> results = new ArrayList<>();
        try (SessionDataSet dataSet = iotdbSession.executeQueryStatement(
                String.format("SELECT * FROM %s", device))) {
            while (dataSet.hasNext()) {
                RowRecord record = dataSet.next();
                results.add(new Object[]{record.getTimestamp(), record.getFields().get(0).getDoubleValue()});
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Linq4j.asEnumerable(results);
    }
}
```

---

### **Step 5: Register IoTDB Table in Schema**

```java
Session session = new Session("127.0.0.1", 6667, "root", "root");
rootSchema.add("IOTDB", new MyStorageSchema(
    Map.of("TEMPERATURE", new IoTDBTable("root.sg1.device1.temperature", session))
));
```

Now Calcite can query IoTDB using:

```sql
SELECT TIME, VALUE FROM IOTDB.TEMPERATURE WHERE TIME > 1690000000000;
```

---

