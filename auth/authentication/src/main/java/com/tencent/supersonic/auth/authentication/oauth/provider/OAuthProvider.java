package com.tencent.supersonic.auth.authentication.oauth.provider;

import com.tencent.supersonic.auth.api.authentication.config.OAuthProviderConfig;
import com.tencent.supersonic.auth.api.authentication.enums.OAuthProviderType;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthAuthorizationRequest;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthTokenResponse;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthUserInfo;
import com.tencent.supersonic.auth.authentication.oauth.model.PKCEChallenge;

/**
 * Interface for OAuth providers.
 */
public interface OAuthProvider {

    /**
     * Get the provider type.
     */
    OAuthProviderType getType();

    /**
     * Initialize the provider with configuration.
     */
    void initialize(String providerName, OAuthProviderConfig config);

    /**
     * Build an authorization request.
     *
     * @param redirectUri the redirect URI
     * @param state the state parameter
     * @param pkce the PKCE challenge (optional)
     * @param nonce the nonce for OIDC (optional)
     * @return the authorization request
     */
    OAuthAuthorizationRequest buildAuthorizationRequest(String redirectUri, String state,
            PKCEChallenge pkce, String nonce);

    /**
     * Exchange an authorization code for tokens.
     *
     * @param code the authorization code
     * @param redirectUri the redirect URI used in the authorization request
     * @param codeVerifier the PKCE code verifier (optional)
     * @return the token response
     * @throws OAuthException if the token exchange fails
     */
    OAuthTokenResponse exchangeCodeForTokens(String code, String redirectUri, String codeVerifier)
            throws OAuthException;

    /**
     * Refresh an access token using a refresh token.
     *
     * @param refreshToken the refresh token
     * @return the new token response
     * @throws OAuthException if the refresh fails
     */
    OAuthTokenResponse refreshAccessToken(String refreshToken) throws OAuthException;

    /**
     * Get user information using an access token.
     *
     * @param accessToken the access token
     * @return the user information
     * @throws OAuthException if fetching user info fails
     */
    OAuthUserInfo getUserInfo(String accessToken) throws OAuthException;

    /**
     * Validate an ID token.
     *
     * @param idToken the ID token
     * @return the user information extracted from the ID token
     * @throws OAuthException if validation fails
     */
    OAuthUserInfo validateIdToken(String idToken) throws OAuthException;

    /**
     * Revoke a token.
     *
     * @param token the token to revoke
     * @throws OAuthException if revocation fails
     */
    void revokeToken(String token) throws OAuthException;

    /**
     * Check if this provider is properly configured.
     */
    boolean isConfigured();
}
