package io.cheshire.security.authz;

import io.cheshire.security.config.SecurityConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating Authorizer instances based on configuration.
 */
@Slf4j
public class AuthorizerFactory {

    /**
     * Creates an appropriate Authorizer based on the security configuration.
     *
     * @param securityConfig the security configuration
     * @return an Authorizer instance, or null if authorization is disabled (type is NONE)
     */
    public static Authorizer createAuthorizer(SecurityConfig securityConfig) {
        if (securityConfig == null || !securityConfig.isEnabled()) {
            log.debug("Security disabled, no authorizer created");
            return null;
        }

        SecurityConfig.AuthorizationConfig authzConfig = securityConfig.getAuthorization();
        AuthorizerType type = authzConfig.getType();

        if (type == null) {
            // Default to SIMPLE if not specified
            type = AuthorizerType.SIMPLE;
            log.debug("No authorizer type specified, defaulting to SIMPLE");
        }

        switch (type) {
            case CEDAR:
                return createCedarAuthorizer(securityConfig, authzConfig);

            case SIMPLE:
                return createSimpleAuthorizer(authzConfig);

            case NONE:
                log.info("Authorization disabled (type: NONE) - authentication only");
                return null;

            default:
                log.warn("Unknown authorizer type: {}, defaulting to SIMPLE", type);
                return createSimpleAuthorizer(authzConfig);
        }
    }

    /**
     * Creates a CedarAuthorizer instance.
     */
    private static Authorizer createCedarAuthorizer(
            SecurityConfig securityConfig,
            SecurityConfig.AuthorizationConfig authzConfig
    ) {
        SecurityConfig.CedarConfig cedarConfig = securityConfig.getCedar();

        CedarPolicyClient cedarClient = CedarPolicyClient.builder()
                .serviceUrl(cedarConfig.getServiceUrl())
                .connectTimeoutMs(cedarConfig.getConnectTimeoutMs())
                .readTimeoutMs(cedarConfig.getReadTimeoutMs())
                .build();

        CedarAuthorizer authorizer = CedarAuthorizer.builder()
                .cedarClient(cedarClient)
                .defaultAction(authzConfig.getDefaultAction())
                .allowOnServiceUnavailable(authzConfig.isAllowOnServiceUnavailable())
                .build();

        log.info("Created CedarAuthorizer - service at {}", cedarConfig.getServiceUrl());
        return authorizer;
    }

    /**
     * Creates a SimpleAuthorizer instance.
     */
    private static Authorizer createSimpleAuthorizer(SecurityConfig.AuthorizationConfig authzConfig) {
        SecurityConfig.SimpleAuthorizerConfig simpleConfig = authzConfig.getSimple();

        SimpleAuthorizer authorizer = SimpleAuthorizer.builder()
                .defaultBehavior(simpleConfig.getDefaultBehavior())
                .adminRole(simpleConfig.getAdminRole())
                .privilegedRoles(simpleConfig.getPrivilegedRoles())
                .capabilityRoles(simpleConfig.getCapabilityRoles())
                .build();

        log.info("Created SimpleAuthorizer - default behavior: {}, admin role: {}",
                simpleConfig.getDefaultBehavior(), simpleConfig.getAdminRole());
        return authorizer;
    }
}

