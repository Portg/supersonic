package com.tencent.supersonic.billing.server.listener;

import com.tencent.supersonic.auth.api.authentication.service.UsageTrackingService;
import com.tencent.supersonic.common.llm.event.LlmUsageEvent;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmUsageListener {

    private final UsageTrackingService usageTrackingService;

    @EventListener
    public void onLlmUsage(LlmUsageEvent event) {
        if (event.getRecords() == null || event.getRecords().isEmpty()) {
            return;
        }
        Map<Long, Long> perTenant = event.getRecords().stream().filter(r -> r.getTenantId() != null)
                .collect(Collectors.groupingBy(LlmUsageDO::getTenantId, Collectors
                        .summingLong(r -> r.getTotalTokens() == null ? 0 : r.getTotalTokens())));
        for (Map.Entry<Long, Long> e : perTenant.entrySet()) {
            try {
                usageTrackingService.recordTokenUsage(e.getKey(), e.getValue());
            } catch (Exception ex) {
                log.error("Failed recording token usage for tenant {}", e.getKey(), ex);
            }
        }
    }
}
