# Changelog

## 0.1.1 — 2026-09-08

Re-tagged release: the `v0.1.0` git tag was cut before the package rename from
`com.jihedapps.keycloak` to `io.github.jihedbfr_art.keycloak`, so JitPack was still serving the
old package under that tag. No functional changes — `v0.1.1` points at the current `main`, with
the correct package name, the license text fix, and the Maven coordinates clarification.

## 0.1.0 — 2026-07-17

First release.

- `KeycloakRealmRoleConverter`: maps `realm_access.roles` and `resource_access.<clientId>.roles`
  from a Keycloak JWT into `SimpleGrantedAuthority`, configurable prefix (default `ROLE_`).
- Auto-configuration registering a `JwtAuthenticationConverter` wired to the converter — zero
  config needed in the common case.
- `ProblemDetailAuthenticationEntryPoint` and `ProblemDetailAccessDeniedHandler`: RFC 7807 bodies
  for 401/403, exposed as beans, not auto-wired into the filter chain.
