package io.cheshire.security;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * Represents an authenticated principal (user, service, etc.).
 */
@Getter
@Builder
public class Principal {

    /**
     * Unique identifier for the principal (e.g., user ID, service name).
     */
    private final String id;

    /**
     * Type of principal (e.g., "User", "Service", "Role").
     */
    @Builder.Default
    private final String type = "User";

    /**
     * Roles assigned to this principal (for RBAC).
     */
    @Builder.Default
    private final Set<String> roles = Set.of();

    /**
     * Additional attributes for ABAC (e.g., department, location).
     */
    @Builder.Default
    private final Map<String, Object> attributes = Map.of();

    /**
     * Returns the entity identifier for this principal.
     * Format: "Type::id" (e.g., "User::alice", "Service::api-gateway").
     * This format is compatible with various policy engines including Cedar Policy.
     */
    public String toEntityIdentifier() {
        return type + "::" + id;
    }

    /**
     * @deprecated Use {@link #toEntityIdentifier()} instead.
     * This method is kept for backward compatibility.
     */
    @Deprecated
    public String toCedarEntity() {
        return toEntityIdentifier();
    }

    @Override
    public String toString() {
        return "Principal{id='" + id + "', type='" + type + "', roles=" + roles + "}";
    }
}

