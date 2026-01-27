package com.tencent.supersonic.auth.api.authentication.service;

import com.tencent.supersonic.auth.api.authentication.pojo.Role;

import java.util.List;

public interface RoleService {

    /**
     * 获取角色列表
     */
    List<Role> getRoleList(Long tenantId);

    /**
     * 根据作用域获取角色列表 (PLATFORM/TENANT)
     */
    List<Role> getRolesByScope(String scope, Long tenantId);

    /**
     * 根据ID获取角色
     */
    Role getRoleById(Long id);

    /**
     * 根据code获取角色
     */
    Role getRoleByCode(String code, Long tenantId);

    /**
     * 创建角色
     */
    Role createRole(Role role, String operator);

    /**
     * 更新角色
     */
    Role updateRole(Role role, String operator);

    /**
     * 删除角色
     */
    void deleteRole(Long id);

    /**
     * 获取角色的权限码列表
     */
    List<String> getPermissionCodesByRoleId(Long roleId);

    /**
     * 获取角色的权限ID列表
     */
    List<Long> getPermissionIdsByRoleId(Long roleId);

    /**
     * 更新角色的权限
     */
    void updateRolePermissions(Long roleId, List<Long> permissionIds, String operator);
}
