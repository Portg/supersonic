package com.tencent.supersonic.auth.api.quota.request;

import lombok.Data;

@Data
public class TenantQuotaReq {
    private Long tenantId;
    private Integer jdbcConcurrent;
    private Integer llmConcurrent;
    private Long monthlyQueryCount;
    private Integer acquireTimeoutMs;
    private Boolean enabled;
}
