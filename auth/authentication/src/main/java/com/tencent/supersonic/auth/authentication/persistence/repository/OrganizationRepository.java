package com.tencent.supersonic.auth.authentication.persistence.repository;

import com.tencent.supersonic.auth.authentication.persistence.dataobject.OrganizationDO;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserOrganizationDO;

import java.util.List;

public interface OrganizationRepository {

    /**
     * 获取所有组织列表
     */
    List<OrganizationDO> getOrganizationList();

    /**
     * 根据租户ID获取组织列表
     */
    List<OrganizationDO> getOrganizationListByTenantId(Long tenantId);

    /**
     * 根据ID获取组织
     */
    OrganizationDO getOrganization(Long id);

    /**
     * 根据父ID获取子组织列表
     */
    List<OrganizationDO> getOrganizationsByParentId(Long parentId);

    /**
     * 添加组织
     */
    void addOrganization(OrganizationDO organizationDO);

    /**
     * 更新组织
     */
    void updateOrganization(OrganizationDO organizationDO);

    /**
     * 删除组织
     */
    void deleteOrganization(Long id);

    /**
     * 获取根组织列表
     */
    List<OrganizationDO> getRootOrganizations(Long tenantId);

    // ========== 用户-组织关联方法 ==========

    /**
     * 获取用户所属的所有组织ID
     */
    List<Long> getOrganizationIdsByUserId(Long userId);

    /**
     * 获取组织下的所有用户ID
     */
    List<Long> getUserIdsByOrganizationId(Long organizationId);

    /**
     * 添加用户-组织关联
     */
    void addUserOrganization(UserOrganizationDO userOrganizationDO);

    /**
     * 删除用户-组织关联
     */
    void deleteUserOrganization(Long userId, Long organizationId);

    /**
     * 删除用户的所有组织关联
     */
    void deleteUserOrganizationsByUserId(Long userId);

    /**
     * 获取用户-组织关联
     */
    List<UserOrganizationDO> getUserOrganizations(Long userId);

    /**
     * 更新用户-组织关联
     */
    void updateUserOrganization(UserOrganizationDO userOrganizationDO);

    /**
     * 检查用户-组织关联是否存在
     */
    boolean existsUserOrganization(Long userId, Long organizationId);
}
