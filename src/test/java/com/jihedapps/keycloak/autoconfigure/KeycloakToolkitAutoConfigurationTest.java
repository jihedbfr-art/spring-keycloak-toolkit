package com.jihedapps.keycloak.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.jihedapps.keycloak.error.ProblemDetailAccessDeniedHandler;
import com.jihedapps.keycloak.error.ProblemDetailAuthenticationEntryPoint;
import com.jihedapps.keycloak.security.KeycloakRealmRoleConverter;

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
    void registersDefaultBeansWhenNothingElseDefinesThem() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(KeycloakRealmRoleConverter.class);
            assertThat(context).hasSingleBean(JwtAuthenticationConverter.class);
            assertThat(context).hasSingleBean(ProblemDetailAuthenticationEntryPoint.class);
            assertThat(context).hasSingleBean(ProblemDetailAccessDeniedHandler.class);
        });
    }

    @Test
    void backsOffProblemDetailBeansWhenDisabled() {
        contextRunner.withPropertyValues("jihedapps.keycloak-toolkit.problem-details-enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemDetailAuthenticationEntryPoint.class);
                    assertThat(context).doesNotHaveBean(ProblemDetailAccessDeniedHandler.class);
                });
    }

    @Test
    void backsOffWhenApplicationDefinesItsOwnConverter() {
        contextRunner.withUserConfiguration(CustomConverterConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(KeycloakRealmRoleConverter.class);
                    assertThat(context.getBean(KeycloakRealmRoleConverter.class))
                            .isSameAs(CustomConverterConfiguration.CUSTOM_INSTANCE);
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
