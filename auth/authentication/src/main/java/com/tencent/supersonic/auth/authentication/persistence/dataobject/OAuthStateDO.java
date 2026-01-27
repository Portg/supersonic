package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * OAuth状态(CSRF和PKCE)数据对象 对应表: s2_oauth_state
 */
@Data
@TableName("s2_oauth_state")
public class OAuthStateDO {

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 状态参数 */
    private String state;

    /** OAuth提供者名称 */
    private String providerName;

    /** PKCE验证码 */
    private String codeVerifier;

    /** 重定向URI */
    private String redirectUri;

    /** OIDC随机数 */
    private String nonce;

    /** 租户ID */
    private Long tenantId;

    /** 创建时间 */
    private Date createdAt;

    /** 过期时间 */
    private Date expiresAt;

    /** 是否已使用 */
    private Integer used;
}
