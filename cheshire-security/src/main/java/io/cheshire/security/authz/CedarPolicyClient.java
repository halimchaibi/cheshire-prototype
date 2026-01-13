package io.cheshire.security.authz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.Request;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for communicating with Cedar Policy service.
 */
@Slf4j
@Getter
@Builder
public class CedarPolicyClient {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /**
     * Base URL of the Cedar Policy service (e.g., "http://localhost:8080").
     */
    private final String serviceUrl;

    /**
     * Connection timeout in milliseconds.
     */
    @Builder.Default
    private final long connectTimeoutMs = 5000;

    /**
     * Read timeout in milliseconds.
     */
    @Builder.Default
    private final long readTimeoutMs = 10000;

    /**
     * HTTP client instance.
     */
    private final OkHttpClient httpClient;

    /**
     * Creates a new CedarPolicyClient with the given service URL.
     */
    public CedarPolicyClient(String serviceUrl, long connectTimeoutMs, long readTimeoutMs, OkHttpClient httpClient) {
        this.serviceUrl = serviceUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.httpClient = httpClient != null ? httpClient : new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Authorizes a request using Cedar Policy.
     *
     * @param request the authorization request
     * @return the authorization response
     * @throws IOException if communication with Cedar service fails
     */
    public AuthorizationResponse authorize(AuthorizationRequest request) throws IOException {
        String url = serviceUrl + "/authorize";

        // Serialize request to JSON
        String jsonBody = objectMapper.writeValueAsString(request);
        log.debug("Sending authorization request to {}: {}", url, jsonBody);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request httpRequest = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                log.error("Cedar Policy service returned error: {} - {}", response.code(), errorBody);
                throw new IOException("Cedar Policy service error: " + response.code() + " - " + errorBody);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Empty response from Cedar Policy service");
            }

            String responseJson = responseBody.string();
            log.debug("Received authorization response: {}", responseJson);

            AuthorizationResponse authzResponse = objectMapper.readValue(
                    responseJson,
                    AuthorizationResponse.class
            );

            return authzResponse;
        }
    }

    /**
     * Authorizes a request synchronously, wrapping IOException in AuthorizationException.
     */
    public AuthorizationResponse authorizeSync(AuthorizationRequest request) {
        try {
            return authorize(request);
        } catch (IOException e) {
            log.error("Failed to communicate with Cedar Policy service", e);
            throw new AuthorizationException("Failed to authorize request: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if the Cedar Policy service is available.
     *
     * @return true if the service is reachable, false otherwise
     */
    public boolean isAvailable() {
        try {
            String url = serviceUrl + "/health";
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.debug("Cedar Policy service health check failed", e);
            return false;
        }
    }
}

