package com.tencent.supersonic.common.quota;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "s2.tenant.quota")
public class TenantQuotaConfig {

    private boolean enabled = false;
    private Default defaultQuota = new Default();
    private Long fallbackTenantId = 1L;

    @Data
    public static class Default {
        private int jdbcConcurrent = 10;
        private int llmConcurrent = 5;
        private int acquireTimeoutMs = 2000;
    }
}
