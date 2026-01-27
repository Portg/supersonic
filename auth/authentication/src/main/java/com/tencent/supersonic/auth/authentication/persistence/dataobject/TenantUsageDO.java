package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * Data object for tenant usage table.
 */
@Data
@TableName("s2_tenant_usage")
public class TenantUsageDO {

    @TableId(type = IdType.AUTO)
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
