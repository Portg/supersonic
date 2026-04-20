package com.tencent.supersonic.common.quota;

import com.tencent.supersonic.common.metrics.AbstractMeterBinder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenantQuotaMeterBinder extends AbstractMeterBinder {

    private final TenantQuotaService service;
    private final Map<Long, Boolean> registered = new ConcurrentHashMap<>();

    public TenantQuotaMeterBinder(TenantQuotaService service) {
        super(Tags.of("module", "tenant_quota"));
        this.service = service;
    }

    @Override
    protected void doBindTo(MeterRegistry registry) {
        refreshTenantGauges();
        Gauge.builder("s2_tenant_quota_known_tenants", service, s -> {
            refreshTenantGauges();
            return s.availablePermits().size();
        }).description("Number of tenants with an active quota semaphore").tags(commonTags())
                .register(registry);
    }

    private void refreshTenantGauges() {
        service.availablePermits().keySet().forEach(this::registerIfAbsent);
    }

    private void registerIfAbsent(Long tenantId) {
        if (registered.putIfAbsent(tenantId, Boolean.TRUE) != null || !hasRegistry()) {
            return;
        }
        String tid = String.valueOf(tenantId);
        Gauge.builder("s2_tenant_jdbc_permits_available", service,
                s -> s.availablePermits().getOrDefault(tenantId, 0))
                .tags(commonTags().and("tenantId", tid))
                .description("Available JDBC permits for tenant").register(getRegistry());
        Gauge.builder("s2_tenant_jdbc_permits_waiting", service,
                s -> s.waitingThreads().getOrDefault(tenantId, 0))
                .tags(commonTags().and("tenantId", tid))
                .description("Threads waiting for a JDBC permit for tenant")
                .register(getRegistry());
    }
}
