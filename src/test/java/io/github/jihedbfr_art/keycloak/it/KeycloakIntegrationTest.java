package io.github.jihedbfr_art.keycloak.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dasniko.testcontainers.keycloak.KeycloakContainer;

import io.github.jihedbfr_art.keycloak.error.ProblemDetailAccessDeniedHandler;
import io.github.jihedbfr_art.keycloak.error.ProblemDetailAuthenticationEntryPoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = {
        KeycloakIntegrationTest.TestApplication.class,
        KeycloakIntegrationTest.TestSecurityConfig.class,
        KeycloakIntegrationTest.TestApiController.class
})
class KeycloakIntegrationTest {

    private static final String KEYCLOAK_IMAGE =
            System.getProperty("keycloak.container.image", "quay.io/keycloak/keycloak:26.7.1");

    @Container
    private static final KeycloakContainer KEYCLOAK =
            new KeycloakContainer(KEYCLOAK_IMAGE).withRealmImportFile("test-realm.json");

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private static final RestTemplate REST_TEMPLATE = new RestTemplate();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @DynamicPropertySource
    static void configureKeycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> KEYCLOAK.getAuthServerUrl() + "/realms/test-realm");
        registry.add("jihedailabs.keycloak.resource-id", () -> "test-client");
        registry.add("jihedailabs.keycloak.realm-roles-enabled", () -> "true");
        registry.add("jihedailabs.keycloak.resource-roles-enabled", () -> "true");
    }

    private static String obtainAccessToken(String username, String password) throws Exception {
        String tokenUrl = KEYCLOAK.getAuthServerUrl() + "/realms/test-realm/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", "test-client");
        body.add("username", username);
        body.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = REST_TEMPLATE.postForEntity(tokenUrl, request, String.class);

        JsonNode jsonNode = OBJECT_MAPPER.readTree(response.getBody());
        return jsonNode.get("access_token").asText();
    }

    @Test
    void publicEndpointIsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/public"))
                .andExpect(status().isOk())
                .andExpect(content().string("public-content"));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/user"));
    }

    @Test
    void aliceCanAccessUserAndEditorEndpointsWithExtractedRoles() throws Exception {
        String token = obtainAccessToken("alice", "password123");

        mockMvc.perform(get("/api/user").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.authorities").isArray())
                .andExpect(jsonPath("$.authorities[?(@ == 'ROLE_USER')]").exists())
                .andExpect(jsonPath("$.authorities[?(@ == 'ROLE_EDITOR')]").exists());

        mockMvc.perform(get("/api/editor").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("editor-content"));
    }

    @Test
    void aliceForbiddenOnAdminEndpointReturns403ProblemDetail() throws Exception {
        String token = obtainAccessToken("alice", "password123");

        mockMvc.perform(get("/api/admin").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.path").value("/api/admin"));
    }

    @Test
    void bobCanAccessAdminAndManagerEndpoints() throws Exception {
        String token = obtainAccessToken("bob", "password123");

        mockMvc.perform(get("/api/admin").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("admin-content"));

        mockMvc.perform(get("/api/manager").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("manager-content"));
    }

    @SpringBootApplication
    static class TestApplication {
    }

    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        public org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthenticationConverter(
                io.github.jihedbfr_art.keycloak.security.KeycloakRealmRoleConverter roleConverter) {
            org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter converter =
                    new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
            converter.setJwtGrantedAuthoritiesConverter(roleConverter);
            return converter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                HttpSecurity http,
                org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthenticationConverter,
                ProblemDetailAuthenticationEntryPoint entryPoint,
                ProblemDetailAccessDeniedHandler accessDeniedHandler) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/public").permitAll()
                    .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                )
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                );
            return http.build();
        }
    }

    @RestController
    static class TestApiController {

        @GetMapping("/api/public")
        public String publicEndpoint() {
            return "public-content";
        }

        @GetMapping("/api/user")
        @PreAuthorize("hasRole('USER')")
        public Map<String, Object> userEndpoint(Authentication authentication) {
            String username = authentication.getName();
            if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
                String preferred = jwtAuth.getToken().getClaimAsString("preferred_username");
                if (preferred != null) {
                    username = preferred;
                }
            }
            return Map.of(
                    "username", username,
                    "authorities", authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList())
            );
        }

        @GetMapping("/api/admin")
        @PreAuthorize("hasRole('ADMIN')")
        public String adminEndpoint() {
            return "admin-content";
        }

        @GetMapping("/api/editor")
        @PreAuthorize("hasRole('EDITOR')")
        public String editorEndpoint() {
            return "editor-content";
        }

        @GetMapping("/api/manager")
        @PreAuthorize("hasRole('MANAGER')")
        public String managerEndpoint() {
            return "manager-content";
        }
    }
}
