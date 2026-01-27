package com.tencent.supersonic.auth.api.authentication.enums;

/**
 * Supported OAuth provider types.
 */
public enum OAuthProviderType {
    /**
     * Google OAuth 2.0
     */
    GOOGLE("https://accounts.google.com/o/oauth2/v2/auth", "https://oauth2.googleapis.com/token",
            "https://openidconnect.googleapis.com/v1/userinfo",
            "https://www.googleapis.com/oauth2/v3/certs", "https://accounts.google.com"),

    /**
     * Microsoft Azure Active Directory
     */
    AZURE_AD("https://login.microsoftonline.com/{tenant}/oauth2/v2.0/authorize",
            "https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token",
            "https://graph.microsoft.com/oidc/userinfo",
            "https://login.microsoftonline.com/{tenant}/discovery/v2.0/keys",
            "https://login.microsoftonline.com/{tenant}/v2.0"),

    /**
     * Keycloak
     */
    KEYCLOAK(null, null, null, null, null),

    /**
     * Generic OpenID Connect provider
     */
    GENERIC_OIDC(null, null, null, null, null);

    private final String defaultAuthorizationUri;
    private final String defaultTokenUri;
    private final String defaultUserInfoUri;
    private final String defaultJwksUri;
    private final String defaultIssuer;

    OAuthProviderType(String defaultAuthorizationUri, String defaultTokenUri,
            String defaultUserInfoUri, String defaultJwksUri, String defaultIssuer) {
        this.defaultAuthorizationUri = defaultAuthorizationUri;
        this.defaultTokenUri = defaultTokenUri;
        this.defaultUserInfoUri = defaultUserInfoUri;
        this.defaultJwksUri = defaultJwksUri;
        this.defaultIssuer = defaultIssuer;
    }

    public String getDefaultAuthorizationUri() {
        return defaultAuthorizationUri;
    }

    public String getDefaultTokenUri() {
        return defaultTokenUri;
    }

    public String getDefaultUserInfoUri() {
        return defaultUserInfoUri;
    }

    public String getDefaultJwksUri() {
        return defaultJwksUri;
    }

    public String getDefaultIssuer() {
        return defaultIssuer;
    }

    /**
     * Check if this provider requires explicit endpoint configuration.
     */
    public boolean requiresExplicitEndpoints() {
        return this == KEYCLOAK || this == GENERIC_OIDC;
    }
}
