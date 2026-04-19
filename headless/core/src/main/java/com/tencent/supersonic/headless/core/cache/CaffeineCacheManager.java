package com.tencent.supersonic.headless.core.cache;

import com.google.common.base.Joiner;
import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CaffeineCacheManager implements CacheManager {

    private static final String SEMANTIC_QUERY_RESP_PREFIX = "__s2_cache_semantic_query_resp__:";
    private static final String STRING_ESCAPE_PREFIX = "__s2_cache_string__:";

    private final CacheCommonConfig cacheCommonConfig;
    private final CacheProvider provider;

    // Explicit constructor: registry.require() throws at startup if namespace is missing
    // (fail-fast).
    public CaffeineCacheManager(CacheCommonConfig cacheCommonConfig,
            CacheProviderRegistry registry) {
        this.cacheCommonConfig = cacheCommonConfig;
        this.provider = registry.require("semantic-query");
    }

    @Override
    public Boolean put(String key, Object value) {
        log.debug("[put cache] key:{}", key);
        provider.put(key, encode(value));
        return true;
    }

    @Override
    public Object get(String key) {
        Object value = provider.get(key).map(this::decode).orElse(null);
        log.debug("[get cache] key:{}, hit:{}", key, value != null);
        return value;
    }

    @Override
    public String generateCacheKey(String prefix, String body) {
        if (StringUtils.isEmpty(prefix)) {
            prefix = "-1";
        }
        return Joiner.on(":").useForNull("null").join(cacheCommonConfig.getCacheCommonApp(),
                cacheCommonConfig.getCacheCommonEnv(), cacheCommonConfig.getCacheCommonVersion(),
                prefix, body);
    }

    @Override
    public Boolean removeCache(String key) {
        provider.evict(key);
        return true;
    }

    private String encode(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String s) {
            if (s.startsWith(SEMANTIC_QUERY_RESP_PREFIX) || s.startsWith(STRING_ESCAPE_PREFIX)) {
                return STRING_ESCAPE_PREFIX + s;
            }
            return s;
        }
        if (value instanceof SemanticQueryResp) {
            return SEMANTIC_QUERY_RESP_PREFIX + JsonUtil.toString(value);
        }
        return value.toString();
    }

    private Object decode(String value) {
        if (value.startsWith(STRING_ESCAPE_PREFIX)) {
            return value.substring(STRING_ESCAPE_PREFIX.length());
        }
        if (!value.startsWith(SEMANTIC_QUERY_RESP_PREFIX)) {
            return value;
        }
        String json = value.substring(SEMANTIC_QUERY_RESP_PREFIX.length());
        try {
            return JsonUtil.toObject(json, SemanticQueryResp.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize cached value as SemanticQueryResp", e);
            return null;
        }
    }
}
