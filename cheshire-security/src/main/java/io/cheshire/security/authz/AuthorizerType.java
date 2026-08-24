package io.cheshire.security.authz;

/**
 * Type of authorizer to use for authorization.
 */
public enum AuthorizerType {
    /**
     * External Cedar Policy service for complex policy evaluation.
     * Requires a Cedar Policy service to be running.
     */
    CEDAR,

    /**
     * Simple local authorizer with basic RBAC support.
     * No external service required - policies are evaluated locally.
     * Suitable for development, testing, and simple production use cases.
     */
    SIMPLE,

    /**
     * No authorization - authentication only.
     * All authenticated users are allowed to perform actions.
     * Useful when you only need authentication without authorization checks.
     */
    NONE
}

