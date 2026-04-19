package com.tencent.supersonic.feishu.server.cache;

import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Thin facade over {@link CacheProviderRegistry} preserving the historical FeishuCacheService API.
 * Each logical store maps to a dedicated cache namespace in UnifiedCacheAutoConfiguration.
 */
@Slf4j
@Component
public class FeishuCacheFacade {

    private static final String TOKEN_KEY = "tenant_access_token";

    private final CacheProvider dedup;
    private final CacheProvider token;
    private final CacheProvider general;
    private final CacheProvider counter;

    public FeishuCacheFacade(CacheProviderRegistry registry) {
        this.dedup = registry.require("feishu-event-dedup");
        this.token = registry.require("feishu-token");
        this.general = registry.require("feishu-general");
        this.counter = registry.require("feishu-counter");
    }

    public boolean isDuplicateEvent(String eventId) {
        if (eventId == null)
            return false;
        return !dedup.putIfAbsent(eventId, "1");
    }

    public String getToken() {
        return token.get(TOKEN_KEY).orElse(null);
    }

    public void putToken(String value) {
        token.put(TOKEN_KEY, value);
    }

    public String get(String key) {
        return general.get(key).orElse(null);
    }

    public void put(String key, String value) {
        general.put(key, value);
    }

    public void remove(String key) {
        general.evict(key);
    }

    public long incrementCounter(String key) {
        return counter.increment(key);
    }
}
