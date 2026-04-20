package com.tencent.supersonic.common.llm.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

@Data
@TableName("s2_llm_usage")
public class LlmUsageDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String userId;
    private String model;
    private String provider;
    private String callType;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Long estimatedCostMicros;
    private String requestId;
    private String traceId;
    private Integer latencyMs;
    private Boolean success;
    private String errorType;
    private Timestamp createdAt;
}
