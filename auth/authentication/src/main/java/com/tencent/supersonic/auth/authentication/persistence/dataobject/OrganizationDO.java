package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 组织数据对象 对应表: s2_organization
 */
@Data
@TableName("s2_organization")
public class OrganizationDO {

    /** 组织ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父组织ID，根组织为0 */
    private Long parentId;

    /** 组织名称 */
    private String name;

    /** 组织全名（包含父级路径） */
    private String fullName;

    /** 是否为根组织 */
    private Integer isRoot;

    /** 排序序号 */
    private Integer sortOrder;

    /** 状态: 1=启用, 0=禁用 */
    private Integer status;

    /** 租户ID */
    private Long tenantId;

    /** 创建时间 */
    private Date createdAt;

    /** 创建人 */
    private String createdBy;

    /** 更新时间 */
    private Date updatedAt;

    /** 更新人 */
    private String updatedBy;
}
