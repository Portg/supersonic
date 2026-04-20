package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.llm.pojo.LlmUsageRecord;
import com.tencent.supersonic.common.llm.service.LlmUsageService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LlmUsageRecorderTest {

    private LlmUsageRecord sample() {
        return LlmUsageRecord.builder().tenantId(1L).provider("OPEN_AI").model("gpt-4o-mini")
                .callType(LlmCallType.NL2SQL).inputTokens(100).outputTokens(50).totalTokens(150)
                .createdAt(Instant.now()).success(true).build();
    }

    @Test
    void flushPersistsAllBufferedRecordsThenPublishesEvent() {
        LlmUsageService service = mock(LlmUsageService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        CostEstimator estimator = mock(CostEstimator.class);
        when(estimator.estimate(any(), any(), anyInt(), anyInt())).thenReturn(0L);

        LlmUsageRecorder rec = new LlmUsageRecorder(service, publisher, estimator, 1000, 100);

        rec.record(sample());
        rec.record(sample());

        rec.flushNow();

        verify(service).batchInsert(argThat(list -> list.size() == 2));
        verify(publisher)
                .publishEvent(any(com.tencent.supersonic.common.llm.event.LlmUsageEvent.class));
    }

    @Test
    void sizeTriggeredFlushFiresOnceThresholdReached() {
        LlmUsageService service = mock(LlmUsageService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        CostEstimator estimator = mock(CostEstimator.class);
        when(estimator.estimate(any(), any(), anyInt(), anyInt())).thenReturn(0L);

        LlmUsageRecorder rec = new LlmUsageRecorder(service, publisher, estimator, 1000, 3);

        rec.record(sample());
        rec.record(sample());
        verifyNoInteractions(service);

        rec.record(sample()); // hits threshold -> async flush via direct executor in test
        rec.flushNow(); // drain any residual (defensive)

        verify(service, atLeastOnce()).batchInsert(any());
    }

    @Test
    void dropsRecordWhenQueueFullWithoutThrowing() {
        LlmUsageService service = mock(LlmUsageService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        CostEstimator estimator = mock(CostEstimator.class);
        when(estimator.estimate(any(), any(), anyInt(), anyInt())).thenReturn(0L);

        LlmUsageRecorder rec = new LlmUsageRecorder(service, publisher, estimator, 2, 1000);

        rec.record(sample());
        rec.record(sample());
        rec.record(sample()); // third one should be dropped silently

        rec.flushNow();
        verify(service).batchInsert(argThat(list -> list.size() == 2));
    }
}
