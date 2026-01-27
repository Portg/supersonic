package com.tencent.supersonic.auth.api.authentication.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * Tenant usage statistics entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantUsage {

    private Long id;

    private Long tenantId;

    private LocalDate usageDate;

    private Integer apiCalls;

    private Long tokensUsed;

    private Integer queryCount;

    private Long storageBytes;

    private Integer activeUsers;

    private Timestamp createdAt;

    private Timestamp updatedAt;
}
