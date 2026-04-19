package com.tencent.supersonic.feishu.api.cache;

/**
 * Abstraction for Feishu caching: event deduplication, token caching, general-purpose KV cache, and
 * rate-limit counters.
 */
public interface FeishuCacheService {

    boolean isDuplicateEvent(String eventId);

    String getToken();

    void putToken(String token);

    String get(String key);

    void put(String key, String value);

    void remove(String key);

    long incrementCounter(String key);
}
