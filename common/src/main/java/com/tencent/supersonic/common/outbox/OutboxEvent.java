package com.tencent.supersonic.common.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("s2_outbox")
public class OutboxEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String aggregateType;

    private String aggregateId;

    private String eventType;

    private String payloadJson;

    private Long tenantId;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    private String processingNode;

    private Integer attempts;

    private String lastError;
}
