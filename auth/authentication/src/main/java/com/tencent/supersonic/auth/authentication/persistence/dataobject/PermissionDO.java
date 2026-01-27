package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_permission")
public class PermissionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    /** 权限类型：MENU, BUTTON, API, DATA */
    private String type;

    /** 作用域：PLATFORM=平台级, TENANT=租户级 */
    private String scope;

    /** 父权限ID */
    private Long parentId;

    /** 菜单路径或API路径 */
    private String path;

    /** 菜单图标 */
    private String icon;

    /** 排序 */
    private Integer sortOrder;

    private String description;

    /** 状态：1=启用，0=禁用 */
    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
