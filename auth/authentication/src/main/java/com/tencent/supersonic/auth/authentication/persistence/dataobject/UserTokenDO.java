package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户令牌信息数据对象 对应表: s2_user_token
 */
@Data
@TableName("s2_user_token")
public class UserTokenDO {

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 令牌名称 */
    private String name;

    /** 用户名 */
    private String userName;

    /** 过期时间(时间戳) */
    private Long expireTime;

    /** 令牌值 */
    private String token;

    /** 盐值 */
    private String salt;

    /** 租户ID */
    private Long tenantId;

    /** 创建时间 */
    private Date createTime;

    /** 创建人 */
    private String createBy;

    /** 更新时间 */
    private Date updateTime;

    /** 更新人 */
    private String updateBy;

    /** 过期日期时间 */
    private Date expireDateTime;
}
