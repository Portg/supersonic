package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.llm.persistence.dataobject.LlmPricingDO;
import com.tencent.supersonic.common.llm.persistence.mapper.LlmPricingDOMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CostEstimatorTest {

    @Test
    void estimateReturns0AndWarnsWhenPricingMissing() {
        LlmPricingDOMapper mapper = mock(LlmPricingDOMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());

        CostEstimatorImpl est = new CostEstimatorImpl(mapper);
        long cost = est.estimate("OPEN_AI", "gpt-unknown", 1000, 500);

        assertThat(cost).isZero();
    }

    @Test
    void estimateComputesCostFromInputAndOutputPricing() {
        LlmPricingDOMapper mapper = mock(LlmPricingDOMapper.class);
        LlmPricingDO p = new LlmPricingDO();
        p.setProvider("OPEN_AI");
        p.setModel("gpt-4o-mini");
        p.setInputPricePer1kMicros(150L); // $0.00015 per 1k in
        p.setOutputPricePer1kMicros(600L); // $0.00060 per 1k out
        when(mapper.selectList(any())).thenReturn(List.of(p));

        CostEstimatorImpl est = new CostEstimatorImpl(mapper);

        long cost = est.estimate("OPEN_AI", "gpt-4o-mini", 2000, 1000);
        // 2 * 150 + 1 * 600 = 900 micro-USD
        assertThat(cost).isEqualTo(900L);
    }

    @Test
    void pricingCachedAfterFirstLookup() {
        LlmPricingDOMapper mapper = mock(LlmPricingDOMapper.class);
        LlmPricingDO p = new LlmPricingDO();
        p.setProvider("OPEN_AI");
        p.setModel("gpt-4o-mini");
        p.setInputPricePer1kMicros(150L);
        p.setOutputPricePer1kMicros(600L);
        when(mapper.selectList(any())).thenReturn(List.of(p));

        CostEstimatorImpl est = new CostEstimatorImpl(mapper);

        est.estimate("OPEN_AI", "gpt-4o-mini", 100, 100);
        est.estimate("OPEN_AI", "gpt-4o-mini", 100, 100);
        est.estimate("OPEN_AI", "gpt-4o-mini", 100, 100);

        verify(mapper, times(1)).selectList(any());
    }

    @Test
    void refreshInvalidatesSpecificPricingEntry() {
        LlmPricingDOMapper mapper = mock(LlmPricingDOMapper.class);
        LlmPricingDO first = new LlmPricingDO();
        first.setInputPricePer1kMicros(100L);
        first.setOutputPricePer1kMicros(100L);
        LlmPricingDO second = new LlmPricingDO();
        second.setInputPricePer1kMicros(200L);
        second.setOutputPricePer1kMicros(200L);
        when(mapper.selectList(any())).thenReturn(List.of(first), List.of(second));

        CostEstimatorImpl est = new CostEstimatorImpl(mapper);

        assertThat(est.estimate("OPEN_AI", "gpt-4o-mini", 1000, 0)).isEqualTo(100L);
        est.refresh("OPEN_AI", "gpt-4o-mini");
        assertThat(est.estimate("OPEN_AI", "gpt-4o-mini", 1000, 0)).isEqualTo(200L);
        verify(mapper, times(2)).selectList(any());
    }
}
