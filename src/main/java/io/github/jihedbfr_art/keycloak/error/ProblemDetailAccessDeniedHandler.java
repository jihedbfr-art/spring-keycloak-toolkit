package io.github.jihedbfr_art.keycloak.error;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * An {@link AccessDeniedHandler} implementation that writes an RFC 7807 problem details JSON payload
 * when an authenticated client lacks the necessary roles or permissions to access a protected resource.
 *
 * <p>Spring Security standardizes access-denied responses into {@code application/problem+json} format with HTTP 403.
 *
 * <p>Usage example in security filter chain configuration:
 * <pre>{@code
 * @Bean
 * public SecurityFilterChain securityFilterChain(HttpSecurity http,
 *         ProblemDetailAccessDeniedHandler accessDeniedHandler) throws Exception {
 *     http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
 *         .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(accessDeniedHandler));
 *     return http.build();
 * }
 * }</pre>
 */
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Constructs a new {@code ProblemDetailAccessDeniedHandler} with the specified Jackson object mapper.
     *
     * @param objectMapper the object mapper used to serialize {@link ProblemDetail} responses (must not be {@code null})
     */
    public ProblemDetailAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Handles an access denied failure by rendering an RFC 7807 403 Forbidden problem response.
     *
     * @param request               the HTTP request that resulted in an {@link AccessDeniedException}
     * @param response              the HTTP response to write the problem details to
     * @param accessDeniedException the exception that triggered the access denied handler
     * @throws IOException if an input or output error occurs while writing the response
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "The authenticated caller does not have the required role for this resource.");
        problem.setTitle("Forbidden");
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        problem.setProperty("path", request.getRequestURI());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
