package io.github.jihedbfr_art.keycloak.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.jihedbfr_art.keycloak.error.ProblemDetailAccessDeniedHandler;
import io.github.jihedbfr_art.keycloak.error.ProblemDetailAuthenticationEntryPoint;
import io.github.jihedbfr_art.keycloak.security.KeycloakRealmRoleConverter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

class KeycloakToolkitAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    KeycloakToolkitAutoConfiguration.class));

    @Test
    void startsSuccessfullyWithEmptyConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(KeycloakRealmRoleConverter.class);
            assertThat(context).hasSingleBean(JwtAuthenticationConverter.class);
            assertThat(context).hasSingleBean(ProblemDetailAuthenticationEntryPoint.class);
            assertThat(context).hasSingleBean(ProblemDetailAccessDeniedHandler.class);
            assertThat(context).hasSingleBean(KeycloakToolkitProperties.class);

            KeycloakToolkitProperties properties = context.getBean(KeycloakToolkitProperties.class);
            assertThat(properties.isRealmRolesEnabled()).isTrue();
            assertThat(properties.isResourceRolesEnabled()).isTrue();
            assertThat(properties.getResourceId()).isNull();
            assertThat(properties.getRolePrefix()).isEqualTo("ROLE_");
            assertThat(properties.isProblemDetailsEnabled()).isTrue();
        });
    }

    @Test
    void backsOffProblemDetailBeansWhenDisabled() {
        contextRunner.withPropertyValues("jihedailabs.keycloak.problem-details-enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(KeycloakRealmRoleConverter.class);
                    assertThat(context).hasSingleBean(JwtAuthenticationConverter.class);
                    assertThat(context).doesNotHaveBean(ProblemDetailAuthenticationEntryPoint.class);
                    assertThat(context).doesNotHaveBean(ProblemDetailAccessDeniedHandler.class);
                });
    }

    @Test
    void backsOffWhenApplicationDefinesItsOwnConverter() {
        contextRunner.withUserConfiguration(CustomConverterConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(KeycloakRealmRoleConverter.class);
                    assertThat(context.getBean(KeycloakRealmRoleConverter.class))
                            .isSameAs(CustomConverterConfiguration.CUSTOM_INSTANCE);
                    assertThat(context).hasSingleBean(JwtAuthenticationConverter.class);
                });
    }

    @Test
    void bindsCustomConfigurationProperties() {
        contextRunner.withPropertyValues(
                "jihedailabs.keycloak.realm-roles-enabled=false",
                "jihedailabs.keycloak.resource-roles-enabled=true",
                "jihedailabs.keycloak.resource-id=my-service",
                "jihedailabs.keycloak.role-prefix=CUSTOM_"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(KeycloakToolkitProperties.class);
            KeycloakToolkitProperties props = context.getBean(KeycloakToolkitProperties.class);
            assertThat(props.isRealmRolesEnabled()).isFalse();
            assertThat(props.isResourceRolesEnabled()).isTrue();
            assertThat(props.getResourceId()).isEqualTo("my-service");
            assertThat(props.getRolePrefix()).isEqualTo("CUSTOM_");
        });
    }

    @Test
    void allowsEmptyStringRolePrefix() {
        contextRunner.withPropertyValues("jihedailabs.keycloak.role-prefix=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    KeycloakToolkitProperties props = context.getBean(KeycloakToolkitProperties.class);
                    assertThat(props.getRolePrefix()).isEqualTo("");
                });
    }

    @Configuration
    static class CustomConverterConfiguration {

        static final KeycloakRealmRoleConverter CUSTOM_INSTANCE =
                new KeycloakRealmRoleConverter(true, false, "ROLE_", null);

        @Bean
        KeycloakRealmRoleConverter keycloakRealmRoleConverter() {
            return CUSTOM_INSTANCE;
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
