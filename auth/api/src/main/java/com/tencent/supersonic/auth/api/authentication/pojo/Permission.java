package com.tencent.supersonic.auth.api.authentication.pojo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class Permission {

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

    /** 状态：true=启用，false=禁用 */
    private Boolean status;

    private Date createdAt;

    private Date updatedAt;

    /** 子权限列表（用于构建树形结构） */
    private List<Permission> children;
}
