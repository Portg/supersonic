package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.llm.event.LlmUsageEvent;
import com.tencent.supersonic.common.llm.pojo.LlmUsageRecord;
import com.tencent.supersonic.common.llm.service.LlmUsageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LlmUsageEventIntegrationTest {

    @Test
    void flushPublishesSingleEventContainingAllBufferedRecords() {
        LlmUsageService service = mock(LlmUsageService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        CostEstimator estimator = (p, m, i, o) -> 0L;

        LlmUsageRecorder rec = new LlmUsageRecorder(service, publisher, estimator, 1000, 100);

        for (int i = 0; i < 5; i++) {
            rec.record(
                    LlmUsageRecord.builder().tenantId(1L).provider("OPEN_AI").model("gpt-4o-mini")
                            .callType(LlmCallType.NL2SQL).inputTokens(10).outputTokens(5)
                            .totalTokens(15).createdAt(Instant.now()).success(true).build());
        }
        rec.flushNow();

        ArgumentCaptor<LlmUsageEvent> captor = ArgumentCaptor.forClass(LlmUsageEvent.class);
        verify(publisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue().getRecords()).hasSize(5);
    }

    @Test
    void eventNotPublishedWhenBatchInsertFails() {
        LlmUsageService service = mock(LlmUsageService.class);
        doThrow(new RuntimeException("db down")).when(service).batchInsert(any());
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        CostEstimator estimator = (p, m, i, o) -> 0L;

        LlmUsageRecorder rec = new LlmUsageRecorder(service, publisher, estimator, 1000, 100);
        rec.record(LlmUsageRecord.builder().tenantId(1L).provider("OPEN_AI").model("gpt-4o-mini")
                .callType(LlmCallType.NL2SQL).inputTokens(10).outputTokens(5).totalTokens(15)
                .createdAt(Instant.now()).success(true).build());
        rec.flushNow();

        verify(publisher, never()).publishEvent(any(LlmUsageEvent.class));
    }
}
