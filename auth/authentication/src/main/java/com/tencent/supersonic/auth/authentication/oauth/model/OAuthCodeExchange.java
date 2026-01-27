package com.tencent.supersonic.auth.authentication.oauth.model;

import lombok.Data;

import java.time.Instant;

/**
 * Temporary storage for OAuth token exchange. After OAuth callback, tokens are stored with a
 * one-time exchange code. Frontend uses this code to securely retrieve tokens via API.
 */
@Data
public class OAuthCodeExchange {

    private String exchangeCode;
    private String accessToken;
    private String refreshToken;
    private String sessionId;
    private Long userId;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean used;

    public static OAuthCodeExchange create(String exchangeCode, String accessToken,
            String refreshToken, String sessionId, Long userId, long ttlSeconds) {
        OAuthCodeExchange exchange = new OAuthCodeExchange();
        exchange.setExchangeCode(exchangeCode);
        exchange.setAccessToken(accessToken);
        exchange.setRefreshToken(refreshToken);
        exchange.setSessionId(sessionId);
        exchange.setUserId(userId);
        exchange.setCreatedAt(Instant.now());
        exchange.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        exchange.setUsed(false);
        return exchange;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}
