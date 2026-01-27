package com.tencent.supersonic.auth.api.authentication.config;

import com.tencent.supersonic.auth.api.authentication.enums.OAuthProviderType;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for a single OAuth provider.
 */
@Data
public class OAuthProviderConfig {

    /**
     * Provider type (GOOGLE, AZURE_AD, KEYCLOAK, GENERIC_OIDC)
     */
    private OAuthProviderType type = OAuthProviderType.GENERIC_OIDC;

    /**
     * OAuth client ID
     */
    private String clientId;

    /**
     * OAuth client secret
     */
    private String clientSecret;

    /**
     * Authorization endpoint URL
     */
    private String authorizationUri;

    /**
     * Token endpoint URL
     */
    private String tokenUri;

    /**
     * User info endpoint URL
     */
    private String userInfoUri;

    /**
     * JWKS (JSON Web Key Set) endpoint URL
     */
    private String jwksUri;

    /**
     * Token issuer URL
     */
    private String issuer;

    /**
     * Azure AD tenant ID (only for AZURE_AD type)
     */
    private String tenantId;

    /**
     * OAuth scopes to request
     */
    private List<String> scopes = new ArrayList<>(List.of("openid", "profile", "email"));

    /**
     * Whether to enable PKCE (Proof Key for Code Exchange)
     */
    private boolean pkceEnabled = true;

    /**
     * Additional parameters to include in authorization request
     */
    private Map<String, String> additionalParams = new HashMap<>();

    /**
     * Whether this provider is enabled
     */
    private boolean enabled = true;

    /**
     * Get the effective authorization URI, falling back to provider defaults.
     */
    public String getEffectiveAuthorizationUri() {
        if (authorizationUri != null && !authorizationUri.isEmpty()) {
            return resolveTemplateVariables(authorizationUri);
        }
        return resolveTemplateVariables(type.getDefaultAuthorizationUri());
    }

    /**
     * Get the effective token URI, falling back to provider defaults.
     */
    public String getEffectiveTokenUri() {
        if (tokenUri != null && !tokenUri.isEmpty()) {
            return resolveTemplateVariables(tokenUri);
        }
        return resolveTemplateVariables(type.getDefaultTokenUri());
    }

    /**
     * Get the effective user info URI, falling back to provider defaults.
     */
    public String getEffectiveUserInfoUri() {
        if (userInfoUri != null && !userInfoUri.isEmpty()) {
            return resolveTemplateVariables(userInfoUri);
        }
        return resolveTemplateVariables(type.getDefaultUserInfoUri());
    }

    /**
     * Get the effective JWKS URI, falling back to provider defaults.
     */
    public String getEffectiveJwksUri() {
        if (jwksUri != null && !jwksUri.isEmpty()) {
            return resolveTemplateVariables(jwksUri);
        }
        return resolveTemplateVariables(type.getDefaultJwksUri());
    }

    /**
     * Get the effective issuer, falling back to provider defaults.
     */
    public String getEffectiveIssuer() {
        if (issuer != null && !issuer.isEmpty()) {
            return resolveTemplateVariables(issuer);
        }
        return resolveTemplateVariables(type.getDefaultIssuer());
    }

    /**
     * Resolve template variables like {tenant} in URLs.
     */
    private String resolveTemplateVariables(String url) {
        if (url == null) {
            return null;
        }
        if (tenantId != null && !tenantId.isEmpty()) {
            url = url.replace("{tenant}", tenantId);
        }
        return url;
    }

    /**
     * Validate the provider configuration.
     */
    public void validate() {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("OAuth client ID is required");
        }

        if (type.requiresExplicitEndpoints()) {
            if (getEffectiveAuthorizationUri() == null) {
                throw new IllegalArgumentException("Authorization URI is required for " + type);
            }
            if (getEffectiveTokenUri() == null) {
                throw new IllegalArgumentException("Token URI is required for " + type);
            }
        }

        if (type == OAuthProviderType.AZURE_AD && (tenantId == null || tenantId.isEmpty())) {
            throw new IllegalArgumentException("Tenant ID is required for Azure AD");
        }
    }
}
