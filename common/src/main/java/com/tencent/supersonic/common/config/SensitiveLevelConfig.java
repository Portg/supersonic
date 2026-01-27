package com.tencent.supersonic.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for sensitive level permission control. Controls which sensitivity levels require
 * explicit authorization.
 */
@Data
@Component
@ConfigurationProperties(prefix = "s2.permission.sensitive")
public class SensitiveLevelConfig {

    /**
     * Whether MID sensitive level requires authorization. If true, MID level fields will be treated
     * like HIGH level (require explicit auth). If false, MID level fields are accessible like LOW
     * level (no auth required). Default: false (backward compatible)
     */
    private boolean midLevelRequireAuth = false;

    /**
     * Whether to log access to MID sensitive level fields. Useful for audit purposes even when auth
     * is not required. Default: true
     */
    private boolean logMidLevelAccess = true;
}
