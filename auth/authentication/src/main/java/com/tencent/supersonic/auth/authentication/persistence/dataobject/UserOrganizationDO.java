package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户-组织关联数据对象 对应表: s2_user_organization
 */
@Data
@TableName("s2_user_organization")
public class UserOrganizationDO {

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 组织ID */
    private Long organizationId;

    /** 是否为主组织 */
    private Integer isPrimary;

    /** 创建时间 */
    private Date createdAt;

    /** 创建人 */
    private String createdBy;
}
