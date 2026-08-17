package io.github.jihedbfr_art.keycloak.error;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * An {@link AuthenticationEntryPoint} implementation that writes an RFC 7807 problem details JSON payload
 * when unauthenticated clients attempt to access a protected resource.
 *
 * <p>Spring Security's default resource server behavior returns either an empty response body with a
 * {@code WWW-Authenticate} header or HTML error pages. This entry point standardizes all 401 responses into
 * {@code application/problem+json} format.
 *
 * <p>Usage example in security filter chain configuration:
 * <pre>{@code
 * @Bean
 * public SecurityFilterChain securityFilterChain(HttpSecurity http,
 *         ProblemDetailAuthenticationEntryPoint authenticationEntryPoint) throws Exception {
 *     http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
 *         .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint));
 *     return http.build();
 * }
 * }</pre>
 */
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Constructs a new {@code ProblemDetailAuthenticationEntryPoint} with the specified Jackson object mapper.
     *
     * @param objectMapper the object mapper used to serialize {@link ProblemDetail} responses (must not be {@code null})
     */
    public ProblemDetailAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Commences an authentication scheme by rendering an RFC 7807 401 Unauthorized problem response.
     *
     * @param request       the HTTP request that resulted in an {@link AuthenticationException}
     * @param response      the HTTP response to write the problem details to
     * @param authException the exception that triggered the authentication entry point
     * @throws IOException if an input or output error occurs while writing the response
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Authentication is required to access this resource.");
        problem.setTitle("Unauthorized");
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        problem.setProperty("path", request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
