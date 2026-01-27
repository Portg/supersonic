package com.tencent.supersonic.auth.api.authentication.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Refresh token configuration.
 */
@Data
@ConfigurationProperties(prefix = "s2.authentication.token.refresh")
public class RefreshTokenConfig {

    /**
     * Whether refresh tokens are enabled
     */
    private boolean enabled = true;

    /**
     * Refresh token timeout in milliseconds (default: 7 days)
     */
    private long timeout = 604800000;

    /**
     * Whether to rotate refresh tokens on each use (issue new refresh token when refreshing access
     * token)
     */
    private boolean rotation = true;

    /**
     * Whether to revoke all refresh tokens when password is changed
     */
    private boolean revokeOnPasswordChange = true;

    /**
     * Maximum number of refresh tokens per user (0 = unlimited)
     */
    private int maxTokensPerUser = 10;

    /**
     * Get the refresh token expiration time in seconds.
     */
    public long getTimeoutSeconds() {
        return timeout / 1000;
    }
}
