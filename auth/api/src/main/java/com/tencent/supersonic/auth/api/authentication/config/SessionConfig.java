package com.tencent.supersonic.auth.api.authentication.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Session management configuration.
 */
@Data
@ConfigurationProperties(prefix = "s2.authentication.session")
public class SessionConfig {

    /**
     * Whether session management is enabled
     */
    private boolean enabled = true;

    /**
     * Session timeout in milliseconds (default: 24 hours)
     */
    private long timeout = 86400000;

    /**
     * Maximum concurrent sessions per user (0 = unlimited)
     */
    private int maxConcurrent = 5;

    /**
     * Session revocation settings
     */
    private RevocationConfig revocation = new RevocationConfig();

    @Data
    public static class RevocationConfig {
        /**
         * Whether session revocation is enabled
         */
        private boolean enabled = true;

        /**
         * Revoke all sessions when password is changed
         */
        private boolean revokeOnPasswordChange = true;
    }

    /**
     * Check if the number of concurrent sessions exceeds the limit.
     */
    public boolean exceedsMaxConcurrent(int currentCount) {
        return maxConcurrent > 0 && currentCount >= maxConcurrent;
    }
}
