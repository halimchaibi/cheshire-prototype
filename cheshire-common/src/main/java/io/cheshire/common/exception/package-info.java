/**
 * Common exception classes for consistent error handling across the framework.
 * <p>
 * <strong>Package Overview:</strong>
 * <p>
 * This package defines the exception hierarchy for the Cheshire Framework:
 * <pre>
 * RuntimeException
 *   └─ CheshireException (base)
 *       ├─ ConfigurationException - Configuration loading/validation errors
 *       ├─ ValidationException - Input/schema validation errors
 *       ├─ ExecutionException - Runtime execution errors
 *       └─ CheshireRuntimeError - Fatal runtime errors
 * </pre>
 * <p>
 * <strong>Base Exception:</strong>
 * <p>
 * {@link io.cheshire.common.exception.CheshireException} is the base runtime exception
 * for all framework errors, providing:
 * <ul>
 *   <li>Consistent error message format</li>
 *   <li>Cause chain preservation</li>
 *   <li>Unchecked exception model (extends RuntimeException)</li>
 * </ul>
 * <p>
 * <strong>Exception Types:</strong>
 * <p>
 * <strong>ConfigurationException:</strong> Configuration-related errors
 * <pre>{@code
 * throw new ConfigurationException("Missing required field: database.url");
 * throw new ConfigurationException("Invalid port: " + port, cause);
 * }</pre>
 * <p>
 * <strong>ValidationException:</strong> Input validation failures
 * <pre>{@code
 * throw new ValidationException("Field 'email' must be a valid email address");
 * throw new ValidationException("Age must be between 0 and 150");
 * }</pre>
 * <p>
 * <strong>ExecutionException:</strong> Runtime execution failures
 * <pre>{@code
 * throw new ExecutionException("Failed to execute query", sqlException);
 * throw new ExecutionException("Pipeline execution failed");
 * }</pre>
 * <p>
 * <strong>Design Philosophy:</strong>
 * <p>
 * Cheshire uses unchecked exceptions (RuntimeException) to avoid:
 * <ul>
 *   <li>Checked exception proliferation through call stacks</li>
 *   <li>Forced try-catch blocks for recoverable errors</li>
 *   <li>Verbose exception declarations on every method</li>
 * </ul>
 * <p>
 * Instead, exceptions are caught at appropriate boundaries:
 * <pre>{@code
 * // Framework boundary - catch and convert to ResponseEntity
 * try {
 *     TaskResult result = session.execute(task);
 *     return ResponseEntity.success(result.output());
 * } catch (CheshireException e) {
 *     log.error("Execution failed", e);
 *     return ResponseEntity.failure(e.getMessage());
 * }
 * }</pre>
 * <p>
 * <strong>Error Handling Patterns:</strong>
 * <p>
 * <strong>1. Fail Fast (Configuration):</strong>
 * <pre>{@code
 * public void validateConfig(Config config) {
 *     if (config.port() < 1 || config.port() > 65535) {
 *         throw new ConfigurationException("Invalid port: " + config.port());
 *     }
 * }
 * }</pre>
 * <p>
 * <strong>2. Error Propagation (Execution):</strong>
 * <pre>{@code
 * public Result executeQuery(Query query) {
 *     try {
 *         return database.query(query);
 *     } catch (SQLException e) {
 *         throw new ExecutionException("Query failed: " + query, e);
 *     }
 * }
 * }</pre>
 * <p>
 * <strong>3. Boundary Conversion (API):</strong>
 * <pre>{@code
 * // Convert exceptions to API responses at boundaries
 * public ResponseEntity handle(Request request) {
 *     try {
 *         return processRequest(request);
 *     } catch (ValidationException e) {
 *         return ResponseEntity.failure(400, e.getMessage());
 *     } catch (ExecutionException e) {
 *         return ResponseEntity.failure(500, "Internal error");
 *     }
 * }
 * }</pre>
 * <p>
 * <strong>Logging:</strong>
 * <p>
 * Exceptions should be logged at appropriate levels:
 * <pre>{@code
 * try {
 *     // operation
 * } catch (ConfigurationException e) {
 *     log.error("Configuration error - startup failed", e);  // ERROR
 *     System.exit(1);
 * } catch (ValidationException e) {
 *     log.warn("Validation failed for input: {}", input, e);  // WARN
 * } catch (ExecutionException e) {
 *     log.error("Execution failed", e);  // ERROR
 * }
 * }</pre>
 * <p>
 * <strong>Custom Exceptions:</strong>
 * <p>
 * Applications can extend CheshireException for domain-specific errors:
 * <pre>{@code
 * public class BlogException extends CheshireException {
 *     public BlogException(String message) {
 *         super(message);
 *     }
 * }
 *
 * public class ArticleNotFoundException extends BlogException {
 *     public ArticleNotFoundException(long id) {
 *         super("Article not found: " + id);
 *     }
 * }
 * }</pre>
 *
 * @see io.cheshire.common.exception.CheshireException
 * @see io.cheshire.common.exception.ConfigurationException
 * @see io.cheshire.common.exception.ValidationException
 * @see io.cheshire.common.exception.ExecutionException
 * @since 1.0.0
 */
package io.cheshire.common.exception;
