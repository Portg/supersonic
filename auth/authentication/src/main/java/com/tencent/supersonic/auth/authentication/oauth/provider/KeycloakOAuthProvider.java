package com.tencent.supersonic.auth.authentication.oauth.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tencent.supersonic.auth.api.authentication.enums.OAuthProviderType;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Keycloak OAuth 2.0 / OIDC provider implementation.
 */
@Slf4j
@Component
public class KeycloakOAuthProvider extends AbstractOAuthProvider {

    @Override
    public OAuthProviderType getType() {
        return OAuthProviderType.KEYCLOAK;
    }

    @Override
    public void revokeToken(String token) throws OAuthException {
        // Keycloak supports token revocation via the token endpoint
        String tokenUri = config.getEffectiveTokenUri();
        if (tokenUri == null || tokenUri.isEmpty()) {
            throw OAuthException.providerNotConfigured(providerName);
        }

        // Keycloak revocation endpoint is typically at /protocol/openid-connect/revoke
        String revokeUri = tokenUri.replace("/token", "/revoke");

        Map<String, String> params = new HashMap<>();
        params.put("token", token);
        params.put("client_id", config.getClientId());
        if (config.getClientSecret() != null && !config.getClientSecret().isEmpty()) {
            params.put("client_secret", config.getClientSecret());
        }

        try {
            executePostRequest(revokeUri, params);
            log.info("Successfully revoked Keycloak token");
        } catch (Exception e) {
            log.warn("Failed to revoke Keycloak token: {}", e.getMessage());
            throw new OAuthException("revoke_failed", "Failed to revoke token: " + e.getMessage(),
                    e);
        }
    }

    @Override
    protected OAuthUserInfo parseUserInfoResponse(String json) throws IOException {
        JsonNode node = OBJECT_MAPPER.readTree(json);

        OAuthUserInfo.OAuthUserInfoBuilder builder = OAuthUserInfo.builder().provider(providerName);

        // Keycloak standard OIDC claims
        if (node.has("sub")) {
            builder.sub(node.path("sub").asText());
        }

        if (node.has("name")) {
            builder.name(node.path("name").asText());
        }

        if (node.has("given_name")) {
            builder.givenName(node.path("given_name").asText());
        }

        if (node.has("family_name")) {
            builder.familyName(node.path("family_name").asText());
        }

        if (node.has("email")) {
            builder.email(node.path("email").asText());
        }

        if (node.has("email_verified")) {
            builder.emailVerified(node.path("email_verified").asBoolean());
        }

        // Keycloak-specific: preferred_username is commonly used
        if (node.has("preferred_username")) {
            String preferredUsername = node.path("preferred_username").asText();
            // If email is not set, use preferred_username if it looks like an email
            if (builder.build().getEmail() == null && preferredUsername.contains("@")) {
                builder.email(preferredUsername);
            }
        }

        if (node.has("picture")) {
            builder.picture(node.path("picture").asText());
        }

        if (node.has("locale")) {
            builder.locale(node.path("locale").asText());
        }

        return builder.build();
    }
}
