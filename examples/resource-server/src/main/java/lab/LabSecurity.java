package lab;

import io.github.jihedbfr_art.keycloak.error.ProblemDetailAccessDeniedHandler;
import io.github.jihedbfr_art.keycloak.error.ProblemDetailAuthenticationEntryPoint;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class LabSecurity {
    @Bean JwtDecoder jwtDecoder(@Value("${lab.issuer}") String issuer, @Value("${lab.jwk-set-uri}") String keys,
            @Value("${lab.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(keys).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<List<String>>("aud", values -> values != null && values.contains(audience))));
        return decoder;
    }

    @Bean SecurityFilterChain security(HttpSecurity http, JwtAuthenticationConverter converter,
            ProblemDetailAuthenticationEntryPoint entryPoint, ProblemDetailAccessDeniedHandler denied) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(routes -> routes.requestMatchers("/api/public").permitAll()
                        .requestMatchers("/api/reader").hasRole("READER")
                        .requestMatchers("/api/admin").hasRole("ADMIN").anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(denied))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint).accessDeniedHandler(denied))
                .build();
    }
}
