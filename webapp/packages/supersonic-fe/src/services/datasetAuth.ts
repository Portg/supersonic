import request from '@/services/request';

export interface DataSetAuthGroup {
  groupId?: number;
  datasetId: number;
  name: string;
  authRules?: AuthRule[];
  dimensionFilters?: string[];
  dimensionFilterDescription?: string;
  authorizedUsers?: string[];
  authorizedDepartmentIds?: string[];
  inheritFromModel?: number;
}

export interface AuthRule {
  dimensions?: string[];
  metrics?: string[];
}

export interface AuthorizedResourceResp {
  authResList: Array<{ modelId: number; name: string }>;
  filters: Array<{ description: string; expressions: string[] }>;
}

export interface PermissionCheckResult {
  hasViewPermission: boolean;
  hasAdminPermission: boolean;
}

// Query auth groups for a dataset
export async function queryDataSetAuthGroups(
  datasetId: number,
  groupId?: number,
): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}dataset/auth/queryGroups`, {
    params: { datasetId, groupId },
  });
}

// Create auth group for a dataset
export async function createDataSetAuthGroup(
  group: DataSetAuthGroup,
): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}dataset/auth/createGroup`, {
    data: group,
  });
}

// Update auth group for a dataset
export async function updateDataSetAuthGroup(
  group: DataSetAuthGroup,
): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}dataset/auth/updateGroup`, {
    data: group,
  });
}

// Remove auth group from a dataset
export async function removeDataSetAuthGroup(groupId: number): Promise<any> {
  return request.delete(
    `${process.env.API_BASE_URL}dataset/auth/removeGroup/${groupId}`,
  );
}

// Query authorized resources for a dataset
export async function queryDataSetAuthorizedResources(
  datasetId: number,
): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}dataset/auth/queryAuthorizedRes`, {
    params: { datasetId },
  });
}

// Check permission for a dataset
export async function checkDataSetPermission(
  datasetId: number,
): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}dataset/auth/checkPermission`, {
    params: { datasetId },
  });
}

// Get row filters for a dataset
export async function getDataSetRowFilters(datasetId: number): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}dataset/auth/rowFilters`, {
    params: { datasetId },
  });
}
