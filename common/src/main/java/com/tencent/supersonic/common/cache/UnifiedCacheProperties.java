package com.tencent.supersonic.common.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "s2.cache")
public class UnifiedCacheProperties {

    private CacheType type = CacheType.CAFFEINE;
    private Map<String, NamespaceConfig> namespaces = new LinkedHashMap<>();

    @Data
    public static class NamespaceConfig {
        private CacheType type;
        private Duration ttl;
        private long maxSize = 10_000L;
        private boolean tenantScoped;
    }
}
