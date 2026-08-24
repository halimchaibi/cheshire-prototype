package io.cheshire.security;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Principal.
 */
class PrincipalTest {

    @Test
    void testToEntityIdentifier() {
        Principal principal = Principal.builder()
                .id("alice")
                .type("User")
                .build();

        assertThat(principal.toEntityIdentifier()).isEqualTo("User::alice");
        // Test backward compatibility
        assertThat(principal.toCedarEntity()).isEqualTo("User::alice");
    }

    @Test
    void testPrincipalWithRoles() {
        Principal principal = Principal.builder()
                .id("alice")
                .type("User")
                .roles(Set.of("admin", "developer"))
                .build();

        assertThat(principal.getRoles()).containsExactlyInAnyOrder("admin", "developer");
    }

    @Test
    void testPrincipalWithAttributes() {
        Principal principal = Principal.builder()
                .id("alice")
                .type("User")
                .attributes(Map.of("department", "engineering", "location", "US"))
                .build();

        assertThat(principal.getAttributes()).containsEntry("department", "engineering");
        assertThat(principal.getAttributes()).containsEntry("location", "US");
    }
}

