Explore **runtime pluggable SPI implementations** beyond JARs, including
Python, Scala, and in-memory Java compilation:

---

# **Extending SPI Implementation in Cheshire**

## **1. Current Model**

* **StepProcessors**, **Executors**, **QueryEngines**, etc., currently take **JAR-based SPI implementations**.
* Loaded via **Java `ServiceLoader`** at startup.

---

## **2. Python-Based SPI Implementations**

You could allow users to write SPI implementations in Python, executed from the JVM via:

### **Option A: Py4J**

* **Py4J** allows Python code to be called from JVM (and vice versa) over a gateway.
* Implementation:

    1. User writes a Python class implementing the SPI interface.
    2. Start a **Py4J GatewayServer** in Python.
    3. JVM-based Executor calls Python methods via gateway.
* Pros:

    * Leverages Python ecosystem (ML libraries, pandas, numpy).
    * Minimal JVM changes.
* Cons:

    * Extra process (Python interpreter).
    * Performance overhead for frequent calls.
    * Requires serialization of inputs/outputs.

### **Option B: Jython (limited)**

* Jython runs Python on the JVM directly.
* Limitation: supports Python 2.7, not modern Python 3.
* Feasible only for simpler processing tasks.

---

## **3. Scala-Based SPI**

* Scala is JVM-native, so you could allow users to provide **Scala classes** implementing SPI.
* Implementation:

    1. Users provide a `.scala` file implementing the SPI interface.
    2. Compile at runtime using **Scala Compiler API**.
    3. Load resulting class into classloader dynamically.
* Pros:

    * Full JVM interoperability.
    * Can use existing Scala libraries seamlessly.
* Cons:

    * Users need to be familiar with Scala syntax.

---

## **4. In-Memory Java Compilation**

* Use **`JavaCompiler` API** (JSR-199) for compiling Java code at runtime.
* Workflow:

    1. User provides a Java source string or file implementing SPI interface.
    2. Use **`javax.tools.JavaCompiler`** or ***Janino*** to compile it to bytecode in memory.
    3. Load compiled class with a **custom ClassLoader**.
    4. Instantiate and inject into pipeline.
* Example skeleton:

```java
import javax.tools.*;
import java.util.*;
import java.lang.reflect.*;

public class InMemoryCompiler {
    public static Object compileAndLoad(String className, String sourceCode, Class<?> interfaceClass) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JavaFileObject file = new JavaSourceFromString(className, sourceCode);

        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
        JavaCompiler.CompilationTask task = compiler.getTask(null, null, null, null, null, compilationUnits);
        if (!task.call()) throw new RuntimeException("Compilation failed");

        ClassLoader loader = new InMemoryClassLoader();
        Class<?> cls = loader.loadClass(className);
        if (!interfaceClass.isAssignableFrom(cls)) {
            throw new RuntimeException("Class does not implement required SPI interface");
        }
        return cls.getDeclaredConstructor().newInstance();
    }
}
```

* **Dependencies**:

    * `JavaSourceFromString` and `InMemoryClassLoader` can be implemented with in-memory byte arrays.
    * Open source references: [InMemoryJavaCompiler](https://github.com/trung/InMemoryJavaCompiler)

* Pros:

    * No external JARs required.
    * Works fully in JVM.

* Cons:

    * User must provide valid Java code.
    * Compilation errors need careful handling.

---
