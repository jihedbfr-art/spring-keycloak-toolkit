package com.jihedapps.keycloak.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRealmRoleConverterTest {

    private Jwt jwtWith(Map<String, Object> claims) {
        return Jwt.withTokenValue("dummy-token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claims(c -> c.putAll(claims))
                .build();
    }

    @Test
    void mapsRealmRolesToUppercasePrefixedAuthorities() {
        Jwt jwt = jwtWith(Map.of(
                "realm_access", Map.of("roles", java.util.List.of("app-admin", "viewer"))
        ));

        KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter(true, false, "ROLE_", null);
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_APP-ADMIN", "ROLE_VIEWER");
    }

    @Test
    void mapsResourceRolesOnlyForConfiguredClientId() {
        Jwt jwt = jwtWith(Map.of(
                "resource_access", Map.of(
                        "my-client", Map.of("roles", java.util.List.of("editor")),
                        "other-client", Map.of("roles", java.util.List.of("should-not-appear"))
                )
        ));

        KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter(false, true, "ROLE_", "my-client");
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_EDITOR");
    }

    @Test
    void returnsEmptySetWhenNoAccessClaimsPresent() {
        Jwt jwt = jwtWith(Map.of("sub", "some-user"));

        KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter(true, true, "ROLE_", "my-client");
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void combinesRealmAndResourceRolesWhenBothEnabled() {
        Jwt jwt = jwtWith(Map.of(
                "realm_access", Map.of("roles", java.util.List.of("app-admin")),
                "resource_access", Map.of("my-client", Map.of("roles", java.util.List.of("editor")))
        ));

        KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter(true, true, "ROLE_", "my-client");
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_APP-ADMIN", "ROLE_EDITOR");
    }
}
