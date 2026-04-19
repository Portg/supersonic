package com.tencent.supersonic.common.quota;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
public class InMemoryTenantQuotaService implements TenantQuotaService {

    private final TenantQuotaConfig config;
    private final Function<Long, TenantQuotaOverride> overrideLoader;
    private final ConcurrentHashMap<Long, Semaphore> semaphores = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> sizes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> timeouts = new ConcurrentHashMap<>();

    public InMemoryTenantQuotaService(TenantQuotaConfig config,
            Function<Long, TenantQuotaOverride> overrideLoader) {
        this.config = config;
        this.overrideLoader = overrideLoader;
    }

    @Override
    public TenantPermit acquireJdbc(Long tenantId, long timeoutMs) {
        if (!config.isEnabled() || tenantId == null) {
            return TenantPermit.noop();
        }
        Semaphore sem = semaphoreFor(tenantId);
        long effectiveTimeout = timeoutMs > 0 ? timeoutMs
                : timeouts.getOrDefault(tenantId, config.getDefaultQuota().getAcquireTimeoutMs());
        try {
            if (!sem.tryAcquire(effectiveTimeout, TimeUnit.MILLISECONDS)) {
                int retryAfter = (int) Math.max(1, effectiveTimeout / 1000);
                log.warn("[TenantQuota] 429 tenantId={} available={} waiting={}", tenantId,
                        sem.availablePermits(), sem.getQueueLength());
                throw new TooManyRequestsException(tenantId, retryAfter);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TooManyRequestsException(tenantId, 1);
        }
        return new TenantPermit(sem, tenantId);
    }

    @Override
    public void refresh(Long tenantId) {
        TenantQuotaOverride o = safeLoadOverride(tenantId);
        int size = o != null && o.isEnabled() ? o.getJdbcConcurrent()
                : config.getDefaultQuota().getJdbcConcurrent();
        int timeout = o != null ? o.getAcquireTimeoutMs()
                : config.getDefaultQuota().getAcquireTimeoutMs();
        semaphores.put(tenantId, new Semaphore(size, true));
        sizes.put(tenantId, size);
        timeouts.put(tenantId, timeout);
        log.info("[TenantQuota] refreshed tenantId={} jdbcConcurrent={} acquireTimeoutMs={}",
                tenantId, size, timeout);
    }

    @Override
    public Map<Long, Integer> availablePermits() {
        Map<Long, Integer> out = new HashMap<>();
        semaphores.forEach((k, v) -> out.put(k, v.availablePermits()));
        return out;
    }

    @Override
    public Map<Long, Integer> waitingThreads() {
        Map<Long, Integer> out = new HashMap<>();
        semaphores.forEach((k, v) -> out.put(k, v.getQueueLength()));
        return out;
    }

    private Semaphore semaphoreFor(Long tenantId) {
        return semaphores.computeIfAbsent(tenantId, this::buildSemaphore);
    }

    private Semaphore buildSemaphore(Long tenantId) {
        TenantQuotaOverride o = safeLoadOverride(tenantId);
        int size = o != null && o.isEnabled() ? o.getJdbcConcurrent()
                : config.getDefaultQuota().getJdbcConcurrent();
        int timeout = o != null ? o.getAcquireTimeoutMs()
                : config.getDefaultQuota().getAcquireTimeoutMs();
        sizes.put(tenantId, size);
        timeouts.put(tenantId, timeout);
        return new Semaphore(size, true);
    }

    private TenantQuotaOverride safeLoadOverride(Long tenantId) {
        try {
            return overrideLoader != null ? overrideLoader.apply(tenantId) : null;
        } catch (Exception e) {
            log.warn("[TenantQuota] override loader failed tenantId={}: {}", tenantId,
                    e.getMessage());
            return null;
        }
    }
}
