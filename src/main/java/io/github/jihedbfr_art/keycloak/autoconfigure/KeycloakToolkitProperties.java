package io.github.jihedbfr_art.keycloak.autoconfigure;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Keycloak Toolkit Spring Boot starter.
 *
 * <p>All properties are bound under the prefix {@code jihedailabs.keycloak}.
 */
@ConfigurationProperties(prefix = "jihedailabs.keycloak")
public class KeycloakToolkitProperties implements InitializingBean {

    /**
     * Whether to extract realm-level roles from {@code realm_access.roles} in the incoming JWT.
     */
    private boolean realmRolesEnabled = true;

    /**
     * Whether to extract client-level roles from {@code resource_access.<resourceId>.roles} in the incoming JWT.
     * Only active if {@code resourceId} is also configured.
     */
    private boolean resourceRolesEnabled = true;

    /**
     * Authority prefix applied to extracted roles (e.g. {@code "ROLE_"}), matching Spring Security conventions.
     */
    private String rolePrefix = "ROLE_";

    /**
     * Keycloak client identifier whose {@code resource_access} roles should be mapped into authorities.
     * If null or blank, resource-level role extraction is skipped.
     */
    private String resourceId;

    /**
     * Whether to automatically register RFC 7807 problem details beans for authentication entry point and access denied handling.
     */
    private boolean problemDetailsEnabled = true;

    /**
     * Default constructor for Spring Boot configuration properties binding.
     */
    public KeycloakToolkitProperties() {
    }

    /**
     * Enforces programmatic validation of properties upon bean initialization,
     * ensuring validation is guaranteed across all environments without external JSR-303 providers.
     */
    @Override
    public void afterPropertiesSet() {
        validate();
    }

    /**
     * Validates property values programmatically.
     *
     * @throws IllegalArgumentException if {@code rolePrefix} is null or blank
     */
    public void validate() {
        if (rolePrefix == null || rolePrefix.isBlank()) {
            throw new IllegalArgumentException("Property 'jihedailabs.keycloak.role-prefix' must not be blank.");
        }
    }

    /**
     * Indicates whether realm role extraction from {@code realm_access.roles} is enabled.
     *
     * @return {@code true} if realm role extraction is enabled, {@code false} otherwise
     */
    public boolean isRealmRolesEnabled() {
        return realmRolesEnabled;
    }

    /**
     * Configures whether realm role extraction from {@code realm_access.roles} is enabled.
     *
     * @param realmRolesEnabled {@code true} to enable realm role extraction
     */
    public void setRealmRolesEnabled(boolean realmRolesEnabled) {
        this.realmRolesEnabled = realmRolesEnabled;
    }

    /**
     * Indicates whether client role extraction from {@code resource_access} is enabled.
     *
     * @return {@code true} if resource role extraction is enabled, {@code false} otherwise
     */
    public boolean isResourceRolesEnabled() {
        return resourceRolesEnabled;
    }

    /**
     * Configures whether client role extraction from {@code resource_access} is enabled.
     *
     * @param resourceRolesEnabled {@code true} to enable resource role extraction
     */
    public void setResourceRolesEnabled(boolean resourceRolesEnabled) {
        this.resourceRolesEnabled = resourceRolesEnabled;
    }

    /**
     * Gets the authority prefix applied to each mapped role.
     *
     * @return the role authority prefix (e.g. {@code "ROLE_"})
     */
    public String getRolePrefix() {
        return rolePrefix;
    }

    /**
     * Sets the authority prefix applied to each mapped role.
     *
     * @param rolePrefix the authority prefix to use (must not be blank)
     */
    public void setRolePrefix(String rolePrefix) {
        this.rolePrefix = rolePrefix;
    }

    /**
     * Gets the configured Keycloak client ID for client-specific role extraction.
     *
     * @return the Keycloak client ID, or {@code null} if not configured
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Sets the Keycloak client ID for client-specific role extraction.
     *
     * @param resourceId the Keycloak client ID
     */
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * Indicates whether RFC 7807 problem details beans are enabled.
     *
     * @return {@code true} if problem details beans should be registered, {@code false} otherwise
     */
    public boolean isProblemDetailsEnabled() {
        return problemDetailsEnabled;
    }

    /**
     * Configures whether RFC 7807 problem details beans are enabled.
     *
     * @param problemDetailsEnabled {@code true} to register problem details beans
     */
    public void setProblemDetailsEnabled(boolean problemDetailsEnabled) {
        this.problemDetailsEnabled = problemDetailsEnabled;
    }
}
