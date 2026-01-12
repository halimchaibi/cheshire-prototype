package io.cheshire.core.server;

import io.cheshire.core.CheshireSession;
import io.cheshire.core.SessionContext;
import io.cheshire.core.TaskBuilder;
import io.cheshire.core.TaskResult;
import io.cheshire.core.server.protocol.RequestEnvelope;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;

/**
 * Clean, stateless-ish dispatcher that manages Task execution.
 */
@Slf4j
public record HttpDispatcher(CheshireSession session) implements CheshireDispatcher {
    @Override
    public ResponseEntity dispatch(RequestEnvelope envelope) {

        log.info("Dispatching request for {}", envelope.toString());
        try {
            SessionContext ctx = new SessionContext(
                    envelope.context().sessionId(),
                    envelope.context().userId(),
                    envelope.context().traceId(),
                    envelope.context().securityContext(),
                    envelope.context().attributes(),
                    Instant.now(),
                    envelope.context().deadline()
            );

            TaskResult result = session.execute(TaskBuilder.from(envelope).build(), ctx);

            return switch (result) {
                case TaskResult.Success(Object output, Map<String, Object> metadata) ->
                        ResponseEntity.ok(output, metadata);

                case TaskResult.Failure(ResponseEntity.Status status, Throwable cause, _) ->
                        ResponseEntity.error(status, cause);
            };

        } catch (Exception e) {
            log.debug("Dispatch execution failed", e);
            return ResponseEntity.error(ResponseEntity.Status.EXECUTION_FAILED, e);
        }
    }
}
