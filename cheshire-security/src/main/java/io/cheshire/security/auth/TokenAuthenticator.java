package io.cheshire.security.auth;

import io.cheshire.security.Principal;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token-based authenticator implementation.
 * Supports simple token validation with configurable token-to-principal mapping.
 */
@Slf4j
@Getter
@Builder
public class TokenAuthenticator implements Authenticator {

    /**
     * Map of valid tokens to principal information.
     * In production, this would typically query a database or external service.
     */
    @Builder.Default
    private final Map<String, PrincipalInfo> tokenRegistry = new ConcurrentHashMap<>();

    /**
     * Header name to extract token from (e.g., "Authorization", "X-API-Key").
     */
    @Builder.Default
    private final String tokenHeader = "Authorization";

    /**
     * Prefix to strip from token (e.g., "Bearer ").
     */
    @Builder.Default
    private final String tokenPrefix = "Bearer ";

    /**
     * Whether to require authentication (if false, missing tokens are allowed).
     */
    @Builder.Default
    private final boolean requireAuth = false;

    /**
     * Registers a token with associated principal information.
     */
    public void registerToken(String token, PrincipalInfo principalInfo) {
        tokenRegistry.put(token, principalInfo);
        log.debug("Registered token for principal: {}", principalInfo.getId());
    }

    /**
     * Removes a token from the registry.
     */
    public void removeToken(String token) {
        tokenRegistry.remove(token);
        log.debug("Removed token from registry");
    }

    @Override
    public CompletableFuture<Principal> authenticate(AuthenticationContext context) {
        return CompletableFuture.supplyAsync(() -> {
            // Extract token from context
            String token = extractToken(context);

            if (token == null || token.isEmpty()) {
                if (requireAuth) {
                    throw new AuthenticationException("Authentication required but no token provided");
                }
                // Return anonymous principal if auth is not required
                return Principal.builder()
                        .id("anonymous")
                        .type("Anonymous")
                        .build();
            }

            // Look up principal for token
            PrincipalInfo principalInfo = tokenRegistry.get(token);
            if (principalInfo == null) {
                throw new AuthenticationException("Invalid or expired token");
            }

            // Build principal from token info
            Principal principal = Principal.builder()
                    .id(principalInfo.getId())
                    .type(principalInfo.getType())
                    .roles(principalInfo.getRoles())
                    .attributes(principalInfo.getAttributes())
                    .build();

            log.debug("Authenticated principal: {} with token", principal.getId());
            return principal;
        });
    }

    @Override
    public boolean canAuthenticate(AuthenticationContext context) {
        return context.hasCredential(tokenHeader) || !requireAuth;
    }

    /**
     * Extracts the token from the authentication context.
     */
    private String extractToken(AuthenticationContext context) {
        String token = context.getCredential(tokenHeader).orElse(null);
        if (token == null) {
            return null;
        }

        // Strip prefix if present
        if (tokenPrefix != null && !tokenPrefix.isEmpty() && token.startsWith(tokenPrefix)) {
            token = token.substring(tokenPrefix.length());
        }

        return token.trim();
    }

    /**
     * Internal class for storing principal information associated with tokens.
     */
    @Getter
    @Builder
    public static class PrincipalInfo {
        private final String id;
        @Builder.Default
        private final String type = "User";
        @Builder.Default
        private final Set<String> roles = Set.of();
        @Builder.Default
        private final Map<String, Object> attributes = Map.of();
    }
}

