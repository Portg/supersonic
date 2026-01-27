package com.tencent.supersonic.billing.api.service;

import com.tencent.supersonic.billing.api.pojo.SubscriptionPlan;
import com.tencent.supersonic.billing.api.pojo.TenantSubscription;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for subscription management.
 */
public interface SubscriptionService {

    /**
     * Get all available subscription plans.
     *
     * @return list of subscription plans
     */
    List<SubscriptionPlan> getAllPlans();

    /**
     * Get active subscription plans.
     *
     * @return list of active subscription plans
     */
    List<SubscriptionPlan> getActivePlans();

    /**
     * Get a subscription plan by ID.
     *
     * @param planId the plan ID
     * @return the plan if found
     */
    Optional<SubscriptionPlan> getPlanById(Long planId);

    /**
     * Get a subscription plan by code.
     *
     * @param code the plan code
     * @return the plan if found
     */
    Optional<SubscriptionPlan> getPlanByCode(String code);

    /**
     * Get the default subscription plan.
     *
     * @return the default plan
     */
    SubscriptionPlan getDefaultPlan();

    /**
     * Create a new subscription plan.
     *
     * @param plan the plan to create
     * @return the created plan
     */
    SubscriptionPlan createPlan(SubscriptionPlan plan);

    /**
     * Update an existing subscription plan.
     *
     * @param plan the plan to update
     * @return the updated plan
     */
    SubscriptionPlan updatePlan(SubscriptionPlan plan);

    /**
     * Delete a subscription plan.
     *
     * @param planId the plan ID to delete
     */
    void deletePlan(Long planId);

    /**
     * Create a subscription for a tenant.
     *
     * @param subscription the subscription to create
     * @return the created subscription
     */
    TenantSubscription createSubscription(TenantSubscription subscription);

    /**
     * Update an existing subscription.
     *
     * @param subscription the subscription to update
     * @return the updated subscription
     */
    TenantSubscription updateSubscription(TenantSubscription subscription);

    /**
     * Get active subscription for a tenant.
     *
     * @param tenantId the tenant ID
     * @return the active subscription if exists
     */
    Optional<TenantSubscription> getActiveSubscription(Long tenantId);

    /**
     * Get all subscriptions for a tenant.
     *
     * @param tenantId the tenant ID
     * @return list of subscriptions
     */
    List<TenantSubscription> getSubscriptionsByTenant(Long tenantId);

    /**
     * Cancel a subscription.
     *
     * @param subscriptionId the subscription ID
     */
    void cancelSubscription(Long subscriptionId);

    /**
     * Change a tenant's subscription to a new plan.
     *
     * @param tenantId the tenant ID
     * @param newPlanId the new plan ID
     * @param billingCycle the billing cycle (MONTHLY/YEARLY), optional
     * @return the new subscription
     */
    TenantSubscription changeSubscription(Long tenantId, Long newPlanId, String billingCycle);

    /**
     * Admin: Assign a subscription to a tenant.
     *
     * @param tenantId the tenant ID
     * @param planId the plan ID to assign
     * @param billingCycle the billing cycle (MONTHLY/YEARLY)
     * @return the created subscription
     */
    TenantSubscription assignSubscription(Long tenantId, Long planId, String billingCycle);
}
