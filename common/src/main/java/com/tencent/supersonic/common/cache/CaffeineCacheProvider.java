package com.tencent.supersonic.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.tencent.supersonic.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class CaffeineCacheProvider implements CacheProvider {

    private final CacheNamespace namespace;
    private final Cache<String, String> values;
    private final Cache<String, AtomicLong> counters;

    public CaffeineCacheProvider(CacheNamespace namespace) {
        this(namespace, Ticker.systemTicker());
    }

    public CaffeineCacheProvider(CacheNamespace namespace, Ticker ticker) {
        this.namespace = namespace;
        this.values = Caffeine.newBuilder().ticker(ticker).expireAfterWrite(namespace.getTtl())
                .maximumSize(namespace.getMaxSize()).build();
        this.counters = Caffeine.newBuilder().ticker(ticker).expireAfterWrite(namespace.getTtl())
                .maximumSize(namespace.getMaxSize()).build();
        log.info("Caffeine cache initialized: namespace={}, ttl={}, maxSize={}, tenantScoped={}",
                namespace.getName(), namespace.getTtl(), namespace.getMaxSize(),
                namespace.isTenantScoped());
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
        return Optional.ofNullable(values.getIfPresent(scoped(key)));
    }

    @Override
    public void put(String key, String value) {
        values.put(scoped(key), value);
    }

    @Override
    public boolean putIfAbsent(String key, String value) {
        String scoped = scoped(key);
        String previous = values.asMap().putIfAbsent(scoped, value);
        return previous == null;
    }

    @Override
    public void evict(String key) {
        values.invalidate(scoped(key));
    }

    @Override
    public long increment(String key) {
        AtomicLong counter = counters.get(scoped(key), k -> new AtomicLong(0L));
        return counter.incrementAndGet();
    }

    private String scoped(String key) {
        if (!namespace.isTenantScoped())
            return key;
        Long tenantId = TenantContext.getTenantId();
        String tenantPart = tenantId == null ? "_" : tenantId.toString();
        return "tenant:" + tenantPart + ":" + key;
    }
}
