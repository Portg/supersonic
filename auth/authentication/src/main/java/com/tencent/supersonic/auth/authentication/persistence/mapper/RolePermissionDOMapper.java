package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.RolePermissionDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RolePermissionDOMapper extends BaseMapper<RolePermissionDO> {

    /**
     * 根据角色ID查询
     */
    @Select("SELECT id, role_id, permission_id, created_at FROM s2_role_permission WHERE role_id = #{roleId}")
    List<RolePermissionDO> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色ID删除
     */
    @Delete("DELETE FROM s2_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入
     */
    @Insert("<script>"
            + "INSERT INTO s2_role_permission (role_id, permission_id, created_at) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.roleId}, #{item.permissionId}, #{item.createdAt})" + "</foreach>"
            + "</script>")
    int batchInsert(@Param("list") List<RolePermissionDO> list);
}
