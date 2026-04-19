package com.tencent.supersonic.common.quota;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryTenantQuotaServiceTest {

    private TenantQuotaConfig configWithDefault(int jdbc, int timeoutMs) {
        TenantQuotaConfig c = new TenantQuotaConfig();
        c.setEnabled(true);
        c.getDefaultQuota().setJdbcConcurrent(jdbc);
        c.getDefaultQuota().setAcquireTimeoutMs(timeoutMs);
        return c;
    }

    @Test
    void acquireAndReleaseReturnsPermitToPool() {
        InMemoryTenantQuotaService svc =
                new InMemoryTenantQuotaService(configWithDefault(1, 100), tid -> null);
        try (TenantPermit p = svc.acquireJdbc(42L, 100)) {
            assertNotNull(p);
            assertEquals(0, svc.availablePermits().get(42L));
        }
        assertEquals(1, svc.availablePermits().get(42L));
    }

    @Test
    void nullTenantReturnsNoopPermit() {
        InMemoryTenantQuotaService svc =
                new InMemoryTenantQuotaService(configWithDefault(1, 100), tid -> null);
        try (TenantPermit p = svc.acquireJdbc(null, 100)) {
            assertNotNull(p);
        }
    }

    @Test
    void timeoutThrowsTooManyRequests() throws Exception {
        InMemoryTenantQuotaService svc =
                new InMemoryTenantQuotaService(configWithDefault(1, 50), tid -> null);
        TenantPermit hold = svc.acquireJdbc(7L, 50);
        try {
            assertThrows(TooManyRequestsException.class, () -> svc.acquireJdbc(7L, 50));
        } finally {
            hold.close();
        }
    }

    @Test
    void tenThreadsQuotaThreeExactlyThreeAcquireImmediately() throws Exception {
        InMemoryTenantQuotaService svc =
                new InMemoryTenantQuotaService(configWithDefault(3, 50), tid -> null);
        int threads = 10;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger acquired = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    TenantPermit p = svc.acquireJdbc(99L, 50);
                    acquired.incrementAndGet();
                    Thread.sleep(200);
                    p.close();
                } catch (TooManyRequestsException e) {
                    rejected.incrementAndGet();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await(5, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertEquals(3, acquired.get(), "exactly 3 threads must acquire");
        assertEquals(7, rejected.get(), "exactly 7 threads must be rejected with 429");
    }

    @Test
    void refreshReplacesSemaphoreWithNewSize() {
        AtomicInteger size = new AtomicInteger(1);
        InMemoryTenantQuotaService svc =
                new InMemoryTenantQuotaService(configWithDefault(1, 100), tid -> {
                    TenantQuotaOverride o = new TenantQuotaOverride();
                    o.setJdbcConcurrent(size.get());
                    o.setAcquireTimeoutMs(100);
                    o.setEnabled(true);
                    return o;
                });
        assertEquals(1,
                svc.availablePermits().get(5L) == null ? 1 : svc.availablePermits().get(5L));
        size.set(5);
        svc.refresh(5L);
        try (TenantPermit p = svc.acquireJdbc(5L, 50)) {
            assertEquals(4, svc.availablePermits().get(5L));
        }
    }
}
