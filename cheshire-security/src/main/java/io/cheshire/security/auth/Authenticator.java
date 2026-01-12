package io.cheshire.security.auth;

import io.cheshire.security.Principal;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for pluggable authentication implementations.
 */
public interface Authenticator {

    /**
     * Authenticates a request based on the authentication context.
     *
     * @param context the authentication context containing credentials
     * @return a CompletableFuture that completes with the authenticated principal
     * @throws AuthenticationException if authentication fails
     */
    CompletableFuture<Principal> authenticate(AuthenticationContext context);

    /**
     * Authenticates a request synchronously.
     *
     * @param context the authentication context containing credentials
     * @return the authenticated principal
     * @throws AuthenticationException if authentication fails
     */
    default Principal authenticateSync(AuthenticationContext context) {
        try {
            return authenticate(context).get();
        } catch (Exception e) {
            if (e.getCause() instanceof AuthenticationException) {
                throw (AuthenticationException) e.getCause();
            }
            throw new AuthenticationException("Authentication failed", e);
        }
    }

    /**
     * Checks if this authenticator can handle the given authentication context.
     *
     * @param context the authentication context to check
     * @return true if this authenticator can handle the context, false otherwise
     */
    boolean canAuthenticate(AuthenticationContext context);
}

