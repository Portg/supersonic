package com.tencent.supersonic.common.cache;

import java.time.Duration;

class RedisCacheProviderTest extends CacheProviderContractTest {

    private final InMemoryStringRedisTemplate fakeRedis = new InMemoryStringRedisTemplate();

    @Override
    protected CacheProvider newProvider(CacheNamespace namespace) {
        return new RedisCacheProvider(namespace, fakeRedis);
    }

    @Override
    protected void advanceTime(Duration amount) {
        fakeRedis.advanceTime(amount);
    }
}
