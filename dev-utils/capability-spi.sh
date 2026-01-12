#!/usr/bin/env bash
set -e

BASE_DIR="cheshire-capability-spi/src/main/java/io/cheshire/spi/capability"

mkdir -p "$BASE_DIR"

# -------------------------------------------------------------------
# Core Capability SPI
# -------------------------------------------------------------------

cat > "$BASE_DIR/Capability.java" <<'EOF'
package io.cheshire.spi.capability;

public interface Capability {
    CanonicalOutput execute(CanonicalInput input, ExecutionContext ctx)
            throws CapabilityException;
}
EOF

cat > "$BASE_DIR/ExecutionContext.java" <<'EOF'
package io.cheshire.spi.capability;

import java.time.Instant;
import java.util.Map;

public interface ExecutionContext {
    CapabilityId capabilityId();
    Map<String, Object> attributes();
    Instant deadline();
    CancellationToken cancellation();
}
EOF

cat > "$BASE_DIR/CanonicalInput.java" <<'EOF'
package io.cheshire.spi.capability;

public interface CanonicalInput {}
EOF

cat > "$BASE_DIR/CanonicalOutput.java" <<'EOF'
package io.cheshire.spi.capability;

public interface CanonicalOutput {}
EOF

# -------------------------------------------------------------------
# Pipeline model
# -------------------------------------------------------------------

cat > "$BASE_DIR/Pipeline.java" <<'EOF'
package io.cheshire.spi.capability;

public interface Pipeline {
    CanonicalOutput run(CanonicalInput input, ExecutionContext ctx)
            throws CapabilityException;
}
EOF

cat > "$BASE_DIR/PipelineConfig.java" <<'EOF'
package io.cheshire.spi.capability;

import java.util.List;

public record PipelineConfig(
        String id,
        List<PipelineStep<?, ?>> steps
) {}
EOF

cat > "$BASE_DIR/PipelineStep.java" <<'EOF'
package io.cheshire.spi.capability;

import java.util.Map;

public record PipelineStep<I, O>(
        StepKind kind,
        String name,
        Step<I, O> implementation,
        Map<String, Object> config
) {}
EOF

cat > "$BASE_DIR/StepKind.java" <<'EOF'
package io.cheshire.spi.capability;

public enum StepKind {
    PREPROCESS,
    PROCESS,
    POSTPROCESS
}
EOF

# -------------------------------------------------------------------
# Steps
# -------------------------------------------------------------------

cat > "$BASE_DIR/Step.java" <<'EOF'
package io.cheshire.spi.capability;

public sealed interface Step<I, O>
        permits Processor, Executor {

    O apply(I input, ExecutionContext ctx)
            throws CapabilityException;
}
EOF

cat > "$BASE_DIR/Processor.java" <<'EOF'
package io.cheshire.spi.capability;

@FunctionalInterface
public non-sealed interface Processor<I, O>
        extends Step<I, O> {}
EOF

cat > "$BASE_DIR/Executor.java" <<'EOF'
package io.cheshire.spi.capability;

@FunctionalInterface
public non-sealed interface Executor
        extends Step<CanonicalInput, CanonicalOutput> {}
EOF

# -------------------------------------------------------------------
# Capability configuration (adapter-only)
# -------------------------------------------------------------------

cat > "$BASE_DIR/CapabilityMetadata.java" <<'EOF'
package io.cheshire.spi.capability;

public record CapabilityMetadata(
        String name,
        String description,
        String uri
) {}
EOF

cat > "$BASE_DIR/CapabilityConfig.java" <<'EOF'
package io.cheshire.spi.capability;

public record CapabilityConfig(
        CapabilityMetadata metadata,
        Class<? extends CanonicalInput> inputType,
        Class<? extends CanonicalOutput> outputType,
        PipelineConfig pipeline
) {}
EOF

cat > "$BASE_DIR/CapabilityConfigAdapter.java" <<'EOF'
package io.cheshire.spi.capability;

public interface CapabilityConfigAdapter<T> {
    CapabilityConfig adapt(T rawConfig)
            throws InvalidCapabilityException;
}
EOF

# -------------------------------------------------------------------
# Errors
# -------------------------------------------------------------------

cat > "$BASE_DIR/ErrorCode.java" <<'EOF'
package io.cheshire.spi.capability;

public enum ErrorCode {
    VALIDATION_ERROR,
    EXECUTION_ERROR,
    TIMEOUT_ERROR,
    CANCELLATION_ERROR,
    CONFIGURATION_ERROR
}
EOF

cat > "$BASE_DIR/CapabilityError.java" <<'EOF'
package io.cheshire.spi.capability;

public sealed interface CapabilityError
        permits ValidationError, ExecutionError, TimeoutError, CancellationError {

    ErrorCode code();
    boolean retryable();
}
EOF

cat > "$BASE_DIR/CapabilityException.java" <<'EOF'
package io.cheshire.spi.capability;

public class CapabilityException extends Exception {

    public CapabilityException(String message) {
        super(message);
    }

    public CapabilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
EOF

cat > "$BASE_DIR/InvalidCapabilityException.java" <<'EOF'
package io.cheshire.spi.capability;

public class InvalidCapabilityException extends CapabilityException {

    public InvalidCapabilityException(String message) {
        super(message);
    }

    public InvalidCapabilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
EOF

cat > "$BASE_DIR/ValidationError.java" <<'EOF'
package io.cheshire.spi.capability;

public final class ValidationError implements CapabilityError {
    public ErrorCode code() { return ErrorCode.VALIDATION_ERROR; }
    public boolean retryable() { return false; }
}
EOF

cat > "$BASE_DIR/ExecutionError.java" <<'EOF'
package io.cheshire.spi.capability;

public final class ExecutionError implements CapabilityError {
    public ErrorCode code() { return ErrorCode.EXECUTION_ERROR; }
    public boolean retryable() { return true; }
}
EOF

cat > "$BASE_DIR/TimeoutError.java" <<'EOF'
package io.cheshire.spi.capability;

public final class TimeoutError implements CapabilityError {
    public ErrorCode code() { return ErrorCode.TIMEOUT_ERROR; }
    public boolean retryable() { return true; }
}
EOF

cat > "$BASE_DIR/CancellationError.java" <<'EOF'
package io.cheshire.spi.capability;

public final class CancellationError implements CapabilityError {
    public ErrorCode code() { return ErrorCode.CANCELLATION_ERROR; }
    public boolean retryable() { return false; }
}
EOF

# -------------------------------------------------------------------
# IDs & cancellation
# -------------------------------------------------------------------

cat > "$BASE_DIR/CapabilityId.java" <<'EOF'
package io.cheshire.spi.capability;

public record CapabilityId(String value) {}
EOF

cat > "$BASE_DIR/CancellationToken.java" <<'EOF'
package io.cheshire.spi.capability;

public interface CancellationToken {
    boolean isCancelled();
}
EOF

echo "cheshire-capability-spi (capability-based) initialized successfully."
