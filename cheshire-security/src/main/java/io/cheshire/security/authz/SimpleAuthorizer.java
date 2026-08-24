package io.cheshire.security.authz;

import io.cheshire.security.Principal;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Simple local authorizer with basic RBAC support.
 *
 * <p>This authorizer evaluates policies locally without requiring an external service.
 * It supports role-based access control (RBAC) with configurable default behavior.
 *
 * <p>Use cases:
 * <ul>
 *   <li>Development and testing environments</li>
 *   <li>Simple production deployments with basic RBAC needs</li>
 *   <li>Scenarios where external policy service is not available</li>
 * </ul>
 */
@Slf4j
@Getter
@Builder
public class SimpleAuthorizer implements Authorizer {

    /**
     * Default behavior when no rule matches.
     */
    @Builder.Default
    private final Behavior defaultBehavior = Behavior.ROLE_BASED;
    /**
     * Role that has full access to all capabilities.
     * If a principal has this role, all authorization checks will pass.
     * Common values: "admin", "superuser", "root".
     */
    @Builder.Default
    private final String adminRole = "admin";
    /**
     * Set of roles that are allowed to execute all capabilities.
     * Principals with any of these roles will have full access.
     */
    @Builder.Default
    private final Set<String> privilegedRoles = Set.of("admin");
    /**
     * Map of capability names to required roles.
     * Format: "capabilityName" -> Set of roles that can execute it.
     * If a capability is not in this map, default behavior applies.
     */
    @Builder.Default
    private final Map<String, Set<String>> capabilityRoles = Map.of();

    @Override
    public CompletableFuture<Boolean> authorize(
            Principal principal,
            String action,
            String resource,
            Map<String, Object> context
    ) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Authorizing request: principal={}, action={}, resource={}",
                    principal.toEntityIdentifier(), action, resource);

            // Extract capability name from resource (format: "Capability::name")
            String capabilityName = extractCapabilityName(resource);

            // Check if principal has admin/privileged role
            if (hasPrivilegedRole(principal)) {
                log.debug("Authorization granted: principal {} has privileged role",
                        principal.toEntityIdentifier());
                return true;
            }

            // Check capability-specific rules
            if (capabilityName != null && capabilityRoles.containsKey(capabilityName)) {
                Set<String> requiredRoles = capabilityRoles.get(capabilityName);
                if (hasAnyRole(principal, requiredRoles)) {
                    log.debug("Authorization granted: principal {} has required role for capability {}",
                            principal.toEntityIdentifier(), capabilityName);
                    return true;
                } else {
                    log.warn("Authorization denied: principal {} lacks required roles {} for capability {}",
                            principal.toEntityIdentifier(), requiredRoles, capabilityName);
                    return false;
                }
            }

            // Apply default behavior
            boolean allowed = evaluateDefaultBehavior(principal);

            if (allowed) {
                log.debug("Authorization granted by default behavior: {}", defaultBehavior);
            } else {
                log.warn("Authorization denied by default behavior: {}", defaultBehavior);
            }

            return allowed;
        });
    }

    /**
     * Extracts capability name from resource identifier.
     * Expected format: "Capability::name" -> "name"
     */
    private String extractCapabilityName(String resource) {
        if (resource == null || !resource.startsWith("Capability::")) {
            return null;
        }
        return resource.substring("Capability::".length());
    }

    /**
     * Checks if principal has a privileged role (admin or in privilegedRoles set).
     */
    private boolean hasPrivilegedRole(Principal principal) {
        Set<String> roles = principal.getRoles();
        if (roles == null || roles.isEmpty()) {
            return false;
        }

        // Check admin role
        if (adminRole != null && roles.contains(adminRole)) {
            return true;
        }

        // Check privileged roles
        if (privilegedRoles != null && !privilegedRoles.isEmpty()) {
            for (String role : roles) {
                if (privilegedRoles.contains(role)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if principal has any of the required roles.
     */
    private boolean hasAnyRole(Principal principal, Set<String> requiredRoles) {
        if (requiredRoles == null || requiredRoles.isEmpty()) {
            return true; // No roles required
        }

        Set<String> principalRoles = principal.getRoles();
        if (principalRoles == null || principalRoles.isEmpty()) {
            return false;
        }

        for (String role : requiredRoles) {
            if (principalRoles.contains(role)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Evaluates default behavior based on configuration.
     */
    private boolean evaluateDefaultBehavior(Principal principal) {
        switch (defaultBehavior) {
            case ALLOW_ALL:
                return true;
            case DENY_ALL:
                return false;
            case ROLE_BASED:
                // In role-based mode without specific rules, allow if principal has any roles
                // This is a permissive default - you can make it stricter if needed
                Set<String> roles = principal.getRoles();
                return roles != null && !roles.isEmpty();
            default:
                log.warn("Unknown default behavior: {}, denying access", defaultBehavior);
                return false;
        }
    }

    /**
     * Default behavior when no specific rule matches.
     */
    public enum Behavior {
        /**
         * Allow all requests (permissive mode).
         * Useful for development or when authorization is handled elsewhere.
         */
        ALLOW_ALL,

        /**
         * Deny all requests (strict mode).
         * Only explicitly allowed requests will be permitted.
         */
        DENY_ALL,

        /**
         * Use role-based rules.
         * Requests are evaluated based on principal roles.
         */
        ROLE_BASED
    }
}

