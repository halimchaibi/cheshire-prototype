# Building Cheshire Framework

This guide covers building, testing, and packaging the Cheshire Framework.

## Prerequisites

### Required

- **Java 21+** with preview features enabled
  ```bash
  java --version
  # Should show Java 21 or higher
  ```

### Optional

- **Maven 3.8+** (not required - project includes Maven wrapper)
- **Git** for version control

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/halimchaibi/cheshire-prototype.git
cd cheshire-prototype
```

### 2. Build with Maven Wrapper

**Linux/macOS:**

```bash
./mvnw clean install
```

**Windows:**

```cmd
mvnw.cmd clean install
```

The Maven wrapper will automatically:

- Download Maven 3.9.6 if not already cached
- Build all modules
- Run all tests
- Install artifacts to local Maven repository

## Build Commands

### Full Build

```bash
# Complete build with all tests
./mvnw clean install

# Build without tests (faster)
./mvnw clean install -DskipTests

# Build with test coverage report
./mvnw clean install jacoco:report
```

### Module-Specific Build

```bash
# Build specific module
cd cheshire-core
../mvnw clean install

# Build multiple modules
./mvnw clean install -pl cheshire-core,cheshire-runtime

# Build module and dependencies
./mvnw clean install -pl cheshire-runtime -am
```

### Testing

```bash
# Run all tests
./mvnw test

# Run tests for specific module
./mvnw test -pl cheshire-core

# Run integration tests
./mvnw verify

# Run specific test class
./mvnw test -Dtest=CheshireArchitectureTest

# Run tests with coverage
./mvnw clean test jacoco:report

# Skip tests
./mvnw clean install -DskipTests
```

### Cleaning

```bash
# Clean all build artifacts
./mvnw clean

# Deep clean (including IDE files)
./mvnw clean
rm -rf target/
rm -rf */target/
```

## Maven Wrapper

### What is Maven Wrapper?

Maven Wrapper (mvnw) is a script that:

- Downloads the correct Maven version automatically
- Ensures consistent builds across all environments
- Eliminates "works on my machine" issues
- No manual Maven installation required

### Wrapper Files

```
cheshire/
├── mvnw              # Unix/Linux/macOS wrapper script
├── mvnw.cmd          # Windows wrapper script
└── .mvn/
    └── wrapper/
        ├── maven-wrapper.properties  # Maven version config
        └── maven-wrapper.jar         # Wrapper implementation
```

### Using System Maven

If you prefer to use your system Maven installation:

```bash
# Check Maven version
mvn --version

# Use system Maven (must be 3.8+)
mvn clean install
```

## Build Profiles

### Default Profile

Builds all modules with standard settings:

```bash
./mvnw clean install
```

### Fast Build (No Tests)

```bash
./mvnw clean install -DskipTests
```

### Release Profile

```bash
./mvnw clean install -Prelease
```

## Module Build Order

Maven automatically builds modules in the correct order based on dependencies:

```
1. cheshire-bom (Bill of Materials)
2. cheshire-common (Utilities)
3. SPI Modules (Interfaces)
   - cheshire-pipeline-spi
   - cheshire-query-engine-spi
   - cheshire-source-provider-spi
4. cheshire-core (Framework Core)
5. Implementation Modules
   - cheshire-query-engine-jdbc
   - cheshire-source-provider-jdbc
6. cheshire-server (Server Implementations)
7. cheshire-runtime (Runtime Container)
8. cheshire-test-common (Test Utilities)
```

## Troubleshooting

### Maven Wrapper Download Issues

If the wrapper fails to download Maven:

```bash
# Manually download Maven
wget https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip

# Or use system Maven
mvn clean install
```

### Java Version Issues

Ensure Java 21+ with preview features:

```bash
# Check Java version
java --version

# Set JAVA_HOME if needed
export JAVA_HOME=/path/to/java-21
export PATH=$JAVA_HOME/bin:$PATH
```

### Build Failures

```bash
# Clean and rebuild
./mvnw clean install -U

# Skip tests to isolate build issues
./mvnw clean install -DskipTests

# Build with debug output
./mvnw clean install -X
```

### Dependency Issues

```bash
# Force update dependencies
./mvnw clean install -U

# Clear local Maven repository cache
rm -rf ~/.m2/repository/io/cheshire
./mvnw clean install
```

## IDE Integration

### IntelliJ IDEA

1. Open the project root `pom.xml`
2. IDEA will automatically detect Maven project
3. Enable annotation processing for Lombok
4. Set Java 21 with preview features enabled

### Eclipse

1. Import as "Existing Maven Project"
2. Right-click project → Maven → Update Project
3. Install Lombok plugin
4. Configure Java 21 with preview features

### VS Code

1. Install Java Extension Pack
2. Open project folder
3. VS Code will detect Maven project automatically

## Continuous Integration

### GitHub Actions Example

```yaml
name: Build

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        
    - name: Build with Maven Wrapper
      run: ./mvnw clean install
      
    - name: Run Tests
      run: ./mvnw test
      
    - name: Generate Coverage Report
      run: ./mvnw jacoco:report
```

## Performance Tips

### Parallel Builds

```bash
# Use multiple threads (2x CPU cores)
./mvnw clean install -T 2C

# Specific thread count
./mvnw clean install -T 4
```

### Offline Mode

```bash
# Build without checking for updates
./mvnw clean install -o
```

### Skip Non-Essential Tasks

```bash
# Skip tests and Javadoc
./mvnw clean install -DskipTests -Dmaven.javadoc.skip=true
```

## Packaging

### Create JAR Files

```bash
# Build JARs for all modules
./mvnw clean package

# JARs will be in each module's target/ directory
```

### Create Distribution

```bash
# Build everything and create distribution
./mvnw clean install
./mvnw package -Pdistribution
```

## Verification

### Run All Checks

```bash
# Full verification (tests + integration tests + checks)
./mvnw clean verify
```

### Architecture Tests

```bash
# Run ArchUnit tests
./mvnw test -pl cheshire-test-common
```

### Code Quality

```bash
# Run Checkstyle
./mvnw checkstyle:check

# Run SpotBugs
./mvnw spotbugs:check
```

## Getting Help

### Maven Help

```bash
# Show effective POM
./mvnw help:effective-pom

# Show dependency tree
./mvnw dependency:tree

# Show active profiles
./mvnw help:active-profiles

# List all goals
./mvnw help:describe -Dplugin=compiler
```

### Documentation

- [Maven Documentation](https://maven.apache.org/guides/)
- [Maven Wrapper Documentation](https://maven.apache.org/wrapper/)
- [Cheshire Framework Docs](docs/)

---

**Need Help?** Open an issue on [GitHub](https://github.com/cheshire-framework/cheshire-framework/issues)

