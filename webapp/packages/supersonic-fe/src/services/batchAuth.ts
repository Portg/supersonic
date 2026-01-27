import request from '@/services/request';

export interface AuthGroup {
  groupId?: number;
  modelId: number;
  name: string;
  authRules?: AuthRule[];
  dimensionFilters?: string[];
  dimensionFilterDescription?: string;
  authorizedUsers?: string[];
  authorizedDepartmentIds?: string[];
}

export interface AuthRule {
  dimensions?: string[];
  metrics?: string[];
}

export interface BatchAuthGroupReq {
  authGroups: AuthGroup[];
}

export interface BatchAuthorizeReq {
  groupIds: number[];
  users?: string[];
  departmentIds?: string[];
  operationType?: 'ADD' | 'REMOVE';
}

export interface BatchOperationResult {
  successCount: number;
  failCount: number;
  successIds: number[];
  failDetails: Record<number, string>;
}

// Batch create auth groups
export async function batchCreateAuthGroups(
  groups: AuthGroup[],
): Promise<any> {
  return request.post(`${process.env.AUTH_API_BASE_URL}batchCreateGroups`, {
    data: { authGroups: groups },
  });
}

// Batch update auth groups
export async function batchUpdateAuthGroups(
  groups: AuthGroup[],
): Promise<any> {
  return request.post(`${process.env.AUTH_API_BASE_URL}batchUpdateGroups`, {
    data: { authGroups: groups },
  });
}

// Batch remove auth groups
export async function batchRemoveAuthGroups(groupIds: number[]): Promise<any> {
  return request.post(`${process.env.AUTH_API_BASE_URL}batchRemoveGroups`, {
    data: { groupIds },
  });
}

// Batch authorize users/departments to groups
export async function batchAuthorize(req: BatchAuthorizeReq): Promise<any> {
  return request.post(`${process.env.AUTH_API_BASE_URL}batchAuthorize`, {
    data: req,
  });
}

// Batch revoke authorization from groups
export async function batchRevokeAuthorize(
  req: BatchAuthorizeReq,
): Promise<any> {
  return request.post(`${process.env.AUTH_API_BASE_URL}batchRevokeAuthorize`, {
    data: req,
  });
}
