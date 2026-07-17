package com.jihedapps.keycloak.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jihedapps.keycloak-toolkit")
public class KeycloakToolkitProperties {

    /** Read realm_access.roles from the token. Enabled by default since almost every realm sets these. */
    private boolean realmRolesEnabled = true;

    /** Read resource_access.<resourceId>.roles from the token. Off unless resourceId is set. */
    private boolean resourceRolesEnabled = true;

    /** Prefix applied before each role, matching Spring Security's hasRole()/hasAuthority() convention. */
    private String rolePrefix = "ROLE_";

    /** Keycloak client ID whose resource_access roles should be picked up. Leave blank to skip client roles. */
    private String resourceId;

    /** Wire a ProblemDetail-based AuthenticationEntryPoint/AccessDeniedHandler bean pair. */
    private boolean problemDetailsEnabled = true;

    public boolean isRealmRolesEnabled() {
        return realmRolesEnabled;
    }

    public void setRealmRolesEnabled(boolean realmRolesEnabled) {
        this.realmRolesEnabled = realmRolesEnabled;
    }

    public boolean isResourceRolesEnabled() {
        return resourceRolesEnabled;
    }

    public void setResourceRolesEnabled(boolean resourceRolesEnabled) {
        this.resourceRolesEnabled = resourceRolesEnabled;
    }

    public String getRolePrefix() {
        return rolePrefix;
    }

    public void setRolePrefix(String rolePrefix) {
        this.rolePrefix = rolePrefix;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public boolean isProblemDetailsEnabled() {
        return problemDetailsEnabled;
    }

    public void setProblemDetailsEnabled(boolean problemDetailsEnabled) {
        this.problemDetailsEnabled = problemDetailsEnabled;
    }
}
