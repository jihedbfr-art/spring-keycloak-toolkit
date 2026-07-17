# Changelog

## 0.1.0 — 2026-07-17

First release.

- `KeycloakRealmRoleConverter`: maps `realm_access.roles` and `resource_access.<clientId>.roles`
  from a Keycloak JWT into `SimpleGrantedAuthority`, configurable prefix (default `ROLE_`).
- Auto-configuration registering a `JwtAuthenticationConverter` wired to the converter — zero
  config needed in the common case.
- `ProblemDetailAuthenticationEntryPoint` and `ProblemDetailAccessDeniedHandler`: RFC 7807 bodies
  for 401/403, exposed as beans, not auto-wired into the filter chain.
