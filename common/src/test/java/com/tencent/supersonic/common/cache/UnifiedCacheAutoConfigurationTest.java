package com.tencent.supersonic.common.cache;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedCacheAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UnifiedCacheAutoConfiguration.class));

    @Test
    void defaultsToCaffeineForAllBuiltinNamespaces() {
        runner.run(ctx -> {
            CacheProviderRegistry registry = ctx.getBean(CacheProviderRegistry.class);
            assertThat(registry.asMap().keySet()).contains("feishu-event-dedup", "feishu-token",
                    "feishu-general", "feishu-counter", "oauth-code", "semantic-query");
            for (CacheProvider p : registry.asMap().values()) {
                assertThat(p).isInstanceOf(CaffeineCacheProvider.class);
            }
        });
    }

    @Test
    void globalRedisTypeSwitchesAllNamespacesWhenRedisAvailable() {
        runner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
                .withPropertyValues("s2.cache.type=redis", "spring.data.redis.host=localhost",
                        "spring.data.redis.port=6379")
                .run(ctx -> {
                    CacheProviderRegistry registry = ctx.getBean(CacheProviderRegistry.class);
                    for (CacheProvider p : registry.asMap().values()) {
                        assertThat(p).isInstanceOf(RedisCacheProvider.class);
                    }
                });
    }

    @Test
    void perNamespaceOverrideTakesPrecedenceOverGlobal() {
        runner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
                .withPropertyValues("s2.cache.type=redis",
                        "s2.cache.namespaces.oauth-code.type=caffeine",
                        "spring.data.redis.host=localhost", "spring.data.redis.port=6379")
                .run(ctx -> {
                    CacheProviderRegistry registry = ctx.getBean(CacheProviderRegistry.class);
                    assertThat(registry.require("oauth-code"))
                            .isInstanceOf(CaffeineCacheProvider.class);
                    assertThat(registry.require("feishu-token"))
                            .isInstanceOf(RedisCacheProvider.class);
                });
    }

    @Test
    void perNamespaceTtlOverrideApplied() {
        runner.withPropertyValues("s2.cache.namespaces.oauth-code.ttl=45s",
                "s2.cache.namespaces.oauth-code.max-size=2000").run(ctx -> {
                    CacheProvider oauth =
                            ctx.getBean(CacheProviderRegistry.class).require("oauth-code");
                    assertThat(oauth.getNamespace().getTtl()).isEqualTo(Duration.ofSeconds(45));
                    assertThat(oauth.getNamespace().getMaxSize()).isEqualTo(2000L);
                });
    }

    @Test
    void adHocNamespaceCanBeDeclaredInYamlOnly() {
        runner.withPropertyValues("s2.cache.namespaces.my-custom.ttl=2m",
                "s2.cache.namespaces.my-custom.max-size=50",
                "s2.cache.namespaces.my-custom.tenant-scoped=true").run(ctx -> {
                    CacheProvider custom =
                            ctx.getBean(CacheProviderRegistry.class).require("my-custom");
                    assertThat(custom.getNamespace().isTenantScoped()).isTrue();
                    assertThat(custom.getNamespace().getTtl()).isEqualTo(Duration.ofMinutes(2));
                });
    }

    @Test
    void springCacheManagerExposedForAtCacheableConsumers() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(org.springframework.cache.CacheManager.class);
            org.springframework.cache.CacheManager mgr =
                    ctx.getBean(org.springframework.cache.CacheManager.class);
            assertThat(mgr.getCache("oauth-code")).isNotNull();
            assertThat(mgr.getCacheNames()).contains("oauth-code", "feishu-token",
                    "semantic-query");
        });
    }
}
