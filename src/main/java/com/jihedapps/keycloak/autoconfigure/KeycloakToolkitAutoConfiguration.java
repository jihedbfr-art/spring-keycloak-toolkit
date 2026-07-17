package com.jihedapps.keycloak.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.jihedapps.keycloak.error.ProblemDetailAccessDeniedHandler;
import com.jihedapps.keycloak.error.ProblemDetailAuthenticationEntryPoint;
import com.jihedapps.keycloak.security.KeycloakRealmRoleConverter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Auto-configuration entry point for this toolkit. Only activates when Spring's OAuth2 resource
 * server classes are on the classpath, and every bean backs off if the application already
 * defines its own (this is meant to be a sane default, not something to fight with).
 */
@AutoConfiguration
@ConditionalOnClass(Jwt.class)
@EnableConfigurationProperties(KeycloakToolkitProperties.class)
public class KeycloakToolkitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KeycloakRealmRoleConverter keycloakRealmRoleConverter(KeycloakToolkitProperties properties) {
        return new KeycloakRealmRoleConverter(
                properties.isRealmRolesEnabled(),
                properties.isResourceRolesEnabled(),
                properties.getRolePrefix(),
                properties.getResourceId());
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter(KeycloakRealmRoleConverter roleConverter) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roleConverter);
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "jihedapps.keycloak-toolkit", name = "problem-details-enabled",
            havingValue = "true", matchIfMissing = true)
    public ProblemDetailAuthenticationEntryPoint problemDetailAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new ProblemDetailAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "jihedapps.keycloak-toolkit", name = "problem-details-enabled",
            havingValue = "true", matchIfMissing = true)
    public ProblemDetailAccessDeniedHandler problemDetailAccessDeniedHandler(ObjectMapper objectMapper) {
        return new ProblemDetailAccessDeniedHandler(objectMapper);
    }
}
