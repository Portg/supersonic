package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants;
import com.tencent.supersonic.common.metrics.Nl2sqlMetrics;
import com.tencent.supersonic.common.metrics.TenantTagNormalizer;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LLMRequestServiceMetricsTest {

    @Test
    void recordingLlmMetricsAddsLatencyAndTokenCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Nl2sqlMetrics metrics =
                new Nl2sqlMetrics(registry, new TenantTagNormalizer(List.of(), 10, true));

        LLMResp resp = new LLMResp();
        resp.setModelName("gpt-4o");
        resp.setPromptTokens(300);
        resp.setCompletionTokens(120);

        metrics.recordLlmLatency(resp.getModelName(), java.time.Duration.ofMillis(500),
                Nl2sqlMetricConstants.OUTCOME_SUCCESS, "acme", "agent-1");
        metrics.recordLlmTokens(resp.getModelName(), resp.getPromptTokens(),
                resp.getCompletionTokens());

        assertThat(registry.find(Nl2sqlMetricConstants.LLM_DURATION).tag("model", "gpt-4o").timer())
                .isNotNull();
        assertThat(registry.find(Nl2sqlMetricConstants.LLM_TOKENS_TOTAL).tag("model", "gpt-4o")
                .tag("kind", "prompt").counter().count()).isEqualTo(300);
    }
}
