package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.PermissionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionDOMapper extends BaseMapper<PermissionDO> {

    /**
     * 根据权限类型查询
     */
    @Select("SELECT id, name, code, type, scope, parent_id, path, icon, sort_order, description, status, created_at, updated_at "
            + "FROM s2_permission WHERE type = #{type} AND status = 1 ORDER BY sort_order")
    List<PermissionDO> selectByType(@Param("type") String type);

    /**
     * 根据角色ID查询权限列表
     */
    @Select("SELECT p.id, p.name, p.code, p.type, p.scope, p.parent_id, p.path, p.icon, p.sort_order, p.description, p.status, p.created_at, p.updated_at "
            + "FROM s2_permission p "
            + "INNER JOIN s2_role_permission rp ON p.id = rp.permission_id "
            + "WHERE rp.role_id = #{roleId} AND p.status = 1 " + "ORDER BY p.sort_order")
    List<PermissionDO> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据多个角色ID查询权限列表
     */
    @Select("<script>"
            + "SELECT DISTINCT p.id, p.name, p.code, p.type, p.scope, p.parent_id, p.path, p.icon, p.sort_order, p.description, p.status, p.created_at, p.updated_at "
            + "FROM s2_permission p "
            + "INNER JOIN s2_role_permission rp ON p.id = rp.permission_id "
            + "WHERE rp.role_id IN "
            + "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>"
            + "#{roleId}" + "</foreach> " + "AND p.status = 1 " + "ORDER BY p.sort_order"
            + "</script>")
    List<PermissionDO> selectByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 根据权限code查询
     */
    @Select("SELECT id, name, code, type, scope, parent_id, path, icon, sort_order, description, status, created_at, updated_at "
            + "FROM s2_permission WHERE code = #{code}")
    PermissionDO selectByCode(@Param("code") String code);

    /**
     * 根据用户ID查询权限列表（通过s2_user_role和s2_role_permission关联）
     */
    @Select("SELECT DISTINCT p.id, p.name, p.code, p.type, p.scope, p.parent_id, p.path, p.icon, p.sort_order, p.description, p.status, p.created_at, p.updated_at "
            + "FROM s2_permission p "
            + "INNER JOIN s2_role_permission rp ON p.id = rp.permission_id "
            + "INNER JOIN s2_user_role ur ON rp.role_id = ur.role_id "
            + "WHERE ur.user_id = #{userId} AND p.status = 1 " + "ORDER BY p.sort_order")
    List<PermissionDO> selectByUserId(@Param("userId") Long userId);
}
