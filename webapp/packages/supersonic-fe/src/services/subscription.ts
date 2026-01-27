import request from './request';
import type { SubscriptionPlan, TenantSubscription } from './tenant';

/**
 * Subscription API Services
 *
 * API Design follows Google RESTful API guidelines:
 * - Version: v1 (major version in URL path)
 * - No module prefix (removed /auth/)
 *
 * Versioning Strategy:
 * - /api/v1/... for current version
 * - /api/v2/... for breaking changes (future)
 */

const API_V1 = '/api/v1';

// ==================== 租户自助订阅管理 ====================
// Resource: subscription-plans, my-subscription

export async function getSubscriptionPlans(): Promise<Result<SubscriptionPlan[]>> {
  return request.get(`${API_V1}/subscription-plans`);
}

export async function getSubscriptionPlanById(planId: number): Promise<Result<SubscriptionPlan>> {
  return request.get(`${API_V1}/subscription-plans/${planId}`);
}

export async function getCurrentSubscription(): Promise<Result<TenantSubscription>> {
  return request.get(`${API_V1}/my-subscription`);
}

export async function getSubscriptionHistory(): Promise<Result<TenantSubscription[]>> {
  return request.get(`${API_V1}/my-subscription/history`);
}

export async function changeSubscription(
  data: { planId: number; billingCycle?: string },
): Promise<Result<TenantSubscription>> {
  return request.put(`${API_V1}/my-subscription`, { data });
}

export async function cancelSubscription(): Promise<Result<void>> {
  return request.delete(`${API_V1}/my-subscription`);
}

// ==================== 管理员订阅计划管理 ====================
// Resource: subscription-plans (admin operations with @PreAuthorize)

export async function getAdminSubscriptionPlans(): Promise<Result<SubscriptionPlan[]>> {
  return request.get(`${API_V1}/subscription-plans/all`);
}

export async function getAdminSubscriptionPlan(planId: number): Promise<Result<SubscriptionPlan>> {
  return request.get(`${API_V1}/subscription-plans/${planId}`);
}

export async function createSubscriptionPlan(
  data: Partial<SubscriptionPlan>,
): Promise<Result<SubscriptionPlan>> {
  return request.post(`${API_V1}/subscription-plans`, { data });
}

export async function updateSubscriptionPlan(
  planId: number,
  data: Partial<SubscriptionPlan>,
): Promise<Result<SubscriptionPlan>> {
  return request.put(`${API_V1}/subscription-plans/${planId}`, { data });
}

export async function deleteSubscriptionPlan(planId: number): Promise<Result<void>> {
  return request.delete(`${API_V1}/subscription-plans/${planId}`);
}

// ==================== 管理员租户订阅管理 ====================
// Resource: tenants/{tenantId}/subscription (admin operations with @PreAuthorize)

export async function getTenantSubscription(tenantId: number): Promise<Result<TenantSubscription>> {
  return request.get(`${API_V1}/tenants/${tenantId}/subscription`);
}

export async function listTenantSubscriptions(tenantId: number): Promise<Result<TenantSubscription[]>> {
  return request.get(`${API_V1}/tenants/${tenantId}/subscriptions`);
}

export async function updateTenantSubscription(
  tenantId: number,
  data: { planId: number; billingCycle: string },
): Promise<Result<TenantSubscription>> {
  return request.put(`${API_V1}/tenants/${tenantId}/subscription`, { data });
}

export async function cancelTenantSubscription(tenantId: number): Promise<Result<void>> {
  return request.delete(`${API_V1}/tenants/${tenantId}/subscription`);
}
