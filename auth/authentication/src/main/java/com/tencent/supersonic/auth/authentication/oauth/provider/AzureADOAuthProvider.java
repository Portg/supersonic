package com.tencent.supersonic.auth.authentication.oauth.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.tencent.supersonic.auth.api.authentication.enums.OAuthProviderType;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Microsoft Azure Active Directory OAuth 2.0 provider implementation.
 */
@Slf4j
@Component
public class AzureADOAuthProvider extends AbstractOAuthProvider {

    @Override
    public OAuthProviderType getType() {
        return OAuthProviderType.AZURE_AD;
    }

    @Override
    protected OAuthUserInfo parseUserInfoResponse(String json) throws IOException {
        JsonNode node = OBJECT_MAPPER.readTree(json);

        OAuthUserInfo.OAuthUserInfoBuilder builder = OAuthUserInfo.builder().provider(providerName);

        // Azure AD uses different claim names
        if (node.has("sub")) {
            builder.sub(node.path("sub").asText());
        } else if (node.has("oid")) {
            builder.sub(node.path("oid").asText());
        } else if (node.has("id")) {
            builder.sub(node.path("id").asText());
        }

        if (node.has("name")) {
            builder.name(node.path("name").asText());
        } else if (node.has("displayName")) {
            builder.name(node.path("displayName").asText());
        }

        if (node.has("given_name")) {
            builder.givenName(node.path("given_name").asText());
        } else if (node.has("givenName")) {
            builder.givenName(node.path("givenName").asText());
        }

        if (node.has("family_name")) {
            builder.familyName(node.path("family_name").asText());
        } else if (node.has("surname")) {
            builder.familyName(node.path("surname").asText());
        }

        if (node.has("email")) {
            builder.email(node.path("email").asText());
        } else if (node.has("mail")) {
            builder.email(node.path("mail").asText());
        } else if (node.has("userPrincipalName")) {
            builder.email(node.path("userPrincipalName").asText());
        }

        if (node.has("email_verified")) {
            builder.emailVerified(node.path("email_verified").asBoolean());
        }

        if (node.has("picture")) {
            builder.picture(node.path("picture").asText());
        }

        if (node.has("locale")) {
            builder.locale(node.path("locale").asText());
        } else if (node.has("preferredLanguage")) {
            builder.locale(node.path("preferredLanguage").asText());
        }

        return builder.build();
    }
}
