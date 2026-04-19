package com.tencent.supersonic.common.cache;

import com.github.benmanes.caffeine.cache.Ticker;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

class CaffeineCacheProviderTest extends CacheProviderContractTest {

    private final AtomicLong fakeNanos = new AtomicLong(0);
    private final Ticker ticker = fakeNanos::get;

    @Override
    protected CacheProvider newProvider(CacheNamespace namespace) {
        return new CaffeineCacheProvider(namespace, ticker);
    }

    @Override
    protected void advanceTime(Duration amount) {
        fakeNanos.addAndGet(amount.toNanos());
    }
}
