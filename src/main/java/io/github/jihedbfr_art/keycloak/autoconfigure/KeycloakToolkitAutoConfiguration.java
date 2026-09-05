package io.github.jihedbfr_art.keycloak.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.jihedbfr_art.keycloak.error.ProblemDetailAccessDeniedHandler;
import io.github.jihedbfr_art.keycloak.error.ProblemDetailAuthenticationEntryPoint;
import io.github.jihedbfr_art.keycloak.security.KeycloakRealmRoleConverter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Spring Boot auto-configuration entry point for the Keycloak Toolkit starter.
 *
 * <p>Activates only when Spring Security OAuth2 resource server JWT classes are on the classpath.
 * Every registered bean provides a conditional back-off ({@link ConditionalOnMissingBean}), ensuring
 * custom application beans always take precedence over default auto-configured implementations.
 */
@AutoConfiguration
@ConditionalOnClass(Jwt.class)
@EnableConfigurationProperties(KeycloakToolkitProperties.class)
public class KeycloakToolkitAutoConfiguration {

    /**
     * Default constructor for {@code KeycloakToolkitAutoConfiguration}.
     */
    public KeycloakToolkitAutoConfiguration() {
    }

    /**
     * Provides the default {@link KeycloakRealmRoleConverter} bean configured with toolkit properties.
     *
     * @param properties the bound Keycloak toolkit properties
     * @return a configured {@link KeycloakRealmRoleConverter}
     */
    @Bean
    @ConditionalOnMissingBean
    public KeycloakRealmRoleConverter keycloakRealmRoleConverter(KeycloakToolkitProperties properties) {
        return new KeycloakRealmRoleConverter(
                properties.isRealmRolesEnabled(),
                properties.isResourceRolesEnabled(),
                properties.getRolePrefix(),
                properties.getResourceId());
    }

    /**
     * Provides the default {@link JwtAuthenticationConverter} configured with the {@link KeycloakRealmRoleConverter}.
     *
     * @param roleConverter the Keycloak role-to-authority converter
     * @return a configured {@link JwtAuthenticationConverter}
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter(KeycloakRealmRoleConverter roleConverter) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roleConverter);
        return converter;
    }

    /**
     * Provides the default {@link ProblemDetailAuthenticationEntryPoint} bean for RFC 7807 401 error formatting.
     *
     * @param objectMapperProvider provider for the application Jackson {@link ObjectMapper}, falls back to default if absent
     * @return a configured {@link ProblemDetailAuthenticationEntryPoint}
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "jihedailabs.keycloak", name = "problem-details-enabled",
            havingValue = "true", matchIfMissing = true)
    public ProblemDetailAuthenticationEntryPoint problemDetailAuthenticationEntryPoint(
            org.springframework.beans.factory.ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new ProblemDetailAuthenticationEntryPoint(objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    /**
     * Provides the default {@link ProblemDetailAccessDeniedHandler} bean for RFC 7807 403 error formatting.
     *
     * @param objectMapperProvider provider for the application Jackson {@link ObjectMapper}, falls back to default if absent
     * @return a configured {@link ProblemDetailAccessDeniedHandler}
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "jihedailabs.keycloak", name = "problem-details-enabled",
            havingValue = "true", matchIfMissing = true)
    public ProblemDetailAccessDeniedHandler problemDetailAccessDeniedHandler(
            org.springframework.beans.factory.ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new ProblemDetailAccessDeniedHandler(objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }
}
