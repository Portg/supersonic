package com.tencent.supersonic.headless.core.cache;

import com.google.common.base.Joiner;
import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CaffeineCacheManager implements CacheManager {

    private final CacheCommonConfig cacheCommonConfig;
    private final CacheProvider provider;

    public CaffeineCacheManager(CacheCommonConfig cacheCommonConfig,
            CacheProviderRegistry registry) {
        this.cacheCommonConfig = cacheCommonConfig;
        this.provider = registry.require("semantic-query");
    }

    @Override
    public Boolean put(String key, Object value) {
        log.debug("[put cache] key:{}", key);
        provider.put(key, value == null ? "" : value.toString());
        return true;
    }

    @Override
    public Object get(String key) {
        Object value = provider.get(key).orElse(null);
        log.debug("[get cache] key:{}, hit:{}", key, value != null);
        return value;
    }

    @Override
    public String generateCacheKey(String prefix, String body) {
        if (StringUtils.isEmpty(prefix)) {
            prefix = "-1";
        }
        return Joiner.on(":").join(cacheCommonConfig.getCacheCommonApp(),
                cacheCommonConfig.getCacheCommonEnv(), cacheCommonConfig.getCacheCommonVersion(),
                prefix, body);
    }

    @Override
    public Boolean removeCache(String key) {
        provider.evict(key);
        return true;
    }
}
