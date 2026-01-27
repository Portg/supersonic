package com.tencent.supersonic.auth.authentication.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.Tenant;
import com.tencent.supersonic.auth.api.authentication.pojo.TenantUsage;
import com.tencent.supersonic.auth.api.authentication.service.TenantService;
import com.tencent.supersonic.auth.api.authentication.service.UsageTrackingService;
import com.tencent.supersonic.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for tenant management.
 */
@RestController
@RequestMapping("/api/auth/tenant")
@Slf4j
public class TenantController {

    private static final Long DEFAULT_TENANT_ID = 1L;

    private final TenantService tenantService;
    private final UsageTrackingService usageTrackingService;

    public TenantController(TenantService tenantService,
            UsageTrackingService usageTrackingService) {
        this.tenantService = tenantService;
        this.usageTrackingService = usageTrackingService;
    }

    /**
     * Get effective tenant ID from context or use default.
     */
    private Long getEffectiveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.warn("TenantContext.getTenantId() returned null, using default tenant: {}",
                    DEFAULT_TENANT_ID);
            return DEFAULT_TENANT_ID;
        }
        return tenantId;
    }

    /**
     * Get the current tenant information.
     */
    @GetMapping("/current")
    public Tenant getCurrentTenant() {
        Long tenantId = getEffectiveTenantId();
        Optional<Tenant> tenant = tenantService.getTenantById(tenantId);
        return tenant.orElse(null);
    }

    /**
     * Update the current tenant information.
     */
    @PutMapping("/current")
    public Tenant updateCurrentTenant(@RequestBody Tenant tenant) {
        Long tenantId = getEffectiveTenantId();
        tenant.setId(tenantId);
        return tenantService.updateTenant(tenant);
    }

    /**
     * Get today's usage for the current tenant.
     */
    @GetMapping("/usage/today")
    public TenantUsage getTodayUsage() {
        Long tenantId = getEffectiveTenantId();
        return usageTrackingService.getTodayUsage(tenantId);
    }

    /**
     * Get usage for the current tenant over a date range.
     */
    @GetMapping("/usage/range")
    public List<TenantUsage> getUsageRange(@RequestParam String startDate,
            @RequestParam String endDate) {
        Long tenantId = getEffectiveTenantId();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return usageTrackingService.getUsageRange(tenantId, start, end);
    }

    /**
     * Get monthly usage summary for the current tenant.
     */
    @GetMapping("/usage/monthly")
    public TenantUsage getMonthlyUsage(@RequestParam int year, @RequestParam int month) {
        Long tenantId = getEffectiveTenantId();
        return usageTrackingService.getMonthlyUsage(tenantId, year, month);
    }

    /**
     * Check if a tenant code is available.
     */
    @GetMapping("/check-code")
    public Boolean checkTenantCode(@RequestParam String code) {
        return tenantService.isTenantCodeAvailable(code);
    }
}
