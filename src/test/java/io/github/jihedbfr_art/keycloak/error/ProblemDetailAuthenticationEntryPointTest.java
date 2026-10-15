package io.github.jihedbfr_art.keycloak.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class ProblemDetailAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProblemDetailAuthenticationEntryPoint entryPoint =
            new ProblemDetailAuthenticationEntryPoint(objectMapper);

    @Test
    void writesProblemJsonBodyWithStatus401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("bad token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(response.getContentType()).isEqualTo("application/problem+json");

        String body = response.getContentAsString();
        assertThat(body).contains("\"status\":401");
        assertThat(body).contains("\"title\":\"Unauthorized\"");
        assertThat(body).contains("\"path\":\"/api/orders/42\"");
    }

    @Test
    void preservesAnExistingAuthenticationChallenge() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"orders\"");

        entryPoint.commence(request, response, new BadCredentialsException("bad token"));

        assertThat(response.getHeaders(HttpHeaders.WWW_AUTHENTICATE))
                .containsExactly("Bearer realm=\"orders\"");
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void doesNotExposeAuthenticationExceptionDetails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("synthetic-sensitive-detail"));

        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(response.getContentAsString()).doesNotContain("synthetic-sensitive-detail");
    }
}
