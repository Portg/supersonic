package com.tencent.supersonic.common.quota;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TenantQuotaMeterBinderTest {

    private static final String MODULE_KEY = "module";
    private static final String MODULE_VALUE = "tenant_quota";
    private static final String ORIGIN_VALUE = "TenantQuotaMeterBinder";

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

        Double available =
                registry.find("s2_tenant_jdbc_permits_available").tag(MODULE_KEY, MODULE_VALUE)
                        .tag("origin", ORIGIN_VALUE).tag("tenantId", "55").gauge().value();
        Double waiting =
                registry.find("s2_tenant_jdbc_permits_waiting").tag(MODULE_KEY, MODULE_VALUE)
                        .tag("origin", ORIGIN_VALUE).tag("tenantId", "55").gauge().value();

        assertNotNull(available);
        assertNotNull(waiting);
        assertEquals(3.0, available);
        assertEquals(0.0, waiting);
    }

    @Test
    void registersTenantGaugesDiscoveredAfterBinding() {
        TenantQuotaConfig config = new TenantQuotaConfig();
        config.setEnabled(true);
        config.getDefaultQuota().setJdbcConcurrent(2);
        config.getDefaultQuota().setAcquireTimeoutMs(100);
        InMemoryTenantQuotaService svc = new InMemoryTenantQuotaService(config, tid -> null);

        MeterRegistry registry = new SimpleMeterRegistry();
        new TenantQuotaMeterBinder(svc).bindTo(registry);

        svc.acquireJdbc(77L, 100).close();
        registry.find("s2_tenant_quota_known_tenants").tag(MODULE_KEY, MODULE_VALUE)
                .tag("origin", ORIGIN_VALUE).gauge().value();

        Double available =
                registry.find("s2_tenant_jdbc_permits_available").tag(MODULE_KEY, MODULE_VALUE)
                        .tag("origin", ORIGIN_VALUE).tag("tenantId", "77").gauge().value();

        assertNotNull(available);
        assertEquals(2.0, available);
    }
}
