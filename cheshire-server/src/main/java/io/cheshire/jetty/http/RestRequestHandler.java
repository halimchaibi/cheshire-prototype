package io.cheshire.jetty.http;

import io.cheshire.core.server.RequestHandler;
import io.cheshire.core.server.RequestHandlerException;
import io.cheshire.core.server.ResponseEntity;
import io.cheshire.core.server.protocol.RequestEnvelope;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.function.Function;

/**
 * REST-specific RequestHandler.
 * Delegates processing to a pure function while enforcing
 * error boundaries and observability guarantees.
 */
@Slf4j
public final class RestRequestHandler
        implements RequestHandler<RequestEnvelope, ResponseEntity> {

    private final Function<RequestEnvelope, ResponseEntity> dispatcher;

    public RestRequestHandler(Function<RequestEnvelope, ResponseEntity> dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher must not be null");
    }

    @Override
    public ResponseEntity handle(RequestEnvelope request)
            throws RequestHandlerException {

        Objects.requireNonNull(request, "request must not be null");
        log.info("Incoming REST request [{}]", request.requestId());
        try {
            ResponseEntity response = dispatcher.apply(request);
            if (response == null) {
                throw new IllegalStateException("dispatcher returned null ResponseEntity");
            }
            return response;
        } catch (RuntimeException e) {
            log.error(
                    "Unhandled runtime error while processing request [{}]",
                    request.requestId(),
                    e
            );
            throw new RequestHandlerException("Unhandled runtime failure", e);
        } catch (Exception e) {

            log.error(
                    "Unexpected checked exception for request [{}]",
                    request.requestId(), e
            );
            throw new RequestHandlerException("Unexpected processing failure", e);
        }
    }
}
