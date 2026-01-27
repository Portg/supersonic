package com.tencent.supersonic.auth.authentication.oauth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OAuth token response from the token endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthTokenResponse {

    /**
     * The access token
     */
    private String accessToken;

    /**
     * The refresh token (optional)
     */
    private String refreshToken;

    /**
     * The ID token (for OIDC)
     */
    private String idToken;

    /**
     * Token type (usually "Bearer")
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Access token expiration time in seconds
     */
    private Long expiresIn;

    /**
     * Granted scopes (space-separated)
     */
    private String scope;

    /**
     * Refresh token expiration time in seconds (optional)
     */
    private Long refreshExpiresIn;

    /**
     * Check if the response includes a refresh token.
     */
    public boolean hasRefreshToken() {
        return refreshToken != null && !refreshToken.isEmpty();
    }

    /**
     * Check if the response includes an ID token.
     */
    public boolean hasIdToken() {
        return idToken != null && !idToken.isEmpty();
    }
}
