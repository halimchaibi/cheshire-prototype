package io.cheshire.security.authz;

import io.cheshire.security.Principal;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for authorization implementations.
 */
public interface Authorizer {

    /**
     * Authorizes a request to perform an action on a resource.
     *
     * @param principal the authenticated principal
     * @param action    the action being requested (e.g., "execute", "read", "write")
     * @param resource  the resource identifier (e.g., "Capability::query_users", "Resource::database1")
     * @param context   additional context attributes for ABAC
     * @return a CompletableFuture that completes with true if authorized, false otherwise
     * @throws AuthorizationException if authorization check fails
     */
    CompletableFuture<Boolean> authorize(
            Principal principal,
            String action,
            String resource,
            Map<String, Object> context
    );

    /**
     * Authorizes a request synchronously.
     *
     * @param principal the authenticated principal
     * @param action    the action being requested
     * @param resource  the resource identifier
     * @param context   additional context attributes
     * @return true if authorized, false otherwise
     * @throws AuthorizationException if authorization check fails or access is denied
     */
    default boolean authorizeSync(
            Principal principal,
            String action,
            String resource,
            Map<String, Object> context
    ) {
        try {
            Boolean result = authorize(principal, action, resource, context).get();
            if (!result) {
                throw new AuthorizationException(
                        String.format("Access denied: %s cannot %s on %s",
                                principal.toEntityIdentifier(), action, resource)
                );
            }
            return true;
        } catch (Exception e) {
            if (e.getCause() instanceof AuthorizationException) {
                throw (AuthorizationException) e.getCause();
            }
            if (e instanceof AuthorizationException) {
                throw (AuthorizationException) e;
            }
            throw new AuthorizationException("Authorization check failed", e);
        }
    }
}

