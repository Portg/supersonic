import { request } from '@umijs/max';

const API_BASE = process.env.AUTH_API_BASE_URL || '/api/auth/';

export interface Organization {
  id: string;
  parentId: string;
  name: string;
  fullName: string;
  subOrganizations?: Organization[];
  isRoot?: boolean;
}

export interface OrganizationReq {
  parentId?: number;
  name: string;
  sortOrder?: number;
  status?: number;
}

export interface UserOrganizationReq {
  userId?: number;
  userIds?: number[];
  organizationId: number;
  isPrimary?: boolean;
}

// Get organization tree
export async function getOrganizationTree() {
  return request<any>(`${API_BASE}organization/tree`, {
    method: 'GET',
  });
}

// Get organization by id
export async function getOrganization(id: number) {
  return request<any>(`${API_BASE}organization/${id}`, {
    method: 'GET',
  });
}

// Create organization
export async function createOrganization(data: OrganizationReq) {
  return request<any>(`${API_BASE}organization`, {
    method: 'POST',
    data,
  });
}

// Update organization
export async function updateOrganization(id: number, data: OrganizationReq) {
  return request<any>(`${API_BASE}organization/${id}`, {
    method: 'PUT',
    data,
  });
}

// Delete organization
export async function deleteOrganization(id: number) {
  return request<any>(`${API_BASE}organization/${id}`, {
    method: 'DELETE',
  });
}

// Get users by organization
export async function getUsersByOrganization(id: number) {
  return request<any>(`${API_BASE}organization/${id}/users`, {
    method: 'GET',
  });
}

// Get user's organizations
export async function getUserOrganizations(userId: number) {
  return request<any>(`${API_BASE}organization/user/${userId}`, {
    method: 'GET',
  });
}

// Assign user to organization
export async function assignUserToOrganization(data: UserOrganizationReq) {
  return request<any>(`${API_BASE}organization/assign`, {
    method: 'POST',
    data,
  });
}

// Remove user from organization
export async function removeUserFromOrganization(data: UserOrganizationReq) {
  return request<any>(`${API_BASE}organization/remove`, {
    method: 'POST',
    data,
  });
}

// Set user's primary organization
export async function setUserPrimaryOrganization(data: UserOrganizationReq) {
  return request<any>(`${API_BASE}organization/setPrimary`, {
    method: 'POST',
    data,
  });
}

// Batch assign users to organization
export async function batchAssignUsersToOrganization(data: UserOrganizationReq) {
  return request<any>(`${API_BASE}organization/batchAssign`, {
    method: 'POST',
    data,
  });
}

// Batch remove users from organization
export async function batchRemoveUsersFromOrganization(data: UserOrganizationReq) {
  return request<any>(`${API_BASE}organization/batchRemove`, {
    method: 'POST',
    data,
  });
}
