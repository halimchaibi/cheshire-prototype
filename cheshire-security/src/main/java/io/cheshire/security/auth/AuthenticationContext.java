package io.cheshire.security.auth;

import io.cheshire.security.Principal;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;

/**
 * Context containing authentication information extracted from a request.
 */
@Getter
@Builder
public class AuthenticationContext {

    /**
     * Raw credentials extracted from the request (e.g., token, API key).
     */
    private final Map<String, String> credentials;

    /**
     * Authentication type (e.g., "token", "bearer", "api-key").
     */
    private final String authType;

    /**
     * Optional principal if authentication has already been performed.
     */
    private final Principal principal;

    /**
     * Gets a credential value by key.
     */
    public Optional<String> getCredential(String key) {
        if (credentials == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(credentials.get(key));
    }

    /**
     * Checks if this context has a specific credential.
     */
    public boolean hasCredential(String key) {
        return credentials != null && credentials.containsKey(key);
    }
}

