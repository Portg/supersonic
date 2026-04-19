package com.tencent.supersonic.common.cache;

import com.tencent.supersonic.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisCacheProvider implements CacheProvider {

    private static final String GLOBAL_PREFIX = "s2:cache:";

    private final CacheNamespace namespace;
    private final StringRedisTemplate redis;
    private final long ttlMs;

    public RedisCacheProvider(CacheNamespace namespace, StringRedisTemplate redis) {
        this.namespace = namespace;
        this.redis = redis;
        this.ttlMs = namespace.getTtl().toMillis();
        log.info("Redis cache initialized: namespace={}, ttl={}, tenantScoped={}",
                namespace.getName(), namespace.getTtl(), namespace.isTenantScoped());
    }

    @Override
    public String getName() {
        return namespace.getName();
    }

    @Override
    public CacheNamespace getNamespace() {
        return namespace;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(redis.opsForValue().get(fullKey(key)));
    }

    @Override
    public void put(String key, String value) {
        redis.opsForValue().set(fullKey(key), value, ttlMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean putIfAbsent(String key, String value) {
        Boolean set =
                redis.opsForValue().setIfAbsent(fullKey(key), value, ttlMs, TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(set);
    }

    @Override
    public void evict(String key) {
        redis.delete(fullKey(key));
    }

    @Override
    public long increment(String key) {
        String fk = fullKey(key);
        Long count = redis.opsForValue().increment(fk);
        if (count != null && count == 1L) {
            redis.expire(fk, ttlMs, TimeUnit.MILLISECONDS);
        }
        return count == null ? 0L : count;
    }

    private String fullKey(String key) {
        StringBuilder sb = new StringBuilder(GLOBAL_PREFIX).append(namespace.getName()).append(":");
        if (namespace.isTenantScoped()) {
            Long tenantId = TenantContext.getTenantId();
            sb.append("tenant:").append(tenantId == null ? "_" : tenantId).append(":");
        }
        sb.append(key);
        return sb.toString();
    }
}
