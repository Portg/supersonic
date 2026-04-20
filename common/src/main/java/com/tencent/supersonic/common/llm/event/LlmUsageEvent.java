package com.tencent.supersonic.common.llm.event;

import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class LlmUsageEvent extends ApplicationEvent {
    private final List<LlmUsageDO> records;

    public LlmUsageEvent(Object source, List<LlmUsageDO> records) {
        super(source);
        this.records = records;
    }
}
