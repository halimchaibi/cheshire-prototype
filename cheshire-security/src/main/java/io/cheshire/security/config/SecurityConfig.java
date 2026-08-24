package io.cheshire.security.config;

import io.cheshire.security.authz.AuthorizerType;
import io.cheshire.security.authz.SimpleAuthorizer;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * Configuration for security features (authentication and authorization).
 */
@Getter
@Builder
public class SecurityConfig {

    /**
     * Whether security is enabled.
     */
    @Builder.Default
    private final boolean enabled = false;

    /**
     * Cedar Policy service configuration.
     */
    @Builder.Default
    private final CedarConfig cedar = CedarConfig.builder().build();

    /**
     * Authentication configuration.
     */
    @Builder.Default
    private final AuthenticationConfig authentication = AuthenticationConfig.builder().build();

    /**
     * Authorization configuration.
     */
    @Builder.Default
    private final AuthorizationConfig authorization = AuthorizationConfig.builder().build();

    /**
     * Creates a default security configuration with security disabled.
     */
    public static SecurityConfig defaults() {
        return SecurityConfig.builder().build();
    }

    /**
     * Creates a default security configuration with security enabled.
     */
    public static SecurityConfig enabled() {
        return SecurityConfig.builder()
                .enabled(true)
                .build();
    }

    /**
     * Cedar Policy service configuration.
     */
    @Getter
    @Builder
    public static class CedarConfig {
        /**
         * Base URL of the Cedar Policy service.
         */
        @Builder.Default
        private final String serviceUrl = "http://localhost:8080";

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
    }

    /**
     * Authentication configuration.
     */
    @Getter
    @Builder
    public static class AuthenticationConfig {
        /**
         * Authentication type (e.g., "token", "bearer", "api-key").
         */
        @Builder.Default
        private final String type = "token";

        /**
         * Header name for token extraction (e.g., "Authorization").
         */
        @Builder.Default
        private final String tokenHeader = "Authorization";

        /**
         * Token prefix to strip (e.g., "Bearer ").
         */
        @Builder.Default
        private final String tokenPrefix = "Bearer ";

        /**
         * Whether authentication is required.
         */
        @Builder.Default
        private final boolean requireAuth = false;
    }

    /**
     * Authorization configuration.
     */
    @Getter
    @Builder
    public static class AuthorizationConfig {
        /**
         * Type of authorizer to use.
         */
        @Builder.Default
        private final AuthorizerType type = AuthorizerType.SIMPLE;

        /**
         * Default action to use if not specified.
         */
        @Builder.Default
        private final String defaultAction = "execute";

        /**
         * Whether to allow requests when Cedar service is unavailable.
         * Only applies when type is CEDAR.
         */
        @Builder.Default
        private final boolean allowOnServiceUnavailable = false;

        /**
         * Configuration for SimpleAuthorizer.
         * Only used when type is SIMPLE.
         */
        @Builder.Default
        private final SimpleAuthorizerConfig simple = SimpleAuthorizerConfig.builder().build();
    }

    /**
     * Configuration for SimpleAuthorizer.
     */
    @Getter
    @Builder
    public static class SimpleAuthorizerConfig {
        /**
         * Default behavior when no specific rule matches.
         */
        @Builder.Default
        private final SimpleAuthorizer.Behavior defaultBehavior = SimpleAuthorizer.Behavior.ROLE_BASED;

        /**
         * Role that has full access to all capabilities (e.g., "admin").
         */
        @Builder.Default
        private final String adminRole = "admin";

        /**
         * Set of roles that are allowed to execute all capabilities.
         */
        @Builder.Default
        private final Set<String> privilegedRoles = Set.of("admin");

        /**
         * Map of capability names to required roles.
         * Format: "capabilityName" -> Set of roles that can execute it.
         */
        @Builder.Default
        private final Map<String, Set<String>> capabilityRoles = Map.of();
    }
}

