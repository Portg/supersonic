package com.tencent.supersonic.auth.authentication.oauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthCodeExchange;
import com.tencent.supersonic.auth.authentication.oauth.util.PKCEUtil;
import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for managing temporary OAuth exchange codes. Exchange codes are short-lived (60 seconds)
 * and can only be used once to retrieve tokens securely via API.
 *
 * <p>
 * Storage backend is the unified CacheProvider for the "oauth-code" namespace, which is
 * configurable via {@code s2.cache.namespaces.oauth-code.type} (caffeine or redis).
 * </p>
 */
@Slf4j
@Service
public class OAuthCodeExchangeService {

    private static final long EXCHANGE_CODE_TTL_SECONDS = 60;

    private final CacheProvider cache;
    private final ObjectMapper objectMapper;

    public OAuthCodeExchangeService(CacheProviderRegistry registry) {
        this.cache = registry.require("oauth-code");
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
    }

    /**
     * Create a new exchange code for the given tokens.
     */
    public String createExchangeCode(String accessToken, String refreshToken, String sessionId,
            Long userId) {
        String exchangeCode = PKCEUtil.generateState();
        OAuthCodeExchange exchange = OAuthCodeExchange.create(exchangeCode, accessToken,
                refreshToken, sessionId, userId, EXCHANGE_CODE_TTL_SECONDS);
        try {
            cache.put(exchangeCode, objectMapper.writeValueAsString(exchange));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize exchange code", e);
        }
        log.debug("Created exchange code for user: {}", userId);
        return exchangeCode;
    }

    /**
     * Exchange the code for tokens. This is a one-time operation.
     *
     * @return OAuthCodeExchange if valid, null otherwise
     */
    public OAuthCodeExchange exchangeCodeForTokens(String exchangeCode) {
        if (exchangeCode == null || exchangeCode.isEmpty()) {
            log.warn("Exchange code is null or empty");
            return null;
        }

        Optional<String> raw = cache.getAndEvict(exchangeCode);
        if (raw.isEmpty()) {
            log.warn("Exchange code not found or invalid: {}", exchangeCode);
            return null;
        }

        try {
            OAuthCodeExchange exchange = objectMapper.readValue(raw.get(), OAuthCodeExchange.class);
            if (!exchange.isValid()) {
                log.debug("Exchange code expired or already used: {}", exchangeCode);
                return null;
            }
            exchange.setUsed(true);
            log.debug("Successfully exchanged code for user: {}", exchange.getUserId());
            return exchange;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize exchange code: {}", e.getMessage());
            return null;
        }
    }
}
