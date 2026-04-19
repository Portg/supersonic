package com.tencent.supersonic.common.cache;

import com.tencent.supersonic.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisCacheProvider implements CacheProvider {

    private static final String GLOBAL_PREFIX = "s2:cache:";

    // Atomically INCR and, on first increment, set the TTL in one round-trip.
    // Avoids a window where INCR succeeds but EXPIRE never runs (JVM crash, network error),
    // which would leave the counter key without expiry and break fixed-window semantics.
    private static final DefaultRedisScript<Long> INCR_AND_EXPIRE_SCRIPT = new DefaultRedisScript<>(
            "local n = redis.call('INCR', KEYS[1])\n"
                    + "if n == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end\n" + "return n",
            Long.class);

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
    public Optional<String> getAndEvict(String key) {
        return Optional.ofNullable(redis.opsForValue().getAndDelete(fullKey(key)));
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
        Long count =
                redis.execute(INCR_AND_EXPIRE_SCRIPT, List.of(fullKey(key)), String.valueOf(ttlMs));
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
