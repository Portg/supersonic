package com.tencent.supersonic.common.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("s2_outbox_dead")
public class OutboxDeadEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long originalId;

    private String aggregateType;

    private String aggregateId;

    private String eventType;

    private String payloadJson;

    private Long tenantId;

    private String failureReason;

    private Integer attempts;

    private LocalDateTime createdAt;

    private LocalDateTime diedAt;
}
