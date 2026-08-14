package io.github.jihedbfr_art.keycloak.security;

import java.util.Collection;
import java.util.Collections;
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
 * Converts Keycloak JWT access token claims into a collection of Spring Security {@link GrantedAuthority}.
 *
 * <p>Keycloak stores roles in structured token claims rather than the flat {@code scope} claim:
 * <ul>
 *   <li>Realm-level roles live under {@code realm_access.roles}.</li>
 *   <li>Client-level roles live under {@code resource_access.<clientId>.roles}.</li>
 * </ul>
 *
 * <p>This converter extracts roles from either or both locations and prefixes each with a configurable
 * authority prefix (defaults to {@code ROLE_}), converting values to uppercase to match standard
 * Spring Security authorization rules such as {@code hasRole("ADMIN")} or {@code @PreAuthorize("hasRole('ADMIN')")}.
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";

    private final boolean realmRolesEnabled;
    private final boolean resourceRolesEnabled;
    private final String rolePrefix;
    private final String resourceId;

    /**
     * Constructs a new {@code KeycloakRealmRoleConverter} with the specified extraction settings.
     *
     * @param realmRolesEnabled    whether to extract realm-level roles from {@code realm_access.roles}
     * @param resourceRolesEnabled whether to extract client-level roles from {@code resource_access.<resourceId>.roles}
     * @param rolePrefix           the authority prefix to prepend to each role (e.g. {@code "ROLE_"})
     * @param resourceId           the Keycloak client ID for resource-level role extraction, or {@code null} to skip
     */
    public KeycloakRealmRoleConverter(boolean realmRolesEnabled, boolean resourceRolesEnabled,
                                       String rolePrefix, String resourceId) {
        this.realmRolesEnabled = realmRolesEnabled;
        this.resourceRolesEnabled = resourceRolesEnabled;
        this.rolePrefix = rolePrefix != null ? rolePrefix : "ROLE_";
        this.resourceId = resourceId;
    }

    /**
     * Converts the given JWT access token into a collection of {@link GrantedAuthority}.
     *
     * @param jwt the source JWT access token to extract roles from
     * @return an unmodifiable collection of granted authorities mapped from the token roles
     */
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        if (jwt == null) {
            return Collections.emptySet();
        }

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

    /**
     * Extracts the string list of roles from an access claim map.
     *
     * @param access the access claim map containing a {@code roles} list
     * @return a collection of role names, or an empty set if absent
     */
    private Collection<String> rolesFrom(Map<String, Object> access) {
        if (access == null || !(access.get(ROLES_CLAIM) instanceof List<?> roles)) {
            return Collections.emptySet();
        }
        return roles.stream().map(String::valueOf).collect(Collectors.toSet());
    }
}
