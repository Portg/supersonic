package com.tencent.supersonic.config;

import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import com.tencent.supersonic.common.cache.CaffeineCacheProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that when all cache-related env vars are forced to "caffeine", every registered
 * namespace uses the Caffeine backend — guarding against per-namespace defaults accidentally
 * pulling in Redis.
 */
@SpringBootTest(classes = com.tencent.supersonic.StandaloneLauncher.class)
@ActiveProfiles("h2")
@TestPropertySource(properties = {
                // Global cache type
                "s2.cache.type=caffeine",
                // Override per-namespace env vars so none pull in Redis
                "S2_CACHE_TYPE=caffeine", "S2_OAUTH_STORAGE_TYPE=caffeine",
                "FEISHU_CACHE_TYPE=caffeine",
                // Stub mail placeholders so EmailDeliveryChannel @ConditionalOnProperty evaluates
                // safely
                "EMAIL_HOST=localhost", "EMAIL_PORT=25", "EMAIL_USERNAME=test@example.com",
                "EMAIL_PASSWORD=test", "spring.mail.host=localhost", "spring.mail.port=25",
                "spring.mail.username=test@example.com", "spring.mail.password=test"})
class UnifiedCacheDefaultSmokeTest {

    @Autowired
    CacheProviderRegistry registry;

    @Test
    void defaultConfigurationIsAllCaffeine() {
        for (CacheProvider p : registry.asMap().values()) {
            assertThat(p).as("namespace %s should be Caffeine-backed", p.getName())
                    .isInstanceOf(CaffeineCacheProvider.class);
        }
    }
}
