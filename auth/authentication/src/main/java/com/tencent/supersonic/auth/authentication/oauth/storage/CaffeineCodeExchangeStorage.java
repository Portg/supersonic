package com.tencent.supersonic.auth.authentication.oauth.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthCodeExchange;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine-based in-memory implementation of CodeExchangeStorage. Suitable for single-node
 * deployments or development environments.
 */
@Slf4j
public class CaffeineCodeExchangeStorage implements CodeExchangeStorage {

    private static final int MAX_SIZE = 10000;
    private static final long DEFAULT_TTL_SECONDS = 60;

    private Cache<String, OAuthCodeExchange> cache;

    @PostConstruct
    public void init() {
        this.cache = Caffeine.newBuilder().maximumSize(MAX_SIZE)
                .expireAfterWrite(DEFAULT_TTL_SECONDS, TimeUnit.SECONDS).build();
        log.info("Initialized Caffeine-based CodeExchangeStorage");
    }

    public CaffeineCodeExchangeStorage() {
        // Cache will be initialized in @PostConstruct
        // Also initialize here for non-Spring usage
        this.cache = Caffeine.newBuilder().maximumSize(MAX_SIZE)
                .expireAfterWrite(DEFAULT_TTL_SECONDS, TimeUnit.SECONDS).build();
    }

    @Override
    public void store(String code, OAuthCodeExchange exchange, long ttlSeconds) {
        cache.put(code, exchange);
        log.debug("Stored exchange code in Caffeine cache: {}", code);
    }

    @Override
    public Optional<OAuthCodeExchange> getAndRemove(String code) {
        OAuthCodeExchange exchange = cache.getIfPresent(code);
        if (exchange != null) {
            cache.invalidate(code);
            if (exchange.isValid()) {
                exchange.setUsed(true);
                log.debug("Retrieved and removed exchange code from Caffeine cache: {}", code);
                return Optional.of(exchange);
            }
            log.debug("Exchange code expired or already used: {}", code);
        }
        return Optional.empty();
    }

    @Override
    public void remove(String code) {
        cache.invalidate(code);
        log.debug("Removed exchange code from Caffeine cache: {}", code);
    }

    @Override
    public String getStorageType() {
        return "caffeine";
    }

    /**
     * Get current cache size (for monitoring).
     */
    public long getSize() {
        return cache.estimatedSize();
    }

    /**
     * Clean up expired entries.
     */
    public void cleanup() {
        cache.cleanUp();
    }
}
