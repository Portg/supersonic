package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data object for tenant quota table (s2_tenant_quota). Stores per-tenant concurrency and rate
 * limits.
 */
@Data
@TableName("s2_tenant_quota")
public class TenantQuotaDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Integer jdbcConcurrent;

    private Integer llmConcurrent;

    private Long monthlyQueryCount;

    private Integer acquireTimeoutMs;

    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
