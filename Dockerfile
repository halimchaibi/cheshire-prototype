# Multi-stage Dockerfile for Cheshire Framework
# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Copy Maven wrapper and POM files
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./
COPY */pom.xml ./

# Download dependencies (layer caching)
RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline --batch-mode || true

# Copy source code
COPY . .

# Build application
RUN ./mvnw clean package -DskipTests --batch-mode && \
    mkdir -p /build/dist && \
    find . -name '*.jar' -not -name '*-tests.jar' -not -name '*-sources.jar' -not -name '*-javadoc.jar' \
      -exec cp {} /build/dist/ \;

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

LABEL maintainer="halim.chaibi@example.com"
LABEL description="Cheshire Framework - Expose resources as capabilities through MCP and HTTP"
LABEL version="${VERSION:-1.0-SNAPSHOT}"

# Create non-root user
RUN groupadd -r cheshire && \
    useradd -r -g cheshire -s /bin/false cheshire

# Set working directory
WORKDIR /app

# Copy JARs from builder
COPY --from=builder /build/dist/*.jar ./lib/

# Copy runtime configuration
COPY --from=builder /build/cheshire-runtime/src/main/resources ./config/

# Create necessary directories
RUN mkdir -p /app/logs /app/data && \
    chown -R cheshire:cheshire /app

# Switch to non-root user
USER cheshire

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# Expose ports
EXPOSE 8080 8443

# Set environment variables
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+UseStringDeduplication"
ENV CHESHIRE_CONFIG_PATH="/app/config"
ENV CHESHIRE_LOG_PATH="/app/logs"

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/lib/cheshire-runtime-*.jar"]

