package lab;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = LabApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtBoundaryTest {
    static final String ISSUER = "https://issuer.example.invalid/realms/lab";
    static final RSAKey KEY = key();
    static final HttpServer JWKS = jwks();
    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    @LocalServerPort int port;

    static RSAKey key() {
        try { return new RSAKeyGenerator(2048).keyID("lab-key").generate(); }
        catch (JOSEException error) { throw new IllegalStateException(error); }
    }

    static HttpServer jwks() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/jwks", exchange -> {
                byte[] body = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.start();
            return server;
        } catch (Exception error) { throw new IllegalStateException(error); }
    }

    @DynamicPropertySource static void properties(DynamicPropertyRegistry properties) {
        properties.add("lab.issuer", () -> ISSUER);
        properties.add("lab.jwk-set-uri", () -> "http://127.0.0.1:" + JWKS.getAddress().getPort() + "/jwks");
    }
    @AfterAll static void stop() { JWKS.stop(0); }

    JWTClaimsSet.Builder claims(List<?> roles) {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder().issuer(ISSUER).subject("synthetic-client")
                .audience("toolkit-lab").issueTime(Date.from(now.minusSeconds(120)))
                .notBeforeTime(Date.from(now.minusSeconds(120))).expirationTime(Date.from(now.plusSeconds(600)))
                .claim("resource_access", Map.of("toolkit-lab", Map.of("roles", roles)));
    }
    String sign(JWTClaimsSet.Builder claims, RSAKey signingKey) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("lab-key").build(), claims.build());
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }
    HttpResponse<String> request(String path, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(10));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return HTTP.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    void denied(JWTClaimsSet.Builder claims, int status) throws Exception {
        assertThat(request("/api/reader", sign(claims, KEY)).statusCode()).isEqualTo(status);
    }

    @Test void publicRouteWorksWithoutToken() throws Exception { assertThat(request("/api/public", null).statusCode()).isEqualTo(200); }
    @Test void missingTokenHasBearerChallengeAndProblemDetail() throws Exception {
        var response = request("/api/reader", null);
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate")).contains("Bearer");
        assertThat(response.headers().firstValue("Content-Type")).get().asString().contains("application/problem+json");
        assertThat(response.body()).contains("401").doesNotContain("Bearer token");
    }
    @Test void validReaderIsAccepted() throws Exception {
        var response = request("/api/reader", sign(claims(List.of("reader")), KEY));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("ROLE_READER");
    }
    @Test void readerCannotUseAdminRoute() throws Exception {
        var response = request("/api/admin", sign(claims(List.of("reader")), KEY));
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("403");
    }
    @Test void adminIsAccepted() throws Exception { assertThat(request("/api/admin", sign(claims(List.of("admin")), KEY)).statusCode()).isEqualTo(200); }
    @Test void wrongIssuerWithSameSigningKeyIsRejected() throws Exception { denied(claims(List.of("reader")).issuer("https://other.example.invalid"), 401); }
    @Test void wrongAudienceIsRejectedDespiteReaderRole() throws Exception { denied(claims(List.of("reader")).audience("other-api"), 401); }
    @Test void missingAudienceIsRejected() throws Exception { denied(claims(List.of("reader")).claim("aud", null), 401); }
    @Test void expiredTokenIsRejectedBeyondClockSkew() throws Exception {
        denied(claims(List.of("reader")).issueTime(Date.from(Instant.now().minusSeconds(1200)))
                .notBeforeTime(Date.from(Instant.now().minusSeconds(1200)))
                .expirationTime(Date.from(Instant.now().minusSeconds(600))), 401);
    }
    @Test void futureNotBeforeIsRejectedBeyondClockSkew() throws Exception { denied(claims(List.of("reader")).notBeforeTime(Date.from(Instant.now().plusSeconds(600))), 401); }
    @Test void untrustedSignatureIsRejected() throws Exception { assertThat(request("/api/reader", sign(claims(List.of("reader")), key())).statusCode()).isEqualTo(401); }
    @Test void roleForAnotherClientDoesNotAuthorize() throws Exception { denied(claims(List.of()).claim("resource_access", Map.of("other-api", Map.of("roles", List.of("reader")))), 403); }
    @Test void malformedRoleValuesDoNotAuthorize() throws Exception { denied(claims(Arrays.asList(42, true, null, "")), 403); }
    @Test void malformedBearerIsRejected() throws Exception { assertThat(request("/api/reader", "not.a.jwt").statusCode()).isEqualTo(401); }
}
