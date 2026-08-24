package io.cheshire.security;

import io.cheshire.security.auth.AuthenticationContext;
import io.cheshire.security.auth.Authenticator;
import io.cheshire.security.auth.TokenAuthenticator;
import io.cheshire.security.config.SecurityConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for security operations.
 */
@Slf4j
public class SecurityUtils {

    /**
     * Creates an authenticator based on security configuration.
     *
     * <p>Supported authenticator types:
     * <ul>
     *   <li>"token" - Token-based authenticator with in-memory registry</li>
     *   <li>"jwt" - JWT-based authenticator (requires io.cheshire.examples.security.SimpleJwtAuthenticator)</li>
     * </ul>
     *
     * <p>For custom authenticators, implement the {@link Authenticator} interface
     * and create instances directly rather than using this factory method.
     */
    public static Authenticator createAuthenticator(SecurityConfig securityConfig) {
        if (securityConfig == null || !securityConfig.isEnabled()) {
            return null;
        }

        SecurityConfig.AuthenticationConfig authConfig = securityConfig.getAuthentication();
        String authType = authConfig.getType();

        if ("token".equals(authType)) {
            return TokenAuthenticator.builder()
                    .tokenHeader(authConfig.getTokenHeader())
                    .tokenPrefix(authConfig.getTokenPrefix())
                    .requireAuth(authConfig.isRequireAuth())
                    .build();
        }

        if ("jwt".equals(authType)) {
            // Use reflection to load SimpleJwtAuthenticator from examples module
            // This allows the security module to remain independent
            try {
                Class<?> jwtAuthClass = Class.forName("io.cheshire.examples.security.SimpleJwtAuthenticator");
                Object jwtAuth = jwtAuthClass.getMethod("builder").invoke(null);
                // Configure builder if needed
                Authenticator authenticator = (Authenticator) jwtAuthClass.getMethod("build").invoke(jwtAuth);
                log.info("Successfully loaded SimpleJwtAuthenticator for JWT authentication");
                return authenticator;
            } catch (ClassNotFoundException e) {
                log.error("SimpleJwtAuthenticator class not found. Make sure cheshire-examples module is on the classpath. " +
                        "Falling back to TokenAuthenticator. Full error: ", e);
                return TokenAuthenticator.builder().build();
            } catch (Exception e) {
                log.error("Failed to load SimpleJwtAuthenticator. Error: ", e);
                return TokenAuthenticator.builder().build();
            }
        }

        log.warn("Unknown authentication type: {}, using token authenticator", authType);
        return TokenAuthenticator.builder().build();
    }

    /**
     * Extracts authentication context from a map of headers/credentials.
     */
    public static AuthenticationContext extractAuthContext(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return AuthenticationContext.builder()
                    .credentials(Map.of())
                    .authType("none")
                    .build();
        }

        // Check for Authorization header
        String authHeader = headers.get("Authorization");
        if (authHeader != null) {
            Map<String, String> credentials = new HashMap<>();
            credentials.put("Authorization", authHeader);
            return AuthenticationContext.builder()
                    .credentials(credentials)
                    .authType("bearer")
                    .build();
        }

        // Check for API key header
        String apiKey = headers.get("X-API-Key");
        if (apiKey != null) {
            Map<String, String> credentials = new HashMap<>();
            credentials.put("X-API-Key", apiKey);
            return AuthenticationContext.builder()
                    .credentials(credentials)
                    .authType("api-key")
                    .build();
        }

        return AuthenticationContext.builder()
                .credentials(headers)
                .authType("unknown")
                .build();
    }

    /**
     * Authenticates a request and sets the security context.
     *
     * @param authenticator the authenticator to use
     * @param authContext   the authentication context
     * @return the authenticated principal, or null if authentication is not required
     */
    public static Principal authenticateAndSetContext(
            Authenticator authenticator,
            AuthenticationContext authContext
    ) {
        if (authenticator == null) {
            log.debug("No authenticator configured, skipping authentication");
            return null;
        }

        if (!authenticator.canAuthenticate(authContext)) {
            log.debug("Authenticator cannot handle authentication context");
            return null;
        }

        try {
            Principal principal = authenticator.authenticateSync(authContext);
            SecurityContext.setPrincipal(principal);
            return principal;
        } catch (Exception e) {
            log.error("Authentication failed", e);
            throw e;
        }
    }
}

