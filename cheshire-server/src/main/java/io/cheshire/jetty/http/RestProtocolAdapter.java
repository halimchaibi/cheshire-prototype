/*-
 * #%L
 * Cheshire :: Servers
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.jetty.http;

import io.cheshire.core.server.ResponseEntity;
import io.cheshire.core.server.protocol.*;
import io.cheshire.jetty.utils.URL;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;

@Slf4j
public class RestProtocolAdapter implements ProtocolAdapter<HttpServletRequest, Map<String, Object>> {

    @Override
    public RequestEnvelope toRequestEnvelope(HttpServletRequest req, String capability)
            throws ProtocolAdapterException {
        try {
            // TODO: Note safe assumes /{capability}/{action}
            String action = URL.getSubpathFromPath(req, 2);

            Map<String, String> headers = new HashMap<>();
            req.getHeaderNames().asIterator().forEachRemaining(name -> headers.put(name, req.getHeader(name)));

            ProtocolMetadata protocolMeta = new ProtocolMetadata("REST", "1.0", headers, req.getRequestURI(),
                    req.getMethod());

            Map<String, Object> queryParams = new HashMap<>();
            req.getParameterMap().forEach((k, v) -> queryParams.put(k, v.length == 1 ? v[0] : v));

            RequestPayload payload = new RequestPayload(determinePayloadType(req.getContentType()), parseBody(req), // The
                                                                                                                    // raw
                                                                                                                    // data
                                                                                                                    // object
                    queryParams, // The parameters map
                    Map.of() // Key
            );

            return new RequestEnvelope(UUID.randomUUID().toString(), capability, action, protocolMeta, payload,
                    // TODO: Empty context for now, needs to be fully created here.
                    RequestContext.empty(), Instant.now());

        } catch (Exception e) {
            throw new ProtocolAdapterException("REST Inbound mapping failed", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromProcessingResult(HttpServletRequest request, ResponseEntity result)
            throws ProtocolAdapterException {
        return (T) switch (result) {
        case ResponseEntity.Success(Object data, var meta) -> {
            var response = new LinkedHashMap<String, Object>();
            response.put("success", true);
            switch (data) {
            case Map<?, ?> dataMap -> dataMap.forEach((k, v) -> response.put(String.valueOf(k), v));
            case Collection<?> list -> {
                response.put("result", list);
                response.put("pagination", Map.of("totalRows", list.size()));
            }
            case null -> response.put("data", null);
            default -> response.put("data", data);
            }

            if (meta.containsKey("DEBUG")) {
                response.put("debug", meta.get("DEBUG"));
            }

            yield response;
        }

        case ResponseEntity.Failure f -> {
            LinkedHashMap<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("error", Map.of("type", f.status(), "message", f.message()));
            yield response;
        }
        };
    }

    @Override
    public String getProtocolType() {
        return "REST";
    }

    /**
     * Defines the MIME types or content types supported by this adapter. Defaults to {@code application/json}.
     *
     * @return An array of supported content type strings.
     */
    @Override
    public String[] getSupportedContentTypes() {
        return ProtocolAdapter.super.getSupportedContentTypes();
    }

    private RequestPayload.PayloadType determinePayloadType(String contentType) {
        if (contentType == null)
            return RequestPayload.PayloadType.EMPTY;
        if (contentType.contains("json"))
            return RequestPayload.PayloadType.JSON;
        if (contentType.contains("xml"))
            return RequestPayload.PayloadType.XML;
        return RequestPayload.PayloadType.UNKNOWN;
    }

    private Object parseBody(HttpServletRequest request) {
        return new HashMap<String, Object>();
    }
}
