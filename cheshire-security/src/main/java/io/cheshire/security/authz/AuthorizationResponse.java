package io.cheshire.security.authz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Map;

/**
 * Response model from authorization service.
 * This is a generic authorization response structure that can be used with various policy engines.
 * The current implementation is optimized for Cedar Policy but the structure is policy-agnostic.
 */
@Getter
public class AuthorizationResponse {

    /**
     * Whether the authorization request is allowed.
     */
    @JsonProperty("decision")
    private String decision;

    /**
     * Optional reason for the decision.
     */
    @JsonProperty("reason")
    private String reason;

    /**
     * Optional diagnostics information.
     */
    @JsonProperty("diagnostics")
    private Map<String, Object> diagnostics;

    /**
     * Checks if the authorization decision is "Allow".
     */
    public boolean isAllowed() {
        return "Allow".equalsIgnoreCase(decision);
    }

    /**
     * Checks if the authorization decision is "Deny".
     */
    public boolean isDenied() {
        return "Deny".equalsIgnoreCase(decision);
    }
}

