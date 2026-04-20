package com.tencent.supersonic.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Nl2sqlMetricsTest {

    private SimpleMeterRegistry registry;
    private Nl2sqlMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        TenantTagNormalizer normalizer = new TenantTagNormalizer(List.of("acme"), 50, true);
        metrics = new Nl2sqlMetrics(registry, normalizer);
    }

    @Test
    void recordStagePublishesTimerWithTags() {
        metrics.recordStage("rule_parse", Duration.ofMillis(123),
                Nl2sqlMetricConstants.OUTCOME_SUCCESS, "acme", "agent-1", "NL2SQLParser");

        Timer t = registry.find(Nl2sqlMetricConstants.STAGE_DURATION).tag("stage", "rule_parse")
                .tag("outcome", "success").tag("tenant_id", "acme").tag("agent_id", "agent-1")
                .tag("parser_name", "NL2SQLParser").timer();
        assertThat(t).isNotNull();
        assertThat(t.count()).isEqualTo(1);
        assertThat(t.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(100);
    }

    @Test
    void recordStageNormalizesUnknownTenant() {
        metrics.recordStage("rule_parse", Duration.ofMillis(10),
                Nl2sqlMetricConstants.OUTCOME_SUCCESS, "some-new-tenant", "agent-1",
                "NL2SQLParser");

        assertThat(registry.find(Nl2sqlMetricConstants.STAGE_DURATION)
                .tag("tenant_id", "some-new-tenant").timer()).isNotNull();
    }

    @Test
    void recordLlmTokensPublishesCounterPerKind() {
        metrics.recordLlmTokens("gpt-4o", 300, 120);
        Counter prompt = registry.find(Nl2sqlMetricConstants.LLM_TOKENS_TOTAL)
                .tag("model", "gpt-4o").tag("kind", "prompt").counter();
        Counter completion = registry.find(Nl2sqlMetricConstants.LLM_TOKENS_TOTAL)
                .tag("model", "gpt-4o").tag("kind", "completion").counter();
        assertThat(prompt.count()).isEqualTo(300);
        assertThat(completion.count()).isEqualTo(120);
    }

    @Test
    void recordLlmTokensIgnoresNonPositiveValues() {
        metrics.recordLlmTokens("gpt-4o", 0, -1);

        assertThat(registry.find(Nl2sqlMetricConstants.LLM_TOKENS_TOTAL).meters()).isEmpty();
    }

    @Test
    void startStageReturnsAutoCloseableThatStopsTimer() {
        try (Nl2sqlMetrics.StageTimer t =
                metrics.startStage("mapper", "acme", "agent-1", "NL2SQLParser")) {
            t.markMapper("KeywordMapper");
            // simulate work
        }
        Timer timer = registry.find(Nl2sqlMetricConstants.STAGE_DURATION).tag("stage", "mapper")
                .tag("mapper_name", "KeywordMapper").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void mapperHitCounterIncrements() {
        metrics.recordMapperHit("KeywordMapper", true, "acme");
        Counter c = registry.find(Nl2sqlMetricConstants.MAPPER_HITS_TOTAL)
                .tag("mapper_name", "KeywordMapper").tag("hit", "true").counter();
        assertThat(c.count()).isEqualTo(1.0);
    }

    @Test
    void missingMeterRegistryIsNoop() {
        Nl2sqlMetrics noop = new Nl2sqlMetrics((io.micrometer.core.instrument.MeterRegistry) null,
                new TenantTagNormalizer(List.of(), 10, true));

        noop.recordStage("rule_parse", Duration.ofMillis(1), Nl2sqlMetricConstants.OUTCOME_SUCCESS,
                "acme", "agent-1", "NL2SQLParser");
        noop.recordLlmLatency("gpt-4o", Duration.ofMillis(1), Nl2sqlMetricConstants.OUTCOME_SUCCESS,
                "acme", "agent-1");
        noop.recordLlmTokens("gpt-4o", 1, 1);
        noop.recordMapperHit("KeywordMapper", true, "acme");
        noop.recordDb("mysql", Duration.ofMillis(1), 1, Nl2sqlMetricConstants.OUTCOME_SUCCESS,
                "acme");
        try (Nl2sqlMetrics.StageTimer ignored =
                noop.startStage("mapper", "acme", "agent-1", "NL2SQLParser")) {
            // no-op
        }

        assertThat(registry.getMeters()).isEmpty();
    }
}
