package io.cheshire.security.authz;

import io.cheshire.security.Principal;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Cedar Policy-based authorizer implementation.
 */
@Slf4j
@Getter
@Builder
public class CedarAuthorizer implements Authorizer {

    private final CedarPolicyClient cedarClient;

    /**
     * Default action to use if not specified (e.g., "execute").
     */
    @Builder.Default
    private final String defaultAction = "execute";

    /**
     * Whether to allow requests when Cedar service is unavailable.
     * If false, throws exception when service is unavailable.
     */
    @Builder.Default
    private final boolean allowOnServiceUnavailable = false;

    public CedarAuthorizer(CedarPolicyClient cedarClient, String defaultAction, boolean allowOnServiceUnavailable) {
        this.cedarClient = cedarClient;
        this.defaultAction = defaultAction != null ? defaultAction : "execute";
        this.allowOnServiceUnavailable = allowOnServiceUnavailable;
    }

    @Override
    public CompletableFuture<Boolean> authorize(
            Principal principal,
            String action,
            String resource,
            Map<String, Object> context
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // Check if Cedar service is available
            if (!cedarClient.isAvailable()) {
                if (allowOnServiceUnavailable) {
                    log.warn("Cedar Policy service unavailable, allowing request by default");
                    return true;
                } else {
                    throw new AuthorizationException("Cedar Policy service is unavailable");
                }
            }

            // Use default action if not provided
            String effectiveAction = action != null ? action : defaultAction;

            // Build authorization request (Cedar-specific format)
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .principal(principal.toEntityIdentifier())  // Uses "Type::id" format compatible with Cedar
                    .action(effectiveAction)
                    .resource(resource)
                    .context(context != null ? context : Map.of())
                    .build();

            log.debug("Authorizing request: principal={}, action={}, resource={}",
                    principal.toEntityIdentifier(), effectiveAction, resource);

            // Call Cedar Policy service
            AuthorizationResponse response = cedarClient.authorizeSync(request);

            boolean allowed = response.isAllowed();

            if (allowed) {
                log.debug("Authorization granted: {} can {} on {}",
                        principal.toEntityIdentifier(), effectiveAction, resource);
            } else {
                log.warn("Authorization denied: {} cannot {} on {} (reason: {})",
                        principal.toEntityIdentifier(), effectiveAction, resource,
                        response.getReason());
            }

            return allowed;
        });
    }
}

