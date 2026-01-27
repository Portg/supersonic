package com.tencent.supersonic.auth.authentication.service;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.request.OrganizationReq;
import com.tencent.supersonic.auth.api.authentication.service.OrganizationService;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.OrganizationDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserOrganizationDO;
import com.tencent.supersonic.auth.authentication.persistence.repository.OrganizationRepository;
import com.tencent.supersonic.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationServiceImpl(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    public List<Organization> getOrganizationTree() {
        Long tenantId = TenantContext.getTenantIdOrDefault(1L);
        List<OrganizationDO> allOrganizations =
                organizationRepository.getOrganizationListByTenantId(tenantId);
        if (allOrganizations.isEmpty()) {
            return Lists.newArrayList();
        }
        return buildOrganizationTree(allOrganizations);
    }

    @Override
    public Organization getOrganization(Long id) {
        OrganizationDO org = organizationRepository.getOrganization(id);
        if (org == null) {
            return null;
        }
        return convertToOrganization(org);
    }

    @Override
    @Transactional
    public Organization createOrganization(OrganizationReq req, String createdBy) {
        Long tenantId = TenantContext.getTenantIdOrDefault(1L);

        OrganizationDO org = new OrganizationDO();
        org.setParentId(req.getParentId() != null ? req.getParentId() : 0L);
        org.setName(req.getName());
        org.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        org.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        org.setTenantId(tenantId);
        org.setIsRoot(req.getParentId() == null || req.getParentId() == 0L ? 1 : 0);
        org.setCreatedAt(new Date());
        org.setCreatedBy(createdBy);
        org.setUpdatedAt(new Date());
        org.setUpdatedBy(createdBy);

        // Build full name
        org.setFullName(buildFullName(req.getParentId(), req.getName()));

        organizationRepository.addOrganization(org);

        return convertToOrganization(org);
    }

    @Override
    @Transactional
    public Organization updateOrganization(Long id, OrganizationReq req, String updatedBy) {
        OrganizationDO org = organizationRepository.getOrganization(id);
        if (org == null) {
            throw new RuntimeException("Organization not found: " + id);
        }

        if (req.getParentId() != null) {
            org.setParentId(req.getParentId());
            org.setIsRoot(req.getParentId() == 0L ? 1 : 0);
        }
        if (req.getName() != null) {
            org.setName(req.getName());
        }
        if (req.getSortOrder() != null) {
            org.setSortOrder(req.getSortOrder());
        }
        if (req.getStatus() != null) {
            org.setStatus(req.getStatus());
        }

        // Rebuild full name
        org.setFullName(buildFullName(org.getParentId(), org.getName()));
        org.setUpdatedAt(new Date());
        org.setUpdatedBy(updatedBy);

        organizationRepository.updateOrganization(org);

        // Update children's full name if name changed
        if (req.getName() != null) {
            updateChildrenFullName(id);
        }

        return convertToOrganization(org);
    }

    @Override
    @Transactional
    public void deleteOrganization(Long id) {
        // Check if has children
        List<OrganizationDO> children = organizationRepository.getOrganizationsByParentId(id);
        if (!children.isEmpty()) {
            throw new RuntimeException("Cannot delete organization with children");
        }

        // Check if has users
        List<Long> userIds = organizationRepository.getUserIdsByOrganizationId(id);
        if (!userIds.isEmpty()) {
            throw new RuntimeException("Cannot delete organization with users assigned");
        }

        organizationRepository.deleteOrganization(id);
    }

    @Override
    public List<Long> getUserIdsByOrganization(Long organizationId) {
        return organizationRepository.getUserIdsByOrganizationId(organizationId);
    }

    @Override
    public Set<Long> getOrganizationIdsByUser(Long userId) {
        List<Long> orgIds = organizationRepository.getOrganizationIdsByUserId(userId);
        return new HashSet<>(orgIds);
    }

    @Override
    @Transactional
    public void assignUserToOrganization(Long userId, Long organizationId, boolean isPrimary,
            String createdBy) {
        // Check if already assigned
        List<UserOrganizationDO> existing = organizationRepository.getUserOrganizations(userId);
        boolean alreadyAssigned =
                existing.stream().anyMatch(uo -> uo.getOrganizationId().equals(organizationId));

        if (alreadyAssigned) {
            if (isPrimary) {
                setUserPrimaryOrganization(userId, organizationId);
            }
            return;
        }

        UserOrganizationDO userOrg = new UserOrganizationDO();
        userOrg.setUserId(userId);
        userOrg.setOrganizationId(organizationId);
        userOrg.setIsPrimary(isPrimary ? 1 : 0);
        userOrg.setCreatedAt(new Date());
        userOrg.setCreatedBy(createdBy);

        // If setting as primary, clear other primary flags
        if (isPrimary) {
            clearUserPrimaryOrganization(userId);
        }

        organizationRepository.addUserOrganization(userOrg);
    }

    @Override
    @Transactional
    public void removeUserFromOrganization(Long userId, Long organizationId) {
        organizationRepository.deleteUserOrganization(userId, organizationId);
    }

    @Override
    @Transactional
    public void setUserPrimaryOrganization(Long userId, Long organizationId) {
        clearUserPrimaryOrganization(userId);

        List<UserOrganizationDO> userOrgs = organizationRepository.getUserOrganizations(userId);
        Optional<UserOrganizationDO> targetOrg = userOrgs.stream()
                .filter(uo -> uo.getOrganizationId().equals(organizationId)).findFirst();

        if (targetOrg.isPresent()) {
            UserOrganizationDO org = targetOrg.get();
            org.setIsPrimary(1);
            organizationRepository.updateUserOrganization(org);
        }
    }

    @Override
    @Transactional
    public void batchAssignUsersToOrganization(List<Long> userIds, Long organizationId,
            String createdBy) {
        for (Long userId : userIds) {
            assignUserToOrganization(userId, organizationId, false, createdBy);
        }
    }

    @Override
    @Transactional
    public void batchRemoveUsersFromOrganization(List<Long> userIds, Long organizationId) {
        for (Long userId : userIds) {
            removeUserFromOrganization(userId, organizationId);
        }
    }

    // ========== Private Helper Methods ==========

    private void clearUserPrimaryOrganization(Long userId) {
        List<UserOrganizationDO> userOrgs = organizationRepository.getUserOrganizations(userId);
        for (UserOrganizationDO uo : userOrgs) {
            if (uo.getIsPrimary() != null && uo.getIsPrimary() == 1) {
                uo.setIsPrimary(0);
                organizationRepository.updateUserOrganization(uo);
            }
        }
    }

    private String buildFullName(Long parentId, String name) {
        if (parentId == null || parentId == 0L) {
            return name;
        }
        OrganizationDO parent = organizationRepository.getOrganization(parentId);
        if (parent == null) {
            return name;
        }
        return parent.getFullName() + "/" + name;
    }

    private void updateChildrenFullName(Long parentId) {
        List<OrganizationDO> children = organizationRepository.getOrganizationsByParentId(parentId);
        for (OrganizationDO child : children) {
            child.setFullName(buildFullName(child.getParentId(), child.getName()));
            organizationRepository.updateOrganization(child);
            updateChildrenFullName(child.getId());
        }
    }

    private Organization convertToOrganization(OrganizationDO org) {
        Organization organization = new Organization();
        organization.setId(String.valueOf(org.getId()));
        organization.setParentId(String.valueOf(org.getParentId()));
        organization.setName(org.getName());
        organization.setFullName(org.getFullName());
        organization.setRoot(org.getIsRoot() != null && org.getIsRoot() == 1);
        return organization;
    }

    private List<Organization> buildOrganizationTree(List<OrganizationDO> allOrganizations) {
        Map<Long, List<OrganizationDO>> childrenMap = allOrganizations.stream()
                .collect(Collectors.groupingBy(OrganizationDO::getParentId));

        List<OrganizationDO> rootOrgs = allOrganizations.stream()
                .filter(org -> org.getIsRoot() != null && org.getIsRoot() == 1)
                .collect(Collectors.toList());

        List<Organization> result = Lists.newArrayList();
        for (OrganizationDO root : rootOrgs) {
            Organization org = buildOrganizationNode(root, childrenMap);
            result.add(org);
        }
        return result;
    }

    private Organization buildOrganizationNode(OrganizationDO org,
            Map<Long, List<OrganizationDO>> childrenMap) {
        Organization organization = convertToOrganization(org);

        List<OrganizationDO> children = childrenMap.get(org.getId());
        if (children != null && !children.isEmpty()) {
            List<Organization> subOrgs = Lists.newArrayList();
            for (OrganizationDO child : children) {
                subOrgs.add(buildOrganizationNode(child, childrenMap));
            }
            organization.setSubOrganizations(subOrgs);
        }

        return organization;
    }
}
