package com.tencent.supersonic.auth.authentication.oauth.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.auth.api.authentication.config.OAuthProviderConfig;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthAuthorizationRequest;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthTokenResponse;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthUserInfo;
import com.tencent.supersonic.auth.authentication.oauth.model.PKCEChallenge;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract base class for OAuth providers.
 */
@Slf4j
public abstract class AbstractOAuthProvider implements OAuthProvider {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected String providerName;
    protected OAuthProviderConfig config;
    protected boolean configured = false;

    @Override
    public void initialize(String providerName, OAuthProviderConfig config) {
        this.providerName = providerName;
        this.config = config;
        this.configured = true;
        log.info("Initialized OAuth provider: {} (type: {})", providerName, getType());
    }

    @Override
    public boolean isConfigured() {
        return configured && config != null;
    }

    @Override
    public OAuthAuthorizationRequest buildAuthorizationRequest(String redirectUri, String state,
            PKCEChallenge pkce, String nonce) {
        OAuthAuthorizationRequest.OAuthAuthorizationRequestBuilder builder =
                OAuthAuthorizationRequest.builder()
                        .authorizationUri(config.getEffectiveAuthorizationUri())
                        .clientId(config.getClientId()).redirectUri(redirectUri)
                        .responseType("code").scopes(config.getScopes()).state(state);

        if (nonce != null) {
            builder.nonce(nonce);
        }

        if (pkce != null && config.isPkceEnabled()) {
            builder.codeChallenge(pkce.getCodeChallenge())
                    .codeChallengeMethod(pkce.getCodeChallengeMethod());
        }

        if (config.getAdditionalParams() != null) {
            builder.additionalParams(new HashMap<>(config.getAdditionalParams()));
        }

        return builder.build();
    }

    @Override
    public OAuthTokenResponse exchangeCodeForTokens(String code, String redirectUri,
            String codeVerifier) throws OAuthException {
        String tokenUri = config.getEffectiveTokenUri();
        if (tokenUri == null || tokenUri.isEmpty()) {
            throw OAuthException.providerNotConfigured(providerName);
        }

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("redirect_uri", redirectUri);
        params.put("client_id", config.getClientId());

        if (config.getClientSecret() != null && !config.getClientSecret().isEmpty()) {
            params.put("client_secret", config.getClientSecret());
        }

        if (codeVerifier != null && config.isPkceEnabled()) {
            params.put("code_verifier", codeVerifier);
        }

        try {
            String responseBody = executePostRequest(tokenUri, params);
            return parseTokenResponse(responseBody);
        } catch (IOException e) {
            throw OAuthException.tokenExchangeFailed(e.getMessage());
        }
    }

    @Override
    public OAuthTokenResponse refreshAccessToken(String refreshToken) throws OAuthException {
        String tokenUri = config.getEffectiveTokenUri();
        if (tokenUri == null || tokenUri.isEmpty()) {
            throw OAuthException.providerNotConfigured(providerName);
        }

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);
        params.put("client_id", config.getClientId());

        if (config.getClientSecret() != null && !config.getClientSecret().isEmpty()) {
            params.put("client_secret", config.getClientSecret());
        }

        try {
            String responseBody = executePostRequest(tokenUri, params);
            return parseTokenResponse(responseBody);
        } catch (IOException e) {
            throw OAuthException.tokenExchangeFailed(e.getMessage());
        }
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) throws OAuthException {
        String userInfoUri = config.getEffectiveUserInfoUri();
        if (userInfoUri == null || userInfoUri.isEmpty()) {
            throw OAuthException.providerNotConfigured(providerName);
        }

        try {
            String responseBody = executeGetRequest(userInfoUri, accessToken);
            return parseUserInfoResponse(responseBody);
        } catch (IOException e) {
            throw OAuthException.userInfoFetchFailed(e.getMessage());
        }
    }

    @Override
    public OAuthUserInfo validateIdToken(String idToken) throws OAuthException {
        // Default implementation just extracts claims from JWT payload
        // Subclasses should override to properly validate signature
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw OAuthException.invalidToken();
            }
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8);
            return parseUserInfoResponse(payload);
        } catch (Exception e) {
            throw new OAuthException("invalid_id_token",
                    "Failed to parse ID token: " + e.getMessage(), e);
        }
    }

    @Override
    public void revokeToken(String token) throws OAuthException {
        // Default implementation - many providers don't support token revocation
        log.warn("Token revocation not supported for provider: {}", providerName);
    }

    /**
     * Execute a POST request with form parameters.
     */
    protected String executePostRequest(String url, Map<String, String> params) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            post.setHeader("Accept", "application/json");

            String body = params.entrySet().stream()
                    .map(e -> encodeParam(e.getKey()) + "=" + encodeParam(e.getValue()))
                    .collect(Collectors.joining("&"));

            post.setEntity(new StringEntity(body, ContentType.APPLICATION_FORM_URLENCODED));

            try (CloseableHttpResponse response = httpClient.execute(post)) {
                HttpEntity entity = response.getEntity();
                String responseBody = entity != null ? EntityUtils.toString(entity) : "";

                if (response.getStatusLine().getStatusCode() >= 400) {
                    log.error("OAuth POST request failed: {} - {}", response.getStatusLine(),
                            responseBody);
                    throw new IOException("Request failed with status "
                            + response.getStatusLine().getStatusCode());
                }

                return responseBody;
            }
        }
    }

    /**
     * Execute a GET request with Bearer token authentication.
     */
    protected String executeGetRequest(String url, String accessToken) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + accessToken);
            get.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(get)) {
                HttpEntity entity = response.getEntity();
                String responseBody = entity != null ? EntityUtils.toString(entity) : "";

                if (response.getStatusLine().getStatusCode() >= 400) {
                    log.error("OAuth GET request failed: {} - {}", response.getStatusLine(),
                            responseBody);
                    throw new IOException("Request failed with status "
                            + response.getStatusLine().getStatusCode());
                }

                return responseBody;
            }
        }
    }

    /**
     * Parse a token response JSON.
     */
    protected OAuthTokenResponse parseTokenResponse(String json) throws IOException {
        JsonNode node = OBJECT_MAPPER.readTree(json);

        // Check for error response
        if (node.has("error")) {
            String error = node.path("error").asText();
            String description = node.path("error_description").asText(error);
            throw new IOException("OAuth error: " + error + " - " + description);
        }

        return OAuthTokenResponse.builder().accessToken(node.path("access_token").asText(null))
                .refreshToken(node.path("refresh_token").asText(null))
                .idToken(node.path("id_token").asText(null))
                .tokenType(node.path("token_type").asText("Bearer"))
                .expiresIn(node.has("expires_in") ? node.path("expires_in").asLong() : null)
                .refreshExpiresIn(
                        node.has("refresh_expires_in") ? node.path("refresh_expires_in").asLong()
                                : null)
                .scope(node.path("scope").asText(null)).build();
    }

    /**
     * Parse a user info response JSON.
     */
    protected OAuthUserInfo parseUserInfoResponse(String json) throws IOException {
        JsonNode node = OBJECT_MAPPER.readTree(json);

        OAuthUserInfo.OAuthUserInfoBuilder builder = OAuthUserInfo.builder().provider(providerName);

        if (node.has("sub")) {
            builder.sub(node.path("sub").asText());
        } else if (node.has("id")) {
            builder.sub(node.path("id").asText());
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

        if (node.has("picture")) {
            builder.picture(node.path("picture").asText());
        }

        if (node.has("locale")) {
            builder.locale(node.path("locale").asText());
        }

        return builder.build();
    }

    private String encodeParam(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
