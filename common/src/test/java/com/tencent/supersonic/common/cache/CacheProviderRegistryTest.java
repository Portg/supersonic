package com.tencent.supersonic.common.cache;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheProviderRegistryTest {

    @Test
    void requireReturnsRegisteredProvider() {
        CacheProvider fake = stub("foo");
        CacheProviderRegistry registry = new CacheProviderRegistry(List.of(fake));
        assertThat(registry.require("foo")).isSameAs(fake);
    }

    @Test
    void requireThrowsWhenMissing() {
        CacheProviderRegistry registry = new CacheProviderRegistry(List.of());
        assertThatThrownBy(() -> registry.require("missing"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("missing")
                .hasMessageContaining("declared in s2.cache.namespaces");
    }

    @Test
    void findReturnsEmptyWhenMissing() {
        CacheProviderRegistry registry = new CacheProviderRegistry(List.of());
        assertThat(registry.find("missing")).isEmpty();
    }

    @Test
    void duplicateNamespaceRejected() {
        CacheProvider a = stub("dupe");
        CacheProvider b = stub("dupe");
        assertThatThrownBy(() -> new CacheProviderRegistry(List.of(a, b)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate cache namespace").hasMessageContaining("dupe");
    }

    private static CacheProvider stub(String name) {
        CacheNamespace ns =
                CacheNamespace.builder().name(name).ttl(Duration.ofMinutes(1)).maxSize(10).build();
        return new CacheProvider() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public CacheNamespace getNamespace() {
                return ns;
            }

            @Override
            public Optional<String> get(String k) {
                return Optional.empty();
            }

            @Override
            public void put(String k, String v) {}

            @Override
            public boolean putIfAbsent(String k, String v) {
                return true;
            }

            @Override
            public void evict(String k) {}

            @Override
            public long increment(String k) {
                return 0;
            }
        };
    }
}
