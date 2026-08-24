package io.cheshire.security;

import io.cheshire.security.auth.Authenticator;
import io.cheshire.security.auth.TokenAuthenticator;
import io.cheshire.security.config.SecurityConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SecurityUtils.
 */
class SecurityUtilsTest {

    @Test
    void shouldCreateTokenAuthenticator() {
        SecurityConfig securityConfig = SecurityConfig.builder()
                .enabled(true)
                .authentication(SecurityConfig.AuthenticationConfig.builder()
                        .type("token")
                        .requireAuth(false)
                        .build())
                .build();

        Authenticator authenticator = SecurityUtils.createAuthenticator(securityConfig);

        assertThat(authenticator).isNotNull();
        assertThat(authenticator).isInstanceOf(TokenAuthenticator.class);
    }
}


