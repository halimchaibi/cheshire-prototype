package io.cheshire.jetty.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cheshire.core.server.RequestHandlerException;
import io.cheshire.core.server.ResponseEntity;
import io.cheshire.core.server.protocol.RequestEnvelope;
import io.cheshire.jetty.utils.URL;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public final class RestAdapterServlet extends HttpServlet {

    private static final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private final transient RestRequestHandler handler;
    private final transient RestProtocolAdapter adapter = new RestProtocolAdapter();

    public RestAdapterServlet(
            RestRequestHandler handler
    ) {
        this.handler = Objects.requireNonNull(handler, "handler must not be null");
    }

    @Override
    protected void service(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException {

        try {
            //TODO: Note safe depends on path configuration for exposure: assumes /api/v{version}/{capability}/{action}
            String capability = URL.getSubpathFromPath(req, 1);
            RequestEnvelope envelope = adapter.toRequestEnvelope(req, capability);
            ResponseEntity handlerResponse = handler.handle(envelope);

            switch (handlerResponse) {
                case ResponseEntity.Success(Object response, Map<String, Object> meta) -> {
                    resp.setStatus(200);
                    Map<String, Object> finalBody = adapter.fromProcessingResult(req, handlerResponse);
                    finalBody.put("debugInfos", meta);
                    writeJson(resp, finalBody);
                }
                case ResponseEntity.Failure(var status, var err, var msg) -> {
                    resp.setStatus(mapStatusToHttp(status));
                    Map<String, Object> errorBody = new HashMap<>();
                    errorBody.put("success", false);
                    errorBody.put("type", status.name());
                    errorBody.put("message", msg);
                    if (err != null) {
                        errorBody.put("debugInfos", Map.of(
                                "exception", err.getClass().getSimpleName(),
                                "detail", err.getMessage() != null ? err.getMessage() : "No detailed message"
                        ));
                    }

                    writeJson(resp, errorBody);
                }
            }
        } catch (RequestHandlerException e) {
            log.error("Request handling failed", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected servlet failure", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
        }
    }

    private void sendError(
            HttpServletResponse resp,
            int status,
            String message
    ) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.getWriter().write("""
                { "error": "%s" }
                """.formatted(message));
    }

    private int mapStatusToHttp(ResponseEntity.Status status) {
        return switch (status) {
            case SUCCESS -> 200;
            case BAD_REQUEST -> 400;
            case UNAUTHORIZED -> 401;
            case FORBIDDEN -> 403;
            case NOT_FOUND -> 404;
            case EXECUTION_FAILED -> 500;
            case SERVICE_UNAVAILABLE -> 503;
        };
    }

    private void writeJson(HttpServletResponse resp, Object payload) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        mapper.writerWithDefaultPrettyPrinter().writeValue(resp.getWriter(), payload);
    }
}
