package com.tencent.supersonic.billing.server.rest;

import com.tencent.supersonic.billing.api.pojo.SubscriptionPlan;
import com.tencent.supersonic.billing.api.service.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for subscription plan management. <br/>
 * API Design follows Google RESTful API guidelines: - Version: v1 (major version in URL path) -
 * Resource: subscription-plans <br/>
 * Endpoints: - GET /api/v1/subscription-plans - List active plans (public) - GET
 * /api/v1/subscription-plans/all - List all plans (admin) - GET /api/v1/subscription-plans/{planId}
 * - Get a specific plan (public) - POST /api/v1/subscription-plans - Create a plan (admin) - PUT
 * /api/v1/subscription-plans/{planId} - Update a plan (admin) - DELETE
 * /api/v1/subscription-plans/{planId} - Delete a plan (admin)
 */
@RestController
@RequestMapping("/api/v1/subscription-plans")
@Slf4j
public class SubscriptionPlanController {

    private final SubscriptionService subscriptionService;

    public SubscriptionPlanController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * List all active subscription plans. Public endpoint - no authentication required.
     */
    @GetMapping
    public List<SubscriptionPlan> listActivePlans() {
        return subscriptionService.getActivePlans();
    }

    /**
     * List all subscription plans (including inactive). Admin only.
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLATFORM_ADMIN')")
    public List<SubscriptionPlan> listAllPlans() {
        return subscriptionService.getAllPlans();
    }

    /**
     * Get a specific subscription plan by ID. Public endpoint.
     */
    @GetMapping("/{planId}")
    public SubscriptionPlan getPlan(@PathVariable Long planId) {
        return subscriptionService.getPlanById(planId).orElse(null);
    }

    /**
     * Get the default subscription plan. Public endpoint.
     */
    @GetMapping("/default")
    public SubscriptionPlan getDefaultPlan() {
        return subscriptionService.getDefaultPlan();
    }

    /**
     * Create a new subscription plan. Admin only.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLATFORM_ADMIN')")
    public SubscriptionPlan createPlan(@RequestBody SubscriptionPlan plan) {
        return subscriptionService.createPlan(plan);
    }

    /**
     * Update an existing subscription plan. Admin only.
     */
    @PutMapping("/{planId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLATFORM_ADMIN')")
    public SubscriptionPlan updatePlan(@PathVariable Long planId,
            @RequestBody SubscriptionPlan plan) {
        plan.setId(planId);
        return subscriptionService.updatePlan(plan);
    }

    /**
     * Delete a subscription plan. Admin only.
     */
    @DeleteMapping("/{planId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLATFORM_ADMIN')")
    public void deletePlan(@PathVariable Long planId) {
        subscriptionService.deletePlan(planId);
    }
}
