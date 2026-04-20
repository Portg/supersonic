import request from './request';

const API_V1 = '/api/v1';

export interface TenantQuota {
  id?: number;
  tenantId: number;
  jdbcConcurrent: number;
  llmConcurrent: number;
  monthlyQueryCount: number;
  acquireTimeoutMs: number;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export async function listTenantQuotas(): Promise<Result<TenantQuota[]>> {
  return request.get<Result<TenantQuota[]>>(`${API_V1}/admin/tenant-quotas`);
}

export async function upsertTenantQuota(
  tenantId: number,
  data: Partial<TenantQuota>,
): Promise<Result<TenantQuota>> {
  return request.put<Result<TenantQuota>>(`${API_V1}/admin/tenant-quotas/${tenantId}`, { data });
}

export async function deleteTenantQuota(tenantId: number): Promise<Result<void>> {
  return request.delete<Result<void>>(`${API_V1}/admin/tenant-quotas/${tenantId}`);
}
