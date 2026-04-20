package com.tencent.supersonic.common.quota;

import com.tencent.supersonic.common.metrics.AbstractMeterBinder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenantQuotaMeterBinder extends AbstractMeterBinder {

    private final TenantQuotaService service;
    private final Map<Long, Boolean> registered = new ConcurrentHashMap<>();
    private MeterRegistry registryRef;

    public TenantQuotaMeterBinder(TenantQuotaService service) {
        this.service = service;
    }

    @Override
    protected void doBindTo(MeterRegistry registry) {
        this.registryRef = registry;
        service.availablePermits().keySet().forEach(this::registerIfAbsent);
        Gauge.builder("s2_tenant_quota_known_tenants", service, s -> s.availablePermits().size())
                .description("Number of tenants with an active quota semaphore").register(registry);
    }

    private void registerIfAbsent(Long tenantId) {
        if (registered.putIfAbsent(tenantId, Boolean.TRUE) != null || registryRef == null) {
            return;
        }
        String tid = String.valueOf(tenantId);
        Gauge.builder("s2_tenant_jdbc_permits_available", service,
                s -> s.availablePermits().getOrDefault(tenantId, 0)).tag("tenantId", tid)
                .description("Available JDBC permits for tenant").register(registryRef);
        Gauge.builder("s2_tenant_jdbc_permits_waiting", service,
                s -> s.waitingThreads().getOrDefault(tenantId, 0)).tag("tenantId", tid)
                .description("Threads waiting for a JDBC permit for tenant").register(registryRef);
    }
}
