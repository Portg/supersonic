package com.tencent.supersonic.auth.authentication.oauth.provider;

import com.tencent.supersonic.auth.api.authentication.enums.OAuthProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Generic OpenID Connect provider implementation. Works with any standard OIDC-compliant identity
 * provider.
 */
@Slf4j
@Component
public class GenericOIDCProvider extends AbstractOAuthProvider {

    @Override
    public OAuthProviderType getType() {
        return OAuthProviderType.GENERIC_OIDC;
    }
}
