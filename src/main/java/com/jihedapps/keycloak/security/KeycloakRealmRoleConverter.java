package com.jihedapps.keycloak.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps a Keycloak access token onto Spring Security authorities.
 *
 * <p>Keycloak does not put roles in the {@code scope} claim the way
 * {@link org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter}
 * expects. Realm roles live under {@code realm_access.roles} and client roles under
 * {@code resource_access.<clientId>.roles}. This converter reads both and turns them into
 * {@code ROLE_*} authorities, which is what {@code hasRole(...)} / {@code @PreAuthorize} expect.
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";

    private final boolean realmRolesEnabled;
    private final boolean resourceRolesEnabled;
    private final String rolePrefix;
    private final String resourceId;

    public KeycloakRealmRoleConverter(boolean realmRolesEnabled, boolean resourceRolesEnabled,
                                       String rolePrefix, String resourceId) {
        this.realmRolesEnabled = realmRolesEnabled;
        this.resourceRolesEnabled = resourceRolesEnabled;
        this.rolePrefix = rolePrefix;
        this.resourceId = resourceId;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();

        if (realmRolesEnabled) {
            roles.addAll(rolesFrom(jwt.getClaimAsMap(REALM_ACCESS_CLAIM)));
        }

        if (resourceRolesEnabled && resourceId != null && !resourceId.isBlank()) {
            Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS_CLAIM);
            if (resourceAccess != null && resourceAccess.get(resourceId) instanceof Map<?, ?> clientAccess) {
                @SuppressWarnings("unchecked")
                Map<String, Object> clientAccessMap = (Map<String, Object>) clientAccess;
                roles.addAll(rolesFrom(clientAccessMap));
            }
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(rolePrefix + role.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toUnmodifiableSet());
    }

    private Collection<String> rolesFrom(Map<String, Object> access) {
        if (access == null || !(access.get(ROLES_CLAIM) instanceof List<?> roles)) {
            return Set.of();
        }
        return roles.stream().map(String::valueOf).collect(Collectors.toSet());
    }
}
