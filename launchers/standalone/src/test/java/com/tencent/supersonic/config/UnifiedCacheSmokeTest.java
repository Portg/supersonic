package com.tencent.supersonic.config;

import com.tencent.supersonic.auth.authentication.oauth.model.OAuthCodeExchange;
import com.tencent.supersonic.auth.authentication.oauth.service.OAuthCodeExchangeService;
import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import com.tencent.supersonic.common.cache.InMemoryStringRedisTemplate;
import com.tencent.supersonic.common.cache.RedisCacheProvider;
import com.tencent.supersonic.feishu.server.cache.FeishuCacheFacade;
import com.tencent.supersonic.headless.core.cache.CacheManager;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.tencent.supersonic.StandaloneLauncher.class)
@ActiveProfiles("h2")
@TestPropertySource(properties = {"s2.cache.type=redis", "spring.data.redis.host=localhost",
                "spring.data.redis.port=16379", "EMAIL_HOST=localhost", "EMAIL_PORT=25",
                "EMAIL_USERNAME=test@example.com", "EMAIL_PASSWORD=test",
                "spring.mail.host=localhost", "spring.mail.port=25",
                "spring.mail.username=test@example.com", "spring.mail.password=test"})
@Import(UnifiedCacheSmokeTest.FakeRedisConfig.class)
@DirtiesContext
class UnifiedCacheSmokeTest {

    @TestConfiguration
    static class FakeRedisConfig {
        @Bean
        @Primary
        public StringRedisTemplate stringRedisTemplate() {
            return new InMemoryStringRedisTemplate();
        }
    }

    // Prevent Redisson from connecting to a real Redis during testing
    @MockBean
    RedissonClient redissonClient;

    @Autowired
    CacheProviderRegistry registry;

    @Autowired
    FeishuCacheFacade feishu;

    @Autowired
    OAuthCodeExchangeService oauth;

    @Autowired
    CacheManager headless;

    @Test
    void allNamespacesUseRedisBackend() {
        for (CacheProvider p : registry.asMap().values()) {
            assertThat(p).as("namespace %s should be Redis-backed", p.getName())
                    .isInstanceOf(RedisCacheProvider.class);
        }
    }

    @Test
    void feishuSubsystemRoundtripsViaRedis() {
        feishu.putToken("tok-xyz");
        assertThat(feishu.getToken()).isEqualTo("tok-xyz");
        feishu.put("conv:42", "ctx");
        assertThat(feishu.get("conv:42")).isEqualTo("ctx");
        assertThat(feishu.incrementCounter("rate:abc")).isEqualTo(1L);
        assertThat(feishu.incrementCounter("rate:abc")).isEqualTo(2L);
    }

    @Test
    void oauthSubsystemRoundtripsViaRedis() {
        String code = oauth.createExchangeCode("at", "rt", "sid", 99L);
        OAuthCodeExchange x = oauth.exchangeCodeForTokens(code);
        assertThat(x).isNotNull();
        assertThat(x.getUserId()).isEqualTo(99L);
        // second use of same code must return null (one-time use)
        assertThat(oauth.exchangeCodeForTokens(code)).isNull();
    }

    @Test
    void headlessSubsystemRoundtripsViaRedis() {
        String key = headless.generateCacheKey("1,2,3", "md5abc");
        headless.put(key, "result-payload");
        assertThat(headless.get(key)).isEqualTo("result-payload");
        headless.removeCache(key);
        assertThat(headless.get(key)).isNull();
    }
}
