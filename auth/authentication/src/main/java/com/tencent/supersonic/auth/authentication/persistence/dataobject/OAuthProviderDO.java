package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * OAuth提供者配置数据对象 对应表: s2_oauth_provider
 */
@Data
@TableName("s2_oauth_provider")
public class OAuthProviderDO {

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提供者名称 */
    private String name;

    /** 提供者类型: GOOGLE, AZURE_AD, KEYCLOAK, GENERIC_OIDC */
    private String type;

    /** OAuth客户端ID */
    private String clientId;

    /** OAuth客户端密钥 */
    private String clientSecret;

    /** 授权端点 */
    private String authorizationUri;

    /** Token端点 */
    private String tokenUri;

    /** 用户信息端点 */
    private String userInfoUri;

    /** JWKS端点 */
    private String jwksUri;

    /** Token签发者 */
    private String issuer;

    /** OAuth范围 */
    private String scopes;

    /** 是否启用PKCE */
    private Integer pkceEnabled;

    /** 附加参数(JSON) */
    private String additionalParams;

    /** 是否启用 */
    private Integer enabled;

    /** 租户ID */
    private Long tenantId;

    /** 创建时间 */
    private Date createdAt;

    /** 更新时间 */
    private Date updatedAt;
}
