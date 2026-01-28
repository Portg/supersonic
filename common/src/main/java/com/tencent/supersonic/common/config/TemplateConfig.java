package com.tencent.supersonic.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "s2.template")
public class TemplateConfig {

    /**
     * Whether to auto-deploy builtin templates on startup.
     */
    private boolean autoDeploy = false;
}
