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
 * Verifies that when {@code s2.cache.type=caffeine}, every registered cache namespace uses the
 * Caffeine backend, including the oauth-code namespace which is also overridden to caffeine here to
 * prevent the deprecated {@code s2.oauth.storage.type} alias from pulling it to Redis.
 */
@SpringBootTest(classes = com.tencent.supersonic.StandaloneLauncher.class)
@ActiveProfiles("h2")
@TestPropertySource(properties = {"s2.cache.type=caffeine",
                // Override deprecated alias so oauth-code also stays on Caffeine
                "S2_OAUTH_STORAGE_TYPE=caffeine", "EMAIL_HOST=localhost", "EMAIL_PORT=25",
                "EMAIL_USERNAME=test@example.com", "EMAIL_PASSWORD=test",
                "spring.mail.host=localhost", "spring.mail.port=25",
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
