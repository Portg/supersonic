package com.tencent.supersonic.auth.api.authentication.service;

import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.request.OrganizationReq;

import java.util.List;
import java.util.Set;

public interface OrganizationService {

    /**
     * Get organization tree for current tenant
     */
    List<Organization> getOrganizationTree();

    /**
     * Get organization by id
     */
    Organization getOrganization(Long id);

    /**
     * Create organization
     */
    Organization createOrganization(OrganizationReq req, String createdBy);

    /**
     * Update organization
     */
    Organization updateOrganization(Long id, OrganizationReq req, String updatedBy);

    /**
     * Delete organization
     */
    void deleteOrganization(Long id);

    /**
     * Get users by organization id
     */
    List<Long> getUserIdsByOrganization(Long organizationId);

    /**
     * Get all organization ids for a user
     */
    Set<Long> getOrganizationIdsByUser(Long userId);

    /**
     * Assign user to organization
     */
    void assignUserToOrganization(Long userId, Long organizationId, boolean isPrimary,
            String createdBy);

    /**
     * Remove user from organization
     */
    void removeUserFromOrganization(Long userId, Long organizationId);

    /**
     * Set user's primary organization
     */
    void setUserPrimaryOrganization(Long userId, Long organizationId);

    /**
     * Batch assign users to organization
     */
    void batchAssignUsersToOrganization(List<Long> userIds, Long organizationId, String createdBy);

    /**
     * Batch remove users from organization
     */
    void batchRemoveUsersFromOrganization(List<Long> userIds, Long organizationId);
}
