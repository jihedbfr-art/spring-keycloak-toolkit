package com.jihedapps.keycloak.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class ProblemDetailAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProblemDetailAccessDeniedHandler handler =
            new ProblemDetailAccessDeniedHandler(objectMapper);

    @Test
    void writesProblemJsonBodyWithStatus403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/orders/42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("missing role"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");

        String body = response.getContentAsString();
        assertThat(body).contains("\"status\":403");
        assertThat(body).contains("\"path\":\"/api/orders/42\"");
    }
}
