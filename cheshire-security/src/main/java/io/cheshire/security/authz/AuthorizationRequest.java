package io.cheshire.security.authz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Request model for authorization.
 * This is a generic authorization request structure that can be used with various policy engines.
 * The current implementation is optimized for Cedar Policy but the structure is policy-agnostic.
 */
@Getter
@Builder
public class AuthorizationRequest {

    /**
     * Principal making the request (e.g., "User::alice").
     */
    @JsonProperty("principal")
    private final String principal;

    /**
     * Action being requested (e.g., "execute", "read", "write").
     */
    @JsonProperty("action")
    private final String action;

    /**
     * Resource being accessed (e.g., "Capability::query_users", "Resource::database1").
     */
    @JsonProperty("resource")
    private final String resource;

    /**
     * Additional context attributes for ABAC (e.g., IP, timestamp, environment).
     */
    @JsonProperty("context")
    @Builder.Default
    private final Map<String, Object> context = Map.of();
}

