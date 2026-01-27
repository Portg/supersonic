package com.tencent.supersonic.auth.api.authentication.pojo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class Role {

    private Long id;

    private String name;

    private String code;

    private String description;

    /** 作用域：PLATFORM=平台级, TENANT=租户级 */
    private String scope;

    private Long tenantId;

    /** 是否系统内置角色 */
    private Boolean isSystem;

    /** 状态：true=启用，false=禁用 */
    private Boolean status;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;

    /** 权限ID列表 */
    private List<Long> permissionIds;

    /** 权限码列表 */
    private List<String> permissionCodes;
}
