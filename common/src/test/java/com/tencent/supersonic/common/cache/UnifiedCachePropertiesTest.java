package com.tencent.supersonic.common.cache;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedCachePropertiesTest {

    @Test
    void bindsGlobalDefaultAndNamespaceOverrides() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("s2.cache.type", "redis");
        env.setProperty("s2.cache.namespaces.oauth-code.type", "caffeine");
        env.setProperty("s2.cache.namespaces.oauth-code.ttl", "45s");
        env.setProperty("s2.cache.namespaces.oauth-code.max-size", "2000");
        env.setProperty("s2.cache.namespaces.oauth-code.tenant-scoped", "false");
        env.setProperty("s2.cache.namespaces.feishu-token.ttl", "110m");

        UnifiedCacheProperties props =
                Binder.get(env).bind("s2.cache", UnifiedCacheProperties.class).get();

        assertThat(props.getType()).isEqualTo(CacheType.REDIS);
        Map<String, UnifiedCacheProperties.NamespaceConfig> nss = props.getNamespaces();
        assertThat(nss).containsKeys("oauth-code", "feishu-token");

        UnifiedCacheProperties.NamespaceConfig oauth = nss.get("oauth-code");
        assertThat(oauth.getType()).isEqualTo(CacheType.CAFFEINE);
        assertThat(oauth.getTtl()).isEqualTo(Duration.ofSeconds(45));
        assertThat(oauth.getMaxSize()).isEqualTo(2000L);
        assertThat(oauth.getTenantScoped()).isEqualTo(Boolean.FALSE);

        UnifiedCacheProperties.NamespaceConfig feishu = nss.get("feishu-token");
        assertThat(feishu.getType()).isNull();
        assertThat(feishu.getTtl()).isEqualTo(Duration.ofMinutes(110));
    }

    @Test
    void globalTypeDefaultsToCaffeineWhenAbsent() {
        MockEnvironment env = new MockEnvironment();
        ConfigurationPropertySources.attach(env);
        UnifiedCacheProperties props =
                Binder.get(env).bindOrCreate("s2.cache", UnifiedCacheProperties.class);
        assertThat(props.getType()).isEqualTo(CacheType.CAFFEINE);
        assertThat(props.getNamespaces()).isEmpty();
    }
}
