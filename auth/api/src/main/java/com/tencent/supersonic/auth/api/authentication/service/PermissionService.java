package com.tencent.supersonic.auth.api.authentication.service;

import com.tencent.supersonic.auth.api.authentication.pojo.Permission;

import java.util.List;

public interface PermissionService {

    /**
     * 获取所有权限列表
     */
    List<Permission> getAllPermissions();

    /**
     * 按类型获取权限列表
     */
    List<Permission> getPermissionsByType(String type);

    /**
     * 按作用域获取权限列表 (PLATFORM/TENANT)
     */
    List<Permission> getPermissionsByScope(String scope);

    /**
     * 根据ID获取权限
     */
    Permission getPermissionById(Long id);

    /**
     * 根据code获取权限
     */
    Permission getPermissionByCode(String code);

    /**
     * 创建权限
     */
    Permission createPermission(Permission permission);

    /**
     * 更新权限
     */
    Permission updatePermission(Permission permission);

    /**
     * 删除权限
     */
    void deletePermission(Long id);

    /**
     * 获取用户的所有权限码（根据用户的角色）
     */
    List<String> getPermissionCodesByUserId(Long userId);

    /**
     * 获取权限树（菜单树）
     */
    List<Permission> getPermissionTree();
}
