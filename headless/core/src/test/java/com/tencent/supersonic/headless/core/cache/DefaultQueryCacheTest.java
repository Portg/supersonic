package com.tencent.supersonic.headless.core.cache;

import com.tencent.supersonic.common.cache.CacheNamespace;
import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import com.tencent.supersonic.common.cache.CaffeineCacheProvider;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.util.ContextUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultQueryCacheTest {

    private GenericApplicationContext context;
    private DefaultQueryCache queryCache;
    private CacheManager cacheManager;

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
        cacheManager = new CaffeineCacheManager(cfg, registry);

        context = new GenericApplicationContext();
        context.registerBean(CacheManager.class, () -> cacheManager);
        context.registerBean(CacheCommonConfig.class, () -> cfg);
        context.refresh();
        new ContextUtils().setApplicationContext(context);

        queryCache = new DefaultQueryCache();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        context.close();
    }

    @Test
    void asyncPutUsesSubmittingTenantContext() throws Exception {
        TenantContext.setTenantId(42L);

        assertThat(queryCache.put("tenant-key", "tenant-value")).isTrue();
        awaitCachedValue("tenant-key", "tenant-value");

        TenantContext.setTenantId(7L);
        assertThat(cacheManager.get("tenant-key")).isNull();

        TenantContext.setTenantId(42L);
        assertThat(cacheManager.get("tenant-key")).isEqualTo("tenant-value");
    }

    private void awaitCachedValue(String key, String expected) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (System.nanoTime() < deadline) {
            Object value = cacheManager.get(key);
            if (expected.equals(value)) {
                return;
            }
            Thread.sleep(10);
        }
        assertThat(cacheManager.get(key)).isEqualTo(expected);
    }
}
