package io.cheshire.security.auth;

import io.cheshire.security.Principal;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for TokenAuthenticator.
 */
class TokenAuthenticatorTest {

    @Test
    void testAuthenticateWithValidToken() {
        TokenAuthenticator authenticator = TokenAuthenticator.builder()
                .tokenHeader("Authorization")
                .tokenPrefix("Bearer ")
                .requireAuth(false)
                .build();

        // Register a token
        authenticator.registerToken("test-token-123",
                TokenAuthenticator.PrincipalInfo.builder()
                        .id("alice")
                        .type("User")
                        .roles(Set.of("admin"))
                        .build());

        // Create authentication context
        AuthenticationContext context = AuthenticationContext.builder()
                .credentials(Map.of("Authorization", "Bearer test-token-123"))
                .authType("bearer")
                .build();

        // Authenticate
        Principal principal = authenticator.authenticateSync(context);

        assertThat(principal).isNotNull();
        assertThat(principal.getId()).isEqualTo("alice");
        assertThat(principal.getType()).isEqualTo("User");
        assertThat(principal.getRoles()).contains("admin");
    }

    @Test
    void testAuthenticateWithInvalidToken() {
        TokenAuthenticator authenticator = TokenAuthenticator.builder()
                .tokenHeader("Authorization")
                .requireAuth(true)
                .build();

        AuthenticationContext context = AuthenticationContext.builder()
                .credentials(Map.of("Authorization", "Bearer invalid-token"))
                .authType("bearer")
                .build();

        assertThatThrownBy(() -> authenticator.authenticateSync(context))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid or expired token");
    }

    @Test
    void testCanAuthenticate() {
        TokenAuthenticator authenticator = TokenAuthenticator.builder()
                .tokenHeader("Authorization")
                .requireAuth(false)
                .build();

        AuthenticationContext context = AuthenticationContext.builder()
                .credentials(Map.of("Authorization", "Bearer token"))
                .authType("bearer")
                .build();

        assertThat(authenticator.canAuthenticate(context)).isTrue();
    }
}

