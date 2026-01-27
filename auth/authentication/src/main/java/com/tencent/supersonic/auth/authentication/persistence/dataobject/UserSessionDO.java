package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户会话跟踪数据对象 对应表: s2_user_session
 */
@Data
@TableName("s2_user_session")
public class UserSessionDO {

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 唯一会话标识 */
    private String sessionId;

    /** 用户ID */
    private Long userId;

    /** 认证方式: LOCAL, OAUTH */
    private String authMethod;

    /** OAuth提供者(如适用) */
    private String providerName;

    /** 会话创建时间 */
    private Date createdAt;

    /** 最后活动时间 */
    private Date lastActivityAt;

    /** 会话过期时间 */
    private Date expiresAt;

    /** IP地址 */
    private String ipAddress;

    /** 用户代理 */
    private String userAgent;

    /** 是否已撤销 */
    private Integer revoked;

    /** 撤销时间 */
    private Date revokedAt;

    /** 撤销原因 */
    private String revokedReason;

    /**
     * Check if this session is revoked.
     */
    public boolean isRevoked() {
        return revoked != null && revoked == 1;
    }

    /**
     * Check if this session is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.before(new Date());
    }

    /**
     * Check if this session is valid (not revoked and not expired).
     */
    public boolean isValid() {
        return !isRevoked() && !isExpired();
    }
}
