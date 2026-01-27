package com.tencent.supersonic.auth.authentication.oauth.service;

import com.tencent.supersonic.auth.api.authentication.config.OAuthConfig;
import com.tencent.supersonic.auth.api.authentication.config.OAuthProviderConfig;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthAuthorizationRequest;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthTokenResponse;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthUserInfo;
import com.tencent.supersonic.auth.authentication.oauth.model.PKCEChallenge;
import com.tencent.supersonic.auth.authentication.oauth.provider.OAuthException;
import com.tencent.supersonic.auth.authentication.oauth.provider.OAuthProvider;
import com.tencent.supersonic.auth.authentication.oauth.provider.OAuthProviderFactory;
import com.tencent.supersonic.auth.authentication.oauth.util.PKCEUtil;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.OAuthStateDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.OAuthTokenDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Main OAuth service coordinating authentication flow.
 */
@Slf4j
@Service
public class OAuthService {

    private final OAuthConfig oauthConfig;
    private final OAuthProviderFactory providerFactory;
    private final OAuthStateService stateService;
    private final OAuthTokenStoreService tokenStoreService;

    public OAuthService(OAuthConfig oauthConfig, OAuthProviderFactory providerFactory,
            OAuthStateService stateService, OAuthTokenStoreService tokenStoreService) {
        this.oauthConfig = oauthConfig;
        this.providerFactory = providerFactory;
        this.stateService = stateService;
        this.tokenStoreService = tokenStoreService;
    }

    /**
     * Check if OAuth is enabled.
     */
    public boolean isEnabled() {
        return oauthConfig.isEnabled();
    }

    /**
     * Get available OAuth provider names.
     */
    public Set<String> getAvailableProviders() {
        return providerFactory.getAvailableProviders();
    }

    /**
     * Check if a provider is available.
     */
    public boolean isProviderAvailable(String providerName) {
        return providerFactory.hasProvider(providerName);
    }

    /**
     * Start the OAuth authorization flow.
     *
     * @param providerName the OAuth provider name
     * @return the authorization URL to redirect the user to
     */
    public String startAuthorization(String providerName) throws OAuthException {
        if (!isEnabled()) {
            throw new OAuthException("oauth_disabled", "OAuth authentication is not enabled");
        }

        OAuthProvider provider = providerFactory.getProvider(providerName);
        OAuthProviderConfig config = oauthConfig.getProvider(providerName);

        if (config == null) {
            throw OAuthException.providerNotConfigured(providerName);
        }

        String redirectUri = oauthConfig.getCallbackUrl(providerName);

        // Create state with PKCE
        OAuthStateDO stateDO =
                stateService.createState(providerName, redirectUri, config.isPkceEnabled());

        PKCEChallenge pkce = null;
        if (config.isPkceEnabled() && stateDO.getCodeVerifier() != null) {
            pkce = new PKCEChallenge(stateDO.getCodeVerifier(),
                    PKCEUtil.generateCodeChallenge(stateDO.getCodeVerifier()), "S256");
        }

        OAuthAuthorizationRequest request = provider.buildAuthorizationRequest(redirectUri,
                stateDO.getState(), pkce, stateDO.getNonce());

        String authUrl = request.buildAuthorizationUrl();
        log.info("Starting OAuth authorization for provider: {}", providerName);

        return authUrl;
    }

    /**
     * Handle the OAuth callback.
     *
     * @param providerName the OAuth provider name
     * @param code the authorization code
     * @param state the state parameter
     * @return the user information from the OAuth provider
     */
    public OAuthCallbackResult handleCallback(String providerName, String code, String state)
            throws OAuthException {
        if (!isEnabled()) {
            throw new OAuthException("oauth_disabled", "OAuth authentication is not enabled");
        }

        // Validate state
        OAuthStateDO stateDO = stateService.validateAndConsumeState(state);

        if (!providerName.equals(stateDO.getProviderName())) {
            throw new OAuthException("provider_mismatch", "Provider name does not match state");
        }

        OAuthProvider provider = providerFactory.getProvider(providerName);

        // Exchange code for tokens
        OAuthTokenResponse tokenResponse = provider.exchangeCodeForTokens(code,
                stateDO.getRedirectUri(), stateDO.getCodeVerifier());

        // Get user info
        OAuthUserInfo userInfo;
        if (tokenResponse.hasIdToken()) {
            try {
                userInfo = provider.validateIdToken(tokenResponse.getIdToken());
            } catch (OAuthException e) {
                log.warn("Failed to validate ID token, falling back to user info endpoint: {}",
                        e.getMessage());
                userInfo = provider.getUserInfo(tokenResponse.getAccessToken());
            }
        } else {
            userInfo = provider.getUserInfo(tokenResponse.getAccessToken());
        }

        userInfo.setProvider(providerName);

        log.info("OAuth callback successful for provider: {}, user: {}", providerName,
                userInfo.getUsername());

        return new OAuthCallbackResult(userInfo, tokenResponse);
    }

    /**
     * Store OAuth tokens for a user.
     */
    public void storeTokens(Long userId, String providerName, OAuthTokenResponse tokenResponse) {
        tokenStoreService.storeTokens(userId, providerName, tokenResponse);
    }

    /**
     * Refresh OAuth tokens for a user.
     */
    public OAuthTokenResponse refreshTokens(Long userId, String providerName)
            throws OAuthException {
        OAuthTokenDO tokenDO = tokenStoreService.getTokens(userId, providerName);
        if (tokenDO == null || !tokenStoreService.hasValidRefreshToken(tokenDO)) {
            throw new OAuthException("no_refresh_token", "No valid refresh token available");
        }

        OAuthProvider provider = providerFactory.getProvider(providerName);
        OAuthTokenResponse newTokens = provider.refreshAccessToken(tokenDO.getRefreshToken());

        // Store updated tokens
        tokenStoreService.storeTokens(userId, providerName, newTokens);

        log.info("Refreshed OAuth tokens for user: {}, provider: {}", userId, providerName);
        return newTokens;
    }

    /**
     * Unlink OAuth provider from a user.
     */
    public void unlinkProvider(Long userId, String providerName) throws OAuthException {
        OAuthTokenDO tokenDO = tokenStoreService.getTokens(userId, providerName);
        if (tokenDO != null) {
            // Try to revoke tokens at the provider
            try {
                OAuthProvider provider = providerFactory.getProvider(providerName);
                if (tokenDO.getAccessToken() != null) {
                    provider.revokeToken(tokenDO.getAccessToken());
                }
                if (tokenDO.getRefreshToken() != null) {
                    provider.revokeToken(tokenDO.getRefreshToken());
                }
            } catch (OAuthException e) {
                log.warn("Failed to revoke tokens at provider: {}", e.getMessage());
            }

            // Delete stored tokens
            tokenStoreService.deleteTokens(userId, providerName);
        }

        log.info("Unlinked OAuth provider {} from user {}", providerName, userId);
    }

    /**
     * Result of an OAuth callback.
     */
    public static class OAuthCallbackResult {
        private final OAuthUserInfo userInfo;
        private final OAuthTokenResponse tokenResponse;

        public OAuthCallbackResult(OAuthUserInfo userInfo, OAuthTokenResponse tokenResponse) {
            this.userInfo = userInfo;
            this.tokenResponse = tokenResponse;
        }

        public OAuthUserInfo getUserInfo() {
            return userInfo;
        }

        public OAuthTokenResponse getTokenResponse() {
            return tokenResponse;
        }
    }
}
