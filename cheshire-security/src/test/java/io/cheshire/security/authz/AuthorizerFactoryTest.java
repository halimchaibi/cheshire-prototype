package io.cheshire.security.authz;

import io.cheshire.security.config.SecurityConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuthorizerFactory.
 */
class AuthorizerFactoryTest {

    @Test
    void testCreateCedarAuthorizer() {
        SecurityConfig config = SecurityConfig.builder()
                .enabled(true)
                .authorization(SecurityConfig.AuthorizationConfig.builder()
                        .type(AuthorizerType.CEDAR)
                        .build())
                .build();

        Authorizer authorizer = AuthorizerFactory.createAuthorizer(config);
        assertThat(authorizer).isNotNull();
        assertThat(authorizer).isInstanceOf(CedarAuthorizer.class);
    }

    @Test
    void testCreateSimpleAuthorizer() {
        SecurityConfig config = SecurityConfig.builder()
                .enabled(true)
                .authorization(SecurityConfig.AuthorizationConfig.builder()
                        .type(AuthorizerType.SIMPLE)
                        .build())
                .build();

        Authorizer authorizer = AuthorizerFactory.createAuthorizer(config);
        assertThat(authorizer).isNotNull();
        assertThat(authorizer).isInstanceOf(SimpleAuthorizer.class);
    }

    @Test
    void testCreateNoneAuthorizer() {
        SecurityConfig config = SecurityConfig.builder()
                .enabled(true)
                .authorization(SecurityConfig.AuthorizationConfig.builder()
                        .type(AuthorizerType.NONE)
                        .build())
                .build();

        Authorizer authorizer = AuthorizerFactory.createAuthorizer(config);
        assertThat(authorizer).isNull();
    }

    @Test
    void testSecurityDisabled() {
        SecurityConfig config = SecurityConfig.builder()
                .enabled(false)
                .build();

        Authorizer authorizer = AuthorizerFactory.createAuthorizer(config);
        assertThat(authorizer).isNull();
    }

    @Test
    void testNullConfig() {
        Authorizer authorizer = AuthorizerFactory.createAuthorizer(null);
        assertThat(authorizer).isNull();
    }

    @Test
    void testDefaultToSimple() {
        SecurityConfig config = SecurityConfig.builder()
                .enabled(true)
                .authorization(SecurityConfig.AuthorizationConfig.builder()
                        // type not specified, should default to SIMPLE
                        .build())
                .build();

        Authorizer authorizer = AuthorizerFactory.createAuthorizer(config);
        assertThat(authorizer).isNotNull();
        assertThat(authorizer).isInstanceOf(SimpleAuthorizer.class);
    }
}

