package io.cheshire.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SecurityContext.
 */
class SecurityContextTest {

    @Test
    void testSetAndGetPrincipal() {
        Principal principal = Principal.builder()
                .id("alice")
                .type("User")
                .build();

        SecurityContext.setPrincipal(principal);

        Principal retrieved = SecurityContext.getPrincipal();
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo("alice");

        SecurityContext.clear();
        assertThat(SecurityContext.getPrincipal()).isNull();
    }

    @Test
    void testIsAuthenticated() {
        assertThat(SecurityContext.isAuthenticated()).isFalse();

        Principal principal = Principal.builder()
                .id("alice")
                .build();

        SecurityContext.setPrincipal(principal);
        assertThat(SecurityContext.isAuthenticated()).isTrue();

        SecurityContext.clear();
        assertThat(SecurityContext.isAuthenticated()).isFalse();
    }

    @Test
    void testWithPrincipal() {
        Principal principal1 = Principal.builder().id("alice").build();
        Principal principal2 = Principal.builder().id("bob").build();

        SecurityContext.setPrincipal(principal1);

        SecurityContext.withPrincipal(principal2, () -> {
            assertThat(SecurityContext.getPrincipal().getId()).isEqualTo("bob");
        });

        // Should restore previous principal
        assertThat(SecurityContext.getPrincipal().getId()).isEqualTo("alice");

        SecurityContext.clear();
    }
}

