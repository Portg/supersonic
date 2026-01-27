package com.tencent.supersonic.auth.authentication.oauth.service;

import com.tencent.supersonic.auth.authentication.oauth.model.OAuthCodeExchange;
import com.tencent.supersonic.auth.authentication.oauth.storage.CodeExchangeStorage;
import com.tencent.supersonic.auth.authentication.oauth.util.PKCEUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for managing temporary OAuth exchange codes. Exchange codes are short-lived (30 seconds)
 * and can only be used once to retrieve tokens securely via API.
 *
 * <p>
 * Storage backend is configurable: - Redis: for distributed/clustered deployments - Caffeine: for
 * single-node deployments (default)
 * </p>
 */
@Slf4j
@Service
public class OAuthCodeExchangeService {

    private static final long EXCHANGE_CODE_TTL_SECONDS = 30;

    private final CodeExchangeStorage storage;

    public OAuthCodeExchangeService(CodeExchangeStorage storage) {
        this.storage = storage;
        log.info("OAuthCodeExchangeService initialized with storage type: {}",
                storage.getStorageType());
    }

    /**
     * Create a new exchange code for the given tokens.
     */
    public String createExchangeCode(String accessToken, String refreshToken, String sessionId,
            Long userId) {
        String exchangeCode = PKCEUtil.generateState(); // Reuse secure random generator

        OAuthCodeExchange exchange = OAuthCodeExchange.create(exchangeCode, accessToken,
                refreshToken, sessionId, userId, EXCHANGE_CODE_TTL_SECONDS);

        storage.store(exchangeCode, exchange, EXCHANGE_CODE_TTL_SECONDS);
        log.debug("Created exchange code for user: {} using {} storage", userId,
                storage.getStorageType());

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

        Optional<OAuthCodeExchange> exchange = storage.getAndRemove(exchangeCode);

        if (exchange.isPresent()) {
            log.debug("Successfully exchanged code for user: {}", exchange.get().getUserId());
            return exchange.get();
        }

        log.warn("Exchange code not found or invalid: {}", exchangeCode);
        return null;
    }

    /**
     * Get the storage type being used.
     */
    public String getStorageType() {
        return storage.getStorageType();
    }
}
