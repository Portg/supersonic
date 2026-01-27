package com.tencent.supersonic.auth.authentication.oauth.storage;

import com.tencent.supersonic.auth.authentication.oauth.model.OAuthCodeExchange;

import java.util.Optional;

/**
 * Storage interface for OAuth exchange codes. Implementations can use in-memory cache (Caffeine),
 * Redis, or other storage backends.
 */
public interface CodeExchangeStorage {

    /**
     * Store an exchange code.
     *
     * @param code the exchange code
     * @param exchange the exchange data
     * @param ttlSeconds time-to-live in seconds
     */
    void store(String code, OAuthCodeExchange exchange, long ttlSeconds);

    /**
     * Retrieve and remove an exchange code (one-time use).
     *
     * @param code the exchange code
     * @return the exchange data if found and valid, empty otherwise
     */
    Optional<OAuthCodeExchange> getAndRemove(String code);

    /**
     * Remove an exchange code.
     *
     * @param code the exchange code
     */
    void remove(String code);

    /**
     * Get the storage type name for logging/monitoring.
     */
    String getStorageType();
}
