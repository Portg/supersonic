package com.tencent.supersonic.feishu.server.cache;

import com.tencent.supersonic.common.cache.CacheNamespace;
import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import com.tencent.supersonic.common.cache.CaffeineCacheProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuCacheFacadeTest {

    private FeishuCacheFacade facade;

    @BeforeEach
    void setup() {
        List<CacheProvider> providers = List.of(new CaffeineCacheProvider(ns("feishu-event-dedup")),
                new CaffeineCacheProvider(ns("feishu-token")),
                new CaffeineCacheProvider(ns("feishu-general")),
                new CaffeineCacheProvider(ns("feishu-counter")));
        facade = new FeishuCacheFacade(new CacheProviderRegistry(providers));
    }

    private CacheNamespace ns(String name) {
        return CacheNamespace.builder().name(name).ttl(Duration.ofMinutes(5)).maxSize(10_000)
                .build();
    }

    @Test
    void tokenRoundtrips() {
        assertThat(facade.getToken()).isNull();
        facade.putToken("abc");
        assertThat(facade.getToken()).isEqualTo("abc");
    }

    @Test
    void duplicateEventDetectedOnSecondCall() {
        assertThat(facade.isDuplicateEvent("evt1")).isFalse();
        assertThat(facade.isDuplicateEvent("evt1")).isTrue();
        assertThat(facade.isDuplicateEvent("evt2")).isFalse();
    }

    @Test
    void duplicateEventNullIdReturnsFalse() {
        assertThat(facade.isDuplicateEvent(null)).isFalse();
    }

    @Test
    void generalGetPutRemoveRoundtrips() {
        facade.put("k1", "v1");
        assertThat(facade.get("k1")).isEqualTo("v1");
        facade.remove("k1");
        assertThat(facade.get("k1")).isNull();
    }

    @Test
    void incrementCounterMonotonic() {
        assertThat(facade.incrementCounter("c1")).isEqualTo(1L);
        assertThat(facade.incrementCounter("c1")).isEqualTo(2L);
        assertThat(facade.incrementCounter("c2")).isEqualTo(1L);
    }
}
