package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.quota.InMemoryTenantQuotaService;
import com.tencent.supersonic.common.quota.TenantPermit;
import com.tencent.supersonic.common.quota.TenantQuotaConfig;
import com.tencent.supersonic.common.quota.TenantQuotaService;
import com.tencent.supersonic.common.quota.TooManyRequestsException;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SqlUtilsQuotaIntegrationTest {

    private TenantQuotaService quotaService;
    private AtomicInteger concurrent;
    private AtomicInteger peakConcurrent;

    @BeforeEach
    void setUp() {
        TenantQuotaConfig config = new TenantQuotaConfig();
        config.setEnabled(true);
        config.getDefaultQuota().setJdbcConcurrent(2);
        config.getDefaultQuota().setAcquireTimeoutMs(100);
        quotaService = new InMemoryTenantQuotaService(config, tid -> null);
        concurrent = new AtomicInteger();
        peakConcurrent = new AtomicInteger();
    }

    @Test
    void initCarriesTenantQuotaDependenciesIntoSqlUtilsInstance() throws Exception {
        SqlUtils root = new SqlUtils();
        setField(root, "tenantQuotaService", quotaService);
        setField(root, "quotaAcquireTimeoutMs", 123L);

        DatabaseResp database = DatabaseResp.builder().id(1L).name("h2").type("h2")
                .url("jdbc:h2:mem:test").username("sa").password("").build();

        SqlUtils initialized = root.init(database);

        assertSame(quotaService, getField(initialized, "tenantQuotaService"));
        assertEquals(123L, getField(initialized, "quotaAcquireTimeoutMs"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void simulatedQuery(Long tenantId) throws InterruptedException {
        try (TenantPermit p = quotaService.acquireJdbc(tenantId, 100)) {
            int now = concurrent.incrementAndGet();
            peakConcurrent.updateAndGet(prev -> Math.max(prev, now));
            Thread.sleep(300);
            concurrent.decrementAndGet();
        }
    }

    @Test
    void fiveParallelQueriesQuotaTwoExactlyTwoConcurrentThreeRejected() throws Exception {
        int threads = 5;
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    simulatedQuery(77L);
                    accepted.incrementAndGet();
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

        assertEquals(2, accepted.get(), "exactly 2 concurrent queries accepted");
        assertEquals(3, rejected.get(), "exactly 3 queries rejected with 429");
        assertEquals(2, peakConcurrent.get(), "peak concurrency equals quota");
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = SqlUtils.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getField(Object target, String name) throws Exception {
        Field field = SqlUtils.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
