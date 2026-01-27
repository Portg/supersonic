package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * JWT刷新令牌数据对象 对应表: s2_refresh_token
 */
@Data
@TableName("s2_refresh_token")
public class RefreshTokenDO {

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 令牌SHA-256哈希 */
    private String tokenHash;

    /** 用户ID */
    private Long userId;

    /** 关联会话ID */
    private Long sessionId;

    /** 签发时间 */
    private Date issuedAt;

    /** 过期时间 */
    private Date expiresAt;

    /** 是否已撤销 */
    private Integer revoked;

    /** 撤销时间 */
    private Date revokedAt;

    /** 设备信息 */
    private String deviceInfo;

    /** IP地址 */
    private String ipAddress;

    /**
     * Check if this refresh token is revoked.
     */
    public boolean isRevoked() {
        return revoked != null && revoked == 1;
    }

    /**
     * Check if this refresh token is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.before(new Date());
    }

    /**
     * Check if this refresh token is valid (not revoked and not expired).
     */
    public boolean isValid() {
        return !isRevoked() && !isExpired();
    }
}
