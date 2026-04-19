package com.tencent.supersonic.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UnifiedCacheProperties.class)
public class UnifiedCacheAutoConfiguration {

    private static Map<String, CacheNamespace> defaults() {
        Map<String, CacheNamespace> m = new LinkedHashMap<>();
        m.put("feishu-event-dedup", CacheNamespace.builder().name("feishu-event-dedup")
                .ttl(Duration.ofMinutes(5)).maxSize(10_000).tenantScoped(false).build());
        m.put("feishu-token", CacheNamespace.builder().name("feishu-token")
                .ttl(Duration.ofMinutes(110)).maxSize(16).tenantScoped(false).build());
        m.put("feishu-general", CacheNamespace.builder().name("feishu-general")
                .ttl(Duration.ofMinutes(30)).maxSize(5_000).tenantScoped(false).build());
        m.put("feishu-counter", CacheNamespace.builder().name("feishu-counter")
                .ttl(Duration.ofSeconds(60)).maxSize(10_000).tenantScoped(false).build());
        m.put("oauth-code", CacheNamespace.builder().name("oauth-code").ttl(Duration.ofSeconds(60))
                .maxSize(10_000).tenantScoped(false).build());
        m.put("semantic-query", CacheNamespace.builder().name("semantic-query")
                .ttl(Duration.ofMinutes(10)).maxSize(5_000).tenantScoped(true).build());
        return m;
    }

    static Map<String, CacheNamespace> resolveNamespaces(UnifiedCacheProperties properties) {
        Map<String, CacheNamespace> out = new LinkedHashMap<>(defaults());
        for (Map.Entry<String, UnifiedCacheProperties.NamespaceConfig> e : properties
                .getNamespaces().entrySet()) {
            String name = e.getKey();
            UnifiedCacheProperties.NamespaceConfig cfg = e.getValue();
            CacheNamespace base = out.get(name);
            CacheNamespace.CacheNamespaceBuilder b =
                    (base == null) ? CacheNamespace.builder().name(name).maxSize(10_000L)
                            : CacheNamespace.builder().name(base.getName())
                                    .typeOverride(base.getTypeOverride()).ttl(base.getTtl())
                                    .maxSize(base.getMaxSize()).tenantScoped(base.isTenantScoped());
            if (cfg.getType() != null)
                b.typeOverride(cfg.getType());
            if (cfg.getTtl() != null)
                b.ttl(cfg.getTtl());
            if (cfg.getMaxSize() > 0L)
                b.maxSize(cfg.getMaxSize());
            if (cfg.getTenantScoped() != null)
                b.tenantScoped(cfg.getTenantScoped());
            CacheNamespace ns = b.build();
            if (ns.getTtl() == null) {
                throw new IllegalStateException("Cache namespace '" + name
                        + "' must declare a TTL (s2.cache.namespaces." + name + ".ttl).");
            }
            out.put(name, ns);
        }
        return out;
    }

    // Caffeine-only fallback: used when spring-data-redis is absent from the classpath.
    // Launchers that never include spring-data-redis (chat, headless) use this path.
    @Bean
    @ConditionalOnMissingBean(CacheProviderRegistry.class)
    @ConditionalOnMissingClass("org.springframework.data.redis.core.StringRedisTemplate")
    public CacheProviderRegistry caffeineOnlyRegistry(UnifiedCacheProperties properties) {
        Map<String, CacheNamespace> resolved = resolveNamespaces(properties);
        List<CacheProvider> providers = new ArrayList<>();
        for (CacheNamespace ns : resolved.values()) {
            CacheType effective =
                    ns.getTypeOverride() != null ? ns.getTypeOverride() : properties.getType();
            if (effective == CacheType.REDIS) {
                log.warn(
                        "Cache namespace '{}' requested REDIS but spring-data-redis is not on the classpath — falling back to Caffeine.",
                        ns.getName());
            }
            providers.add(new CaffeineCacheProvider(ns));
        }
        log.info("Unified cache (no-Redis mode): namespaces={}", resolved.keySet());
        return new CacheProviderRegistry(providers);
    }

    @Bean
    @ConditionalOnMissingBean(org.springframework.cache.CacheManager.class)
    public org.springframework.cache.CacheManager unifiedSpringCacheManager(
            CacheProviderRegistry registry) {
        return new UnifiedSpringCacheManager(registry);
    }

    // Inner class loaded only when spring-data-redis is on the classpath.
    // Isolates StringRedisTemplate references so launchers without Redis can still start.
    @Slf4j
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    static class WithRedis {

        @Bean
        @ConditionalOnMissingBean
        public CacheProviderRegistry cacheProviderRegistry(UnifiedCacheProperties properties,
                ObjectProvider<StringRedisTemplate> redisProvider) {
            StringRedisTemplate redis = redisProvider.getIfAvailable();
            Map<String, CacheNamespace> resolved = resolveNamespaces(properties);
            List<CacheProvider> providers = new ArrayList<>();
            for (CacheNamespace ns : resolved.values()) {
                CacheType effective =
                        ns.getTypeOverride() != null ? ns.getTypeOverride() : properties.getType();
                providers.add(buildProvider(ns, effective, redis));
            }
            log.info("Unified cache initialized: globalType={}, namespaces={}, redisAvailable={}",
                    properties.getType(), resolved.keySet(), redis != null);
            return new CacheProviderRegistry(providers);
        }

        private CacheProvider buildProvider(CacheNamespace ns, CacheType type,
                StringRedisTemplate redis) {
            if (type == CacheType.REDIS) {
                if (redis == null) {
                    log.warn(
                            "Cache namespace '{}' requested REDIS but no StringRedisTemplate available — falling back to Caffeine.",
                            ns.getName());
                    return new CaffeineCacheProvider(ns);
                }
                return new RedisCacheProvider(ns, redis);
            }
            return new CaffeineCacheProvider(ns);
        }
    }
}
