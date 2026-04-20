package com.tencent.supersonic.billing.server.listener;

import com.tencent.supersonic.auth.api.authentication.service.UsageTrackingService;
import com.tencent.supersonic.common.llm.event.LlmUsageEvent;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class LlmUsageListenerTest {

    private LlmUsageDO dao(Long tenantId, int tokens) {
        LlmUsageDO d = new LlmUsageDO();
        d.setTenantId(tenantId);
        d.setTotalTokens(tokens);
        return d;
    }

    @Test
    void aggregatesByTenantAndInvokesRecordTokenUsageOncePerTenant() {
        UsageTrackingService usageService = mock(UsageTrackingService.class);
        LlmUsageListener listener = new LlmUsageListener(usageService);

        listener.onLlmUsage(
                new LlmUsageEvent(this, List.of(dao(1L, 100), dao(1L, 50), dao(2L, 200))));

        verify(usageService).recordTokenUsage(1L, 150L);
        verify(usageService).recordTokenUsage(2L, 200L);
        verifyNoMoreInteractions(usageService);
    }

    @Test
    void skipsRowsWithNullTenantId() {
        UsageTrackingService usageService = mock(UsageTrackingService.class);
        LlmUsageListener listener = new LlmUsageListener(usageService);

        listener.onLlmUsage(new LlmUsageEvent(this, List.of(dao(null, 100), dao(1L, 50))));

        verify(usageService).recordTokenUsage(1L, 50L);
        verifyNoMoreInteractions(usageService);
    }
}
