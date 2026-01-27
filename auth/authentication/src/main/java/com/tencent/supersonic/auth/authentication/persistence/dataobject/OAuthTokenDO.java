package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户OAuth令牌数据对象 对应表: s2_oauth_token
 */
@Data
@TableName("s2_oauth_token")
public class OAuthTokenDO {

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** OAuth提供者名称 */
    private String providerName;

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** ID令牌 */
    private String idToken;

    /** 令牌类型 */
    private String tokenType;

    /** 访问令牌过期时间 */
    private Date expiresAt;

    /** 刷新令牌过期时间 */
    private Date refreshExpiresAt;

    /** 授权范围 */
    private String scopes;

    /** 创建时间 */
    private Date createdAt;

    /** 更新时间 */
    private Date updatedAt;
}
