package com.tencent.supersonic.common.cache;

import com.tencent.supersonic.common.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class CacheProviderContractTest {

    protected abstract CacheProvider newProvider(CacheNamespace namespace);

    protected abstract void advanceTime(Duration amount);

    protected CacheNamespace defaultNs() {
        return CacheNamespace.builder().name("test-default").ttl(Duration.ofSeconds(30))
                .maxSize(100).tenantScoped(false).build();
    }

    protected CacheNamespace tenantScopedNs() {
        return CacheNamespace.builder().name("test-tenant").ttl(Duration.ofSeconds(30)).maxSize(100)
                .tenantScoped(true).build();
    }

    @BeforeEach
    void clearTenant() {
        TenantContext.clear();
    }

    @AfterEach
    void afterEach() {
        TenantContext.clear();
    }

    @Test
    void getReturnsEmptyWhenKeyAbsent() {
        assertThat(newProvider(defaultNs()).get("missing")).isEmpty();
    }

    @Test
    void putThenGetRoundtrips() {
        CacheProvider cache = newProvider(defaultNs());
        cache.put("k1", "v1");
        assertThat(cache.get("k1")).contains("v1");
    }

    @Test
    void evictRemovesEntry() {
        CacheProvider cache = newProvider(defaultNs());
        cache.put("k1", "v1");
        cache.evict("k1");
        assertThat(cache.get("k1")).isEmpty();
    }

    @Test
    void evictIdempotentOnMissingKey() {
        CacheProvider cache = newProvider(defaultNs());
        cache.evict("never-written");
        assertThat(cache.get("never-written")).isEmpty();
    }

    @Test
    void putIfAbsentReturnsTrueWhenKeyAbsent() {
        CacheProvider cache = newProvider(defaultNs());
        assertThat(cache.putIfAbsent("k1", "v1")).isTrue();
        assertThat(cache.get("k1")).contains("v1");
    }

    @Test
    void putIfAbsentReturnsFalseAndDoesNotOverwrite() {
        CacheProvider cache = newProvider(defaultNs());
        cache.put("k1", "existing");
        assertThat(cache.putIfAbsent("k1", "replacement")).isFalse();
        assertThat(cache.get("k1")).contains("existing");
    }

    @Test
    void putOverwritesExistingValue() {
        CacheProvider cache = newProvider(defaultNs());
        cache.put("k1", "v1");
        cache.put("k1", "v2");
        assertThat(cache.get("k1")).contains("v2");
    }

    @Test
    void incrementStartsAtOneForNewKey() {
        assertThat(newProvider(defaultNs()).increment("counter")).isEqualTo(1L);
    }

    @Test
    void incrementIsMonotonic() {
        CacheProvider cache = newProvider(defaultNs());
        assertThat(cache.increment("counter")).isEqualTo(1L);
        assertThat(cache.increment("counter")).isEqualTo(2L);
        assertThat(cache.increment("counter")).isEqualTo(3L);
    }

    @Test
    void incrementCounterResetsAfterTtlWindow() {
        CacheNamespace ns = CacheNamespace.builder().name("rate-limit").ttl(Duration.ofSeconds(5))
                .maxSize(100).build();
        CacheProvider cache = newProvider(ns);
        assertThat(cache.increment("hits")).isEqualTo(1L);
        assertThat(cache.increment("hits")).isEqualTo(2L);
        advanceTime(Duration.ofSeconds(6));
        assertThat(cache.increment("hits")).isEqualTo(1L); // fixed-window reset
    }

    @Test
    void entryExpiresAfterTtl() {
        CacheNamespace ns = CacheNamespace.builder().name("short-lived").ttl(Duration.ofSeconds(10))
                .maxSize(10).build();
        CacheProvider cache = newProvider(ns);
        cache.put("k1", "v1");
        assertThat(cache.get("k1")).contains("v1");
        advanceTime(Duration.ofSeconds(11));
        assertThat(cache.get("k1")).isEmpty();
    }

    @Test
    void tenantScopedKeysAreIsolatedBetweenTenants() {
        CacheProvider cache = newProvider(tenantScopedNs());
        TenantContext.setTenantId(1L);
        cache.put("shared", "tenant1-value");
        TenantContext.setTenantId(2L);
        assertThat(cache.get("shared")).isEmpty();
        cache.put("shared", "tenant2-value");
        TenantContext.setTenantId(1L);
        assertThat(cache.get("shared")).contains("tenant1-value");
        TenantContext.setTenantId(2L);
        assertThat(cache.get("shared")).contains("tenant2-value");
    }

    @Test
    void tenantScopedKeysUseUnderscoreWhenContextUnset() {
        CacheProvider cache = newProvider(tenantScopedNs());
        cache.put("shared", "no-tenant");
        TenantContext.setTenantId(1L);
        assertThat(cache.get("shared")).isEmpty();
        TenantContext.clear();
        assertThat(cache.get("shared")).contains("no-tenant");
    }

    @Test
    void nonTenantScopedKeysIgnoreTenantContext() {
        CacheProvider cache = newProvider(defaultNs());
        TenantContext.setTenantId(1L);
        cache.put("global", "v1");
        TenantContext.setTenantId(2L);
        assertThat(cache.get("global")).contains("v1");
    }

    @Test
    void getNamespaceExposesSourceDescriptor() {
        CacheNamespace ns = defaultNs();
        CacheProvider cache = newProvider(ns);
        assertThat(cache.getName()).isEqualTo(ns.getName());
        assertThat(cache.getNamespace()).isEqualTo(ns);
    }

    @Test
    void optionalSemanticsNeverThrowOnMissing() {
        Optional<String> result = newProvider(defaultNs()).get("never-put");
        assertThat(result).isNotNull().isEmpty();
    }
}
