/*-
 * #%L
 * Cheshire :: Runtime
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

/**
 * Deployment utilities for production environments.
 *
 * <p><strong>Package Overview:</strong>
 *
 * <p>This package provides utilities for deploying Cheshire applications in production:
 *
 * <ul>
 *   <li>Configuration management (externalized config)
 *   <li>Environment variable resolution
 *   <li>Cloud platform integration
 *   <li>Container orchestration support
 * </ul>
 *
 * <p><strong>Deployment Patterns:</strong>
 *
 * <ul>
 *   <li><strong>Standalone JAR:</strong> Single executable with embedded config
 *   <li><strong>Docker Container:</strong> Containerized deployment with volume-mounted config
 *   <li><strong>Kubernetes:</strong> ConfigMaps and Secrets for configuration
 *   <li><strong>Cloud Functions:</strong> Serverless deployment with environment variables
 * </ul>
 *
 * <p><strong>Example Docker Deployment:</strong>
 *
 * <pre>{@code
 * # Dockerfile
 * FROM eclipse-temurin:21-jre
 * COPY target/app.jar /app/app.jar
 * EXPOSE 8080
 * ENTRYPOINT ["java", "-jar", "/app/app.jar"]
 *
 * # Run with external config
 * docker run -v /path/to/config:/config -p 8080:8080 app:latest --config /config/app.yaml
 * }</pre>
 *
 * <p><strong>Example Kubernetes Deployment:</strong>
 *
 * <pre>{@code
 * apiVersion: v1
 * kind: ConfigMap
 * metadata:
 *   name: cheshire-config
 * data:
 *   application.yaml: |
 *     application:
 *       name: my-app
 *     # ... rest of config
 * ---
 * apiVersion: apps/v1
 * kind: Deployment
 * metadata:
 *   name: cheshire-app
 * spec:
 *   replicas: 3
 *   template:
 *     spec:
 *       containers:
 *       - name: app
 *         image: cheshire-app:1.0
 *         volumeMounts:
 *         - name: config
 *           mountPath: /config
 *       volumes:
 *       - name: config
 *         configMap:
 *           name: cheshire-config
 * }</pre>
 *
 * @see io.cheshire.runtime.CheshireRuntime
 * @since 1.0.0
 */
package io.cheshire.runtime.deployment;
