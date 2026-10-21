package lab;

import com.nimbusds.jose.util.JSONObjectUtils;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = LabApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KeycloakServiceAccountTest {
    @Container static final KeycloakContainer KEYCLOAK = new KeycloakContainer("quay.io/keycloak/keycloak:26.7.2")
            .withRealmImportFile("iam-lab-realm.json");
    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    @LocalServerPort int port;

    @DynamicPropertySource static void properties(DynamicPropertyRegistry properties) {
        properties.add("lab.issuer", () -> KEYCLOAK.getAuthServerUrl() + "/realms/iam-lab");
        properties.add("lab.jwk-set-uri", () -> KEYCLOAK.getAuthServerUrl() + "/realms/iam-lab/protocol/openid-connect/certs");
    }

    String token(String client) throws Exception {
        String basic = Base64.getEncoder().encodeToString((client + ":" + client + "-only-for-local-demo").getBytes(StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(URI.create(KEYCLOAK.getAuthServerUrl() + "/realms/iam-lab/protocol/openid-connect/token"))
                .timeout(Duration.ofSeconds(10)).header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials")).build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return (String) JSONObjectUtils.parse(response.body()).get("access_token");
    }
    HttpResponse<String> request(String path, String client) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token(client)).GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }
    @Test void readerTokenAuthorizesReaderRoute() throws Exception {
        var response = request("/api/reader", "lab-reader");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("ROLE_READER");
    }
    @Test void readerCannotUseAdminRoute() throws Exception { assertThat(request("/api/admin", "lab-reader").statusCode()).isEqualTo(403); }
    @Test void adminTokenAuthorizesAdminRoute() throws Exception { assertThat(request("/api/admin", "lab-admin").statusCode()).isEqualTo(200); }
    @Test void validAudienceWithoutRoleIsForbidden() throws Exception { assertThat(request("/api/reader", "lab-no-role").statusCode()).isEqualTo(403); }
    @Test void roleWithAnotherAudienceIsUnauthorized() throws Exception { assertThat(request("/api/reader", "lab-other-audience").statusCode()).isEqualTo(401); }
}
