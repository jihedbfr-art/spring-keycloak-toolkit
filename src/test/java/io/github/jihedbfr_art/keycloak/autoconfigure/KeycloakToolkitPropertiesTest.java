package io.github.jihedbfr_art.keycloak.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KeycloakToolkitPropertiesTest {

    @Test
    void defaultValuesAreSane() {
        KeycloakToolkitProperties properties = new KeycloakToolkitProperties();

        assertThat(properties.isRealmRolesEnabled()).isTrue();
        assertThat(properties.isResourceRolesEnabled()).isTrue();
        assertThat(properties.getResourceId()).isNull();
        assertThat(properties.getRolePrefix()).isEqualTo("ROLE_");
        assertThat(properties.isProblemDetailsEnabled()).isTrue();
    }

    @Test
    void allowsEmptyStringRolePrefix() {
        KeycloakToolkitProperties properties = new KeycloakToolkitProperties();
        properties.setRolePrefix("");

        assertThat(properties.getRolePrefix()).isEqualTo("");
        properties.validate();
    }

    @Test
    void rejectsWhitespaceOnlyRolePrefix() {
        KeycloakToolkitProperties properties = new KeycloakToolkitProperties();

        assertThatThrownBy(() -> properties.setRolePrefix("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Property 'jihedailabs.keycloak.role-prefix' must not be null or whitespace-only.");
    }

    @Test
    void rejectsNullRolePrefix() {
        KeycloakToolkitProperties properties = new KeycloakToolkitProperties();

        assertThatThrownBy(() -> properties.setRolePrefix(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Property 'jihedailabs.keycloak.role-prefix' must not be null or whitespace-only.");
    }
}
