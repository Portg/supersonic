package com.tencent.supersonic.common.quota;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    private final Set<Long> bypassedTenants = ConcurrentHashMap.newKeySet();

    public InMemoryTenantQuotaService(TenantQuotaConfig config,
            Function<Long, TenantQuotaOverride> overrideLoader) {
        this.config = config;
        this.overrideLoader = overrideLoader;
    }

    @Override
    public TenantPermit acquireJdbc(Long tenantId, long timeoutMs) {
        if (!config.isEnabled()) {
            return TenantPermit.noop();
        }
        Long effectiveTenantId = resolveTenantId(tenantId);
        if (effectiveTenantId == null || bypassedTenants.contains(effectiveTenantId)) {
            return TenantPermit.noop();
        }
        Semaphore sem = semaphoreFor(effectiveTenantId);
        if (sem == null) {
            return TenantPermit.noop();
        }
        long effectiveTimeout = timeoutMs > 0 ? timeoutMs
                : timeouts.getOrDefault(effectiveTenantId,
                        positive(config.getDefaultQuota().getAcquireTimeoutMs()));
        try {
            if (!sem.tryAcquire(effectiveTimeout, TimeUnit.MILLISECONDS)) {
                int retryAfter = (int) Math.max(1, effectiveTimeout / 1000);
                log.warn("[TenantQuota] 429 tenantId={} available={} waiting={}", effectiveTenantId,
                        sem.availablePermits(), sem.getQueueLength());
                throw new TooManyRequestsException(effectiveTenantId, retryAfter);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TooManyRequestsException(effectiveTenantId, 1);
        }
        return new TenantPermit(sem, effectiveTenantId);
    }

    @Override
    public void refresh(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        TenantQuotaOverride o = safeLoadOverride(tenantId);
        if (o != null && !o.isEnabled()) {
            bypassedTenants.add(tenantId);
            semaphores.remove(tenantId);
            sizes.remove(tenantId);
            timeouts.remove(tenantId);
            log.info("[TenantQuota] bypassed tenantId={}", tenantId);
            return;
        }
        bypassedTenants.remove(tenantId);
        int size = effectiveJdbcConcurrent(o);
        int timeout = effectiveAcquireTimeoutMs(o);
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
        if (o != null && !o.isEnabled()) {
            bypassedTenants.add(tenantId);
            sizes.remove(tenantId);
            timeouts.remove(tenantId);
            return null;
        }
        bypassedTenants.remove(tenantId);
        int size = effectiveJdbcConcurrent(o);
        int timeout = effectiveAcquireTimeoutMs(o);
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

    private Long resolveTenantId(Long tenantId) {
        return tenantId != null ? tenantId : config.getFallbackTenantId();
    }

    private int effectiveJdbcConcurrent(TenantQuotaOverride override) {
        int configured = override != null ? override.getJdbcConcurrent()
                : config.getDefaultQuota().getJdbcConcurrent();
        return positive(configured);
    }

    private int effectiveAcquireTimeoutMs(TenantQuotaOverride override) {
        int configured = override != null && override.getAcquireTimeoutMs() > 0
                ? override.getAcquireTimeoutMs()
                : config.getDefaultQuota().getAcquireTimeoutMs();
        return positive(configured);
    }

    private int positive(int value) {
        return Math.max(1, value);
    }
}
