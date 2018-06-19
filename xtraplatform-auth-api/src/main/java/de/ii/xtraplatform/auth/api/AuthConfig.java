package de.ii.xtraplatform.auth.api;

/**
 * @author zahnen
 */
public interface AuthConfig {
    boolean isJwt();

    String getJwtValidationUrl();

    String getUserInfoUrl();

    boolean isActive();

    String getConnectionInfoEndpoint();

    String getUserNameKey();

    String getUserRoleKey();
}
