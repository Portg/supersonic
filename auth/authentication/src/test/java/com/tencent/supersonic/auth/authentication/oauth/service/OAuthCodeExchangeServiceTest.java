package com.tencent.supersonic.auth.authentication.oauth.service;

import com.tencent.supersonic.auth.authentication.oauth.model.OAuthCodeExchange;
import com.tencent.supersonic.common.cache.CacheNamespace;
import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import com.tencent.supersonic.common.cache.CaffeineCacheProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthCodeExchangeServiceTest {

    private OAuthCodeExchangeService service;

    @BeforeEach
    void setup() {
        CacheNamespace ns = CacheNamespace.builder().name("oauth-code").ttl(Duration.ofSeconds(60))
                .maxSize(1000).build();
        CacheProvider provider = new CaffeineCacheProvider(ns);
        CacheProviderRegistry registry =
                new CacheProviderRegistry(List.<CacheProvider>of(provider));
        service = new OAuthCodeExchangeService(registry);
    }

    @Test
    void createExchangeCodeReturnsNonNull() {
        String code = service.createExchangeCode("access", "refresh", "sid", 42L);
        assertThat(code).isNotBlank();
    }

    @Test
    void exchangeCodeForTokensRetrievesExchange() {
        String code = service.createExchangeCode("access", "refresh", "sid", 42L);
        OAuthCodeExchange x = service.exchangeCodeForTokens(code);
        assertThat(x).isNotNull();
        assertThat(x.getAccessToken()).isEqualTo("access");
        assertThat(x.getUserId()).isEqualTo(42L);
    }

    @Test
    void exchangeCodeIsOneTimeUse() {
        String code = service.createExchangeCode("access", "refresh", "sid", 42L);
        assertThat(service.exchangeCodeForTokens(code)).isNotNull();
        assertThat(service.exchangeCodeForTokens(code)).isNull();
    }

    @Test
    void exchangeCodeNullReturnsNull() {
        assertThat(service.exchangeCodeForTokens(null)).isNull();
        assertThat(service.exchangeCodeForTokens("")).isNull();
    }

    @Test
    void unknownCodeReturnsNull() {
        assertThat(service.exchangeCodeForTokens("does-not-exist")).isNull();
    }
}
