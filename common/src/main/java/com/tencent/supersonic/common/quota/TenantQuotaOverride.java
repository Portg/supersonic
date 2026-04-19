package com.tencent.supersonic.common.quota;

import lombok.Data;

@Data
public class TenantQuotaOverride {
    private int jdbcConcurrent;
    private int llmConcurrent;
    private int acquireTimeoutMs;
    private boolean enabled = true;
}
