# JWT boundary lab

[Version française](README.fr.md)

A small toolkit consumer API, with real HTTP requests and verified rejection cases. This is a standalone project, not a Maven module added to the root build.

## Build and test

Requirements: JDK 17 or 21, Maven, Docker and Compose for the packaged check, and standard Python 3 for the smoke test. The POM pins Spring Boot 4.1.0 and tests use Keycloak 26.7.2. Maven and Docker downloads need network access.

From the repository root:

```bash
mvn clean install
mvn -f examples/resource-server/pom.xml clean verify
```

The first command tests and installs toolkit `0.1.1` into the local Maven cache. The second builds the consumer JAR and runs 19 tests: 14 with a local RSA key and JWKS server, 5 with real Keycloak Testcontainers. Requests cross the security filter and an HTTP server on a random port; they do not use mock-injected security JWTs.

| Check | Expected result |
|---|---:|
| No Bearer on reader route | 401 + Bearer challenge |
| Reader to reader route | 200 |
| Reader to admin route | 403 |
| Admin to admin route | 200 |
| Same key, different issuer | 401 |
| Missing or different audience | 401 |
| Expiry or nbf beyond the tested clock tolerance | 401 |
| Different signature or malformed Bearer | 401 |
| Role for another client or malformed role values | 403 |

The issuer test keeps the same key and other claims to isolate that rejection. Time tests sit ten minutes beyond the boundary, outside the default clock tolerance; they do not precisely measure that tolerance. Signature tests do not establish JWKS rotation behaviour.

## Try the built JAR

From `examples/resource-server`, start Keycloak, then the API in another terminal:

```bash
docker compose -p toolkit-iam-lab up -d
java -jar target/resource-server-lab-0.1.0-SNAPSHOT.jar
```

In a third terminal, from the same directory:

```bash
python smoke.py
```

Six checks should return 401, 200, 403, 200, 403, 401. The script waits for the realm and API, then obtains tokens in memory without printing or storing them. It accepts only loopback HTTP URLs. Defaults bind Keycloak to `127.0.0.1:18080` and the API to `127.0.0.1:18081`. Stop the API with Ctrl+C, then:

```bash
docker compose -p toolkit-iam-lab down
```

`LAB_KEYCLOAK_PORT` changes the Compose port; then adapt the API's `LAB_ISSUER` and `LAB_JWK_SET_URI`, and the script's `--issuer`. `--server.port` changes the JAR's port; `--api` changes the script's API URL. Maven tests allocate their own ports and containers.

## What is configured

The toolkit converts `toolkit-lab` client roles to authorities. This example disables realm roles, explicitly wires the converter and configures Problem Details responses. The application remains responsible for its filter chain and JWT validator: RS256 signature, issuer, time and audience `toolkit-lab`. A JWKS URL alone does not replace issuer or audience validation.

The [synthetic realm](src/test/resources/iam-lab-realm.json) has four service-account clients: reader, administrator, no role and another audience. It uses `client_credentials`, without user passwords. Explicit role scope mappings restrict roles; `fullScopeAllowed` is false. An audience mapper is separate from the role mapper.

Fixture secrets are public, synthetic and limited to this local lab. There are no real accounts or operator data. Development mode, HTTP and ephemeral storage serve the demonstration only. This model does not cover browser/PKCE, user sessions, MFA, immediate revocation, key rotation or MCP transport.

## Check the evidence

`JwtBoundaryTest` covers locally signed claims. `KeycloakServiceAccountTest` covers real issuance and service-account roles/audiences. `smoke.py` exercises the running JAR. CI repeats the 19 tests and six packaged checks with JDK 17 and 21; verify results at the PR head. The root toolkit matrix remains separate.

References: [Spring Security JWT validation](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html), [Keycloak service accounts](https://www.keycloak.org/docs/latest/server_admin/index.html#_service_accounts), [Keycloak audiences](https://www.keycloak.org/docs/latest/server_admin/index.html#_audience), [RFC 6750 section 3](https://www.rfc-editor.org/rfc/rfc6750#section-3).

---

<div align="center">
  <img src="https://raw.githubusercontent.com/jihedbfr-art/jihedbfr-art/main/assets/brand/jihedailabs-logo.svg" alt="JihedAiLabs" width="120"/>
  <br/>
  <sub>A <a href="https://github.com/jihedbfr-art"><b>JihedAiLabs</b></a> project</sub>
</div>
