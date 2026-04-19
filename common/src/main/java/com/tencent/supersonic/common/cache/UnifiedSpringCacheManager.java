package com.tencent.supersonic.common.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Collections;

public class UnifiedSpringCacheManager implements CacheManager {

    private final CacheProviderRegistry registry;

    public UnifiedSpringCacheManager(CacheProviderRegistry registry) {
        this.registry = registry;
    }

    @Override
    @Nullable
    public Cache getCache(@NonNull String name) {
        return registry.find(name).map(ProviderCache::new).orElse(null);
    }

    @Override
    @NonNull
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(registry.asMap().keySet());
    }

    private static final class ProviderCache implements Cache {
        private final CacheProvider provider;

        ProviderCache(CacheProvider provider) {
            this.provider = provider;
        }

        @Override
        @NonNull
        public String getName() {
            return provider.getName();
        }

        @Override
        @NonNull
        public Object getNativeCache() {
            return provider;
        }

        @Override
        @Nullable
        public ValueWrapper get(@NonNull Object key) {
            return provider.get(key.toString()).map(v -> (ValueWrapper) new SimpleValueWrapper(v))
                    .orElse(null);
        }

        @Override
        @Nullable
        public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
            Object value = provider.get(key.toString()).orElse(null);
            if (value == null)
                return null;
            if (type != null && !type.isInstance(value)) {
                throw new IllegalStateException("Cached value for key '" + key
                        + "' is not of required type " + type.getName());
            }
            @SuppressWarnings("unchecked")
            T typed = (T) value;
            return typed;
        }

        @Override
        @Nullable
        public <T> T get(@NonNull Object key,
                @NonNull java.util.concurrent.Callable<T> valueLoader) {
            throw new UnsupportedOperationException(
                    "Synchronous load-through not supported — use CacheProvider API directly.");
        }

        @Override
        public void put(@NonNull Object key, @Nullable Object value) {
            if (value == null)
                provider.evict(key.toString());
            else
                provider.put(key.toString(), value.toString());
        }

        @Override
        public void evict(@NonNull Object key) {
            provider.evict(key.toString());
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException(
                    "clear() not supported — namespaces auto-expire via TTL.");
        }
    }
}
