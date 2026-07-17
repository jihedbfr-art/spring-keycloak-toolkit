# spring-keycloak-toolkit

[![JitPack](https://jitpack.io/v/jihedbfr-art/spring-keycloak-toolkit.svg)](https://jitpack.io/#jihedbfr-art/spring-keycloak-toolkit)
[![CI](https://github.com/jihedbfr-art/spring-keycloak-toolkit/actions/workflows/ci.yml/badge.svg)](https://github.com/jihedbfr-art/spring-keycloak-toolkit/actions)

Small Spring Boot auto-configuration for apps that sit behind Keycloak as a resource server.
It fixes the one thing that trips up almost every Spring + Keycloak setup: Spring Security's
default JWT converter has no idea that Keycloak puts roles under `realm_access.roles` and
`resource_access.<clientId>.roles` instead of a flat `scope` claim. Without this, `hasRole(...)`
and `@PreAuthorize` silently do nothing because the token never produces any `ROLE_*` authority.

I've hit this same wiring on every Keycloak-secured backend I've built over the last two years
(realms, custom SPI authenticators, the works), and copy-pasted a version of this converter into
each one. This is that code, finally pulled out, tested, and packaged so I stop rewriting it.

## What it gives you

- `KeycloakRealmRoleConverter` — reads realm and/or client roles off the JWT and maps them to
  `SimpleGrantedAuthority` with a configurable prefix (defaults to `ROLE_`, matching Spring
  Security's own convention).
- Auto-configuration that registers a `JwtAuthenticationConverter` wired to the role converter,
  so `spring-security-oauth2-resource-server` picks it up with zero extra config in the common case.
- `ProblemDetailAuthenticationEntryPoint` / `ProblemDetailAccessDeniedHandler` — RFC 7807 JSON
  bodies for 401/403 instead of Spring Security's default empty response. Not wired into your
  filter chain automatically (every app matches endpoints differently), just exposed as beans you
  plug into `exceptionHandling(...)`.

## What it deliberately does not do

It does not configure your `SecurityFilterChain`, does not touch endpoint matchers, and does not
assume a particular realm/client layout beyond "roles live where Keycloak puts them." Auto-wiring
a security chain for you would mean guessing at your public endpoints, and getting that wrong
silently is worse than writing four lines of config yourself.

## Install

Via [JitPack](https://jitpack.io/#jihedbfr-art/spring-keycloak-toolkit) — add the JitPack repo,
then pull the tag as a dependency:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>com.github.jihedbfr-art</groupId>
    <artifactId>spring-keycloak-toolkit</artifactId>
    <version>v0.1.0</version>
</dependency>
```

Or build and install locally:

```bash
git clone https://github.com/jihedbfr-art/spring-keycloak-toolkit.git
cd spring-keycloak-toolkit
mvn clean install
```

```xml
<dependency>
    <groupId>com.jihedapps</groupId>
    <artifactId>spring-keycloak-toolkit</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Usage

Point it at your Keycloak client ID and you're done for role mapping:

```yaml
jihedapps:
  keycloak-toolkit:
    resource-id: my-client       # the Keycloak client whose resource_access roles you want
    resource-roles-enabled: true
    realm-roles-enabled: true    # on by default
```

If you want the JSON error bodies too:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http,
        ProblemDetailAuthenticationEntryPoint entryPoint,
        ProblemDetailAccessDeniedHandler accessDeniedHandler) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .exceptionHandling(e -> e
            .authenticationEntryPoint(entryPoint)
            .accessDeniedHandler(accessDeniedHandler));
    return http.build();
}
```

## Configuration reference

| Property | Default | What it does |
|---|---|---|
| `jihedapps.keycloak-toolkit.realm-roles-enabled` | `true` | Read `realm_access.roles` |
| `jihedapps.keycloak-toolkit.resource-roles-enabled` | `true` | Read `resource_access.<resource-id>.roles` |
| `jihedapps.keycloak-toolkit.resource-id` | *(none)* | Keycloak client ID for resource roles; skipped if blank |
| `jihedapps.keycloak-toolkit.role-prefix` | `ROLE_` | Prefix applied before each role |
| `jihedapps.keycloak-toolkit.problem-details-enabled` | `true` | Register the ProblemDetail entry point / handler beans |

## Compatibility

Built and tested against Spring Boot 3.2.5 / Spring Security 6 / Java 17, which is what I run in
production. Older Spring Boot 3.x lines will likely work since the autoconfigure API in scope here
has been stable, but I haven't tested them.

## Roadmap

Things I want to add once this has seen a bit more real use rather than guessing upfront:

- multi-tenant support (more than one `resource-id` at a time)
- a test fixture module (`JwtTestUtils` or similar) so consumers don't hand-roll fake JWTs the way
  the tests in this repo do
- Maven Central publishing once GPG signing is set up

## License

MIT — see [LICENSE](LICENSE).
