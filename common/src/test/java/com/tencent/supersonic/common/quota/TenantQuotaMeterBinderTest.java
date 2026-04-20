package com.tencent.supersonic.common.quota;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TenantQuotaMeterBinderTest {

    @Test
    void publishesAvailableAndWaitingGaugesPerTenant() {
        TenantQuotaConfig config = new TenantQuotaConfig();
        config.setEnabled(true);
        config.getDefaultQuota().setJdbcConcurrent(3);
        config.getDefaultQuota().setAcquireTimeoutMs(100);
        InMemoryTenantQuotaService svc = new InMemoryTenantQuotaService(config, tid -> null);

        svc.acquireJdbc(55L, 100).close();

        MeterRegistry registry = new SimpleMeterRegistry();
        new TenantQuotaMeterBinder(svc).bindTo(registry);

        Double available = registry.find("s2_tenant_jdbc_permits_available").tag("tenantId", "55")
                .gauge().value();
        Double waiting = registry.find("s2_tenant_jdbc_permits_waiting").tag("tenantId", "55")
                .gauge().value();

        assertNotNull(available);
        assertNotNull(waiting);
        assertEquals(3.0, available);
        assertEquals(0.0, waiting);
    }
}
