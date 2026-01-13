package io.cheshire.security;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local security context for storing authenticated principal information.
 */
@Slf4j
public class SecurityContext {

    private static final ThreadLocal<Principal> context = new ThreadLocal<>();

    /**
     * Gets the current principal from the security context.
     *
     * @return the current principal, or null if not set
     */
    public static Principal getPrincipal() {
        return context.get();
    }

    /**
     * Sets the current principal in the security context.
     */
    public static void setPrincipal(Principal principal) {
        context.set(principal);
        log.debug("Security context set for principal: {}", principal);
    }

    /**
     * Checks if a principal is currently authenticated.
     *
     * @return true if a principal is set, false otherwise
     */
    public static boolean isAuthenticated() {
        return context.get() != null;
    }

    /**
     * Clears the security context for the current thread.
     */
    public static void clear() {
        Principal principal = context.get();
        context.remove();
        if (principal != null) {
            log.debug("Security context cleared for principal: {}", principal);
        }
    }

    /**
     * Executes a runnable with the given principal in the security context.
     * The context is automatically cleared after execution.
     */
    public static void withPrincipal(Principal principal, Runnable runnable) {
        Principal previous = context.get();
        try {
            setPrincipal(principal);
            runnable.run();
        } finally {
            if (previous != null) {
                setPrincipal(previous);
            } else {
                clear();
            }
        }
    }
}

