package com.tencent.supersonic.common.llm.pojo;

import com.tencent.supersonic.common.llm.LlmCallType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LlmUsageRecord {
    private Long tenantId;
    private String userId;
    private String provider;
    private String model;
    private LlmCallType callType;
    private int inputTokens;
    private int outputTokens;
    private int totalTokens;
    private long estimatedCostMicros;
    private String requestId;
    private String traceId;
    private Integer latencyMs;
    private boolean success;
    private String errorType;
    private Instant createdAt;
}
