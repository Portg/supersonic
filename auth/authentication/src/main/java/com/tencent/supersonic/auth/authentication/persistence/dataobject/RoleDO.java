package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_role")
public class RoleDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String description;

    /** 作用域：PLATFORM=平台级, TENANT=租户级 */
    private String scope;

    private Long tenantId;

    /** 是否系统内置角色 */
    private Integer isSystem;

    /** 状态：1=启用，0=禁用 */
    private Integer status;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;
}
