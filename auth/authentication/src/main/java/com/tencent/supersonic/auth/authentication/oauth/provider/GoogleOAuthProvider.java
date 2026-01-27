package com.tencent.supersonic.auth.authentication.oauth.provider;

import com.tencent.supersonic.auth.api.authentication.enums.OAuthProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Google OAuth 2.0 provider implementation.
 */
@Slf4j
@Component
public class GoogleOAuthProvider extends AbstractOAuthProvider {

    @Override
    public OAuthProviderType getType() {
        return OAuthProviderType.GOOGLE;
    }

    @Override
    public void revokeToken(String token) throws OAuthException {
        // Google supports token revocation
        String revokeUrl = "https://oauth2.googleapis.com/revoke?token=" + token;
        try {
            executePostRequest(revokeUrl, java.util.Collections.emptyMap());
            log.info("Successfully revoked Google token");
        } catch (Exception e) {
            log.warn("Failed to revoke Google token: {}", e.getMessage());
            throw new OAuthException("revoke_failed", "Failed to revoke token: " + e.getMessage(),
                    e);
        }
    }
}
