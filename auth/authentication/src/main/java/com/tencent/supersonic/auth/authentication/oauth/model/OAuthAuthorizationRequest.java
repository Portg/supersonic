package com.tencent.supersonic.auth.authentication.oauth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OAuth authorization request parameters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthAuthorizationRequest {

    /**
     * Authorization endpoint URL
     */
    private String authorizationUri;

    /**
     * OAuth client ID
     */
    private String clientId;

    /**
     * Redirect URI after authorization
     */
    private String redirectUri;

    /**
     * Response type (usually "code" for authorization code flow)
     */
    @Builder.Default
    private String responseType = "code";

    /**
     * Requested scopes
     */
    private List<String> scopes;

    /**
     * State parameter for CSRF protection
     */
    private String state;

    /**
     * Nonce for OIDC
     */
    private String nonce;

    /**
     * PKCE code challenge
     */
    private String codeChallenge;

    /**
     * PKCE code challenge method
     */
    @Builder.Default
    private String codeChallengeMethod = "S256";

    /**
     * Additional parameters
     */
    @Builder.Default
    private Map<String, String> additionalParams = new HashMap<>();

    /**
     * Build the full authorization URL with all parameters.
     */
    public String buildAuthorizationUrl() {
        StringBuilder url = new StringBuilder(authorizationUri);
        url.append(authorizationUri.contains("?") ? "&" : "?");

        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("redirect_uri", redirectUri);
        params.put("response_type", responseType);

        if (scopes != null && !scopes.isEmpty()) {
            params.put("scope", String.join(" ", scopes));
        }

        if (state != null) {
            params.put("state", state);
        }

        if (nonce != null) {
            params.put("nonce", nonce);
        }

        if (codeChallenge != null) {
            params.put("code_challenge", codeChallenge);
            params.put("code_challenge_method", codeChallengeMethod);
        }

        // Add additional parameters
        if (additionalParams != null) {
            params.putAll(additionalParams);
        }

        String queryString = params.entrySet().stream()
                .map(e -> encodeParam(e.getKey()) + "=" + encodeParam(e.getValue()))
                .collect(Collectors.joining("&"));

        url.append(queryString);
        return url.toString();
    }

    private String encodeParam(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
