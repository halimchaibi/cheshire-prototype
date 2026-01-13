package io.cheshire.security.authz;

import io.cheshire.security.Principal;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SimpleAuthorizer.
 */
class SimpleAuthorizerTest {

    @Test
    void testAllowAllBehavior() {
        SimpleAuthorizer authorizer = SimpleAuthorizer.builder()
                .defaultBehavior(SimpleAuthorizer.Behavior.ALLOW_ALL)
                .build();

        Principal principal = Principal.builder()
                .id("user1")
                .build();

        CompletableFuture<Boolean> result = authorizer.authorize(principal, "execute", "Capability::test", Map.of());
        assertThat(result.join()).isTrue();
    }

    @Test
    void testDenyAllBehavior() {
        SimpleAuthorizer authorizer = SimpleAuthorizer.builder()
                .defaultBehavior(SimpleAuthorizer.Behavior.DENY_ALL)
                .build();

        Principal principal = Principal.builder()
                .id("user1")
                .build();

        CompletableFuture<Boolean> result = authorizer.authorize(principal, "execute", "Capability::test", Map.of());
        assertThat(result.join()).isFalse();
    }

    @Test
    void testAdminRoleAccess() {
        SimpleAuthorizer authorizer = SimpleAuthorizer.builder()
                .defaultBehavior(SimpleAuthorizer.Behavior.DENY_ALL)
                .adminRole("admin")
                .build();

        Principal admin = Principal.builder()
                .id("admin1")
                .roles(Set.of("admin"))
                .build();

        Principal user = Principal.builder()
                .id("user1")
                .roles(Set.of("user"))
                .build();

        // Admin should have access
        CompletableFuture<Boolean> adminResult = authorizer.authorize(admin, "execute", "Capability::test", Map.of());
        assertThat(adminResult.join()).isTrue();

        // Regular user should be denied
        CompletableFuture<Boolean> userResult = authorizer.authorize(user, "execute", "Capability::test", Map.of());
        assertThat(userResult.join()).isFalse();
    }

    @Test
    void testCapabilitySpecificRoles() {
        SimpleAuthorizer authorizer = SimpleAuthorizer.builder()
                .defaultBehavior(SimpleAuthorizer.Behavior.DENY_ALL)
                .capabilityRoles(Map.of(
                        "query_users", Set.of("admin", "manager"),
                        "read_data", Set.of("user", "admin")
                ))
                .build();

        Principal admin = Principal.builder()
                .id("admin1")
                .roles(Set.of("admin"))
                .build();

        Principal manager = Principal.builder()
                .id("manager1")
                .roles(Set.of("manager"))
                .build();

        Principal user = Principal.builder()
                .id("user1")
                .roles(Set.of("user"))
                .build();

        // Admin can access query_users
        CompletableFuture<Boolean> adminQueryResult = authorizer.authorize(admin, "execute", "Capability::query_users", Map.of());
        assertThat(adminQueryResult.join()).isTrue();

        // Manager can access query_users
        CompletableFuture<Boolean> managerQueryResult = authorizer.authorize(manager, "execute", "Capability::query_users", Map.of());
        assertThat(managerQueryResult.join()).isTrue();

        // User cannot access query_users
        CompletableFuture<Boolean> userQueryResult = authorizer.authorize(user, "execute", "Capability::query_users", Map.of());
        assertThat(userQueryResult.join()).isFalse();

        // User can access read_data
        CompletableFuture<Boolean> userReadResult = authorizer.authorize(user, "execute", "Capability::read_data", Map.of());
        assertThat(userReadResult.join()).isTrue();
    }

    @Test
    void testRoleBasedDefaultBehavior() {
        SimpleAuthorizer authorizer = SimpleAuthorizer.builder()
                .defaultBehavior(SimpleAuthorizer.Behavior.ROLE_BASED)
                .build();

        Principal withRoles = Principal.builder()
                .id("user1")
                .roles(Set.of("user"))
                .build();

        Principal withoutRoles = Principal.builder()
                .id("user2")
                .build();

        // Principal with roles should be allowed (permissive default)
        CompletableFuture<Boolean> withRolesResult = authorizer.authorize(withRoles, "execute", "Capability::test", Map.of());
        assertThat(withRolesResult.join()).isTrue();

        // Principal without roles should be denied
        CompletableFuture<Boolean> withoutRolesResult = authorizer.authorize(withoutRoles, "execute", "Capability::test", Map.of());
        assertThat(withoutRolesResult.join()).isFalse();
    }

    @Test
    void testPrivilegedRoles() {
        SimpleAuthorizer authorizer = SimpleAuthorizer.builder()
                .defaultBehavior(SimpleAuthorizer.Behavior.DENY_ALL)
                .privilegedRoles(Set.of("admin", "superuser"))
                .build();

        Principal admin = Principal.builder()
                .id("admin1")
                .roles(Set.of("admin"))
                .build();

        Principal superuser = Principal.builder()
                .id("super1")
                .roles(Set.of("superuser"))
                .build();

        Principal user = Principal.builder()
                .id("user1")
                .roles(Set.of("user"))
                .build();

        // Admin should have access
        CompletableFuture<Boolean> adminResult = authorizer.authorize(admin, "execute", "Capability::test", Map.of());
        assertThat(adminResult.join()).isTrue();

        // Superuser should have access
        CompletableFuture<Boolean> superuserResult = authorizer.authorize(superuser, "execute", "Capability::test", Map.of());
        assertThat(superuserResult.join()).isTrue();

        // Regular user should be denied
        CompletableFuture<Boolean> userResult = authorizer.authorize(user, "execute", "Capability::test", Map.of());
        assertThat(userResult.join()).isFalse();
    }

    @Test
    void testAnonymousPrincipal() {
        SimpleAuthorizer authorizer = SimpleAuthorizer.builder()
                .defaultBehavior(SimpleAuthorizer.Behavior.ALLOW_ALL)
                .build();

        Principal anonymous = Principal.builder()
                .id("anonymous")
                .type("Anonymous")
                .roles(Set.of())
                .build();

        CompletableFuture<Boolean> result = authorizer.authorize(anonymous, "execute", "Capability::test", Map.of());
        assertThat(result.join()).isTrue();
    }
}

