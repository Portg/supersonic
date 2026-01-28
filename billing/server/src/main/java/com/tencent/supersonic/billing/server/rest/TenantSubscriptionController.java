package com.tencent.supersonic.billing.server.rest;

import com.tencent.supersonic.billing.api.pojo.TenantSubscription;
import com.tencent.supersonic.billing.api.request.SubscriptionRequest;
import com.tencent.supersonic.billing.api.service.SubscriptionService;
import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.context.TenantContext;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for tenant subscription management.
 *
 * <p>
 * API Design follows Google RESTful API guidelines:
 * </p>
 * <ul>
 * <li>Version: v1 (major version in URL path)</li>
 * <li>Resources: my-subscription (self-service), tenants/{tenantId}/subscription (admin)</li>
 * </ul>
 *
 * <p>
 * Self-Service Endpoints:
 * </p>
 * <ul>
 * <li>GET /api/v1/my-subscription - Get current subscription</li>
 * <li>GET /api/v1/my-subscription/history - Get subscription history</li>
 * <li>PUT /api/v1/my-subscription - Change subscription</li>
 * <li>DELETE /api/v1/my-subscription - Cancel subscription</li>
 * </ul>
 *
 * <p>
 * Admin Endpoints:
 * </p>
 * <ul>
 * <li>GET /api/v1/tenants/{tenantId}/subscription - Get tenant's subscription</li>
 * <li>GET /api/v1/tenants/{tenantId}/subscriptions - Get subscription history</li>
 * <li>PUT /api/v1/tenants/{tenantId}/subscription - Assign subscription</li>
 * <li>DELETE /api/v1/tenants/{tenantId}/subscription - Cancel subscription</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
public class TenantSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final TenantConfig tenantConfig;

    public TenantSubscriptionController(SubscriptionService subscriptionService,
            TenantConfig tenantConfig) {
        this.subscriptionService = subscriptionService;
        this.tenantConfig = tenantConfig;
    }

    /**
     * Get the current tenant's active subscription.
     */
    @GetMapping("/my-subscription")
    public TenantSubscription getCurrentSubscription() {
        return subscriptionService.getActiveSubscription(getEffectiveTenantId()).orElse(null);
    }

    /**
     * Get subscription history for the current tenant.
     */
    @GetMapping("/my-subscription/history")
    public List<TenantSubscription> getSubscriptionHistory() {
        return subscriptionService.getSubscriptionsByTenant(getEffectiveTenantId());
    }

    /**
     * Change the current tenant's subscription.
     */
    @PutMapping("/my-subscription")
    public TenantSubscription changeSubscription(@Valid @RequestBody SubscriptionRequest request) {
        Long tenantId = getEffectiveTenantId();
        return subscriptionService.changeSubscription(tenantId, request.getPlanId(),
                request.getBillingCycle());
    }

    /**
     * Cancel the current tenant's subscription.
     */
    @DeleteMapping("/my-subscription")
    public void cancelSubscription() {
        Long tenantId = getEffectiveTenantId();
        subscriptionService.getActiveSubscription(tenantId)
                .ifPresent(sub -> subscriptionService.cancelSubscription(sub.getId()));
    }

    /**
     * Get a specific tenant's active subscription.
     */
    @GetMapping("/tenants/{tenantId}/subscription")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLATFORM_ADMIN')")
    public TenantSubscription getTenantSubscription(@PathVariable Long tenantId) {
        return subscriptionService.getActiveSubscription(tenantId).orElse(null);
    }

    /**
     * Get all subscriptions for a tenant (history).
     */
    @GetMapping("/tenants/{tenantId}/subscriptions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLATFORM_ADMIN')")
    public List<TenantSubscription> listTenantSubscriptions(@PathVariable Long tenantId) {
        return subscriptionService.getSubscriptionsByTenant(tenantId);
    }

    /**
     * Assign a subscription to a tenant.
     */
    @PutMapping("/tenants/{tenantId}/subscription")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLATFORM_ADMIN')")
    public TenantSubscription assignSubscription(@PathVariable Long tenantId,
            @Valid @RequestBody SubscriptionRequest request) {
        return subscriptionService.assignSubscription(tenantId, request.getPlanId(),
                request.getBillingCycle());
    }

    /**
     * Cancel a tenant's subscription.
     */
    @DeleteMapping("/tenants/{tenantId}/subscription")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLATFORM_ADMIN')")
    public void cancelTenantSubscription(@PathVariable Long tenantId) {
        subscriptionService.getActiveSubscription(tenantId)
                .ifPresent(sub -> subscriptionService.cancelSubscription(sub.getId()));
    }

    /**
     * Get effective tenant ID from context or use default.
     */
    private Long getEffectiveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.warn("TenantContext.getTenantId() returned null, using default tenant: {}",
                    tenantConfig.getDefaultTenantId());
            return tenantConfig.getDefaultTenantId();
        }
        return tenantId;
    }
}
