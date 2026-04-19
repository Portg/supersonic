package com.tencent.supersonic.headless.core.cache;

import com.tencent.supersonic.common.cache.CacheNamespace;
import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import com.tencent.supersonic.common.cache.CaffeineCacheProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaffeineCacheManagerTest {

    private CacheManager manager;

    @BeforeEach
    void setup() {
        CacheNamespace ns = CacheNamespace.builder().name("semantic-query")
                .ttl(Duration.ofMinutes(10)).maxSize(100).tenantScoped(true).build();
        CacheProvider provider = new CaffeineCacheProvider(ns);
        CacheProviderRegistry registry =
                new CacheProviderRegistry(List.<CacheProvider>of(provider));
        CacheCommonConfig cfg = new CacheCommonConfig();
        cfg.setCacheCommonApp("supersonic");
        cfg.setCacheCommonEnv("test");
        cfg.setCacheCommonVersion(0);
        cfg.setCacheEnable(Boolean.TRUE);
        manager = new CaffeineCacheManager(cfg, registry);
    }

    @Test
    void putThenGet() {
        manager.put("k1", "v1");
        assertThat(manager.get("k1")).isEqualTo("v1");
    }

    @Test
    void removeClearsEntry() {
        manager.put("k1", "v1");
        manager.removeCache("k1");
        assertThat(manager.get("k1")).isNull();
    }

    @Test
    void generateCacheKeyIncludesAppEnvVersionPrefixBody() {
        String key = manager.generateCacheKey("111,222", "abc123");
        assertThat(key).isEqualTo("supersonic:test:0:111,222:abc123");
    }

    @Test
    void generateCacheKeyHandlesEmptyPrefix() {
        String key = manager.generateCacheKey("", "abc");
        assertThat(key).isEqualTo("supersonic:test:0:-1:abc");
    }
}
