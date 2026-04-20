package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants;
import com.tencent.supersonic.common.metrics.Nl2sqlMetrics;
import com.tencent.supersonic.common.metrics.TenantTagNormalizer;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorrectorMetricsDecoratorTest {

    @Test
    void wrapsDelegateAndEmitsStageMetricWithCorrectorTag() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Nl2sqlMetrics metrics = new Nl2sqlMetrics(new TenantTagNormalizer(List.of(), 10, true));
        metrics.bindTo(registry);

        SemanticCorrector delegate = (ctx, info) -> {
            /* no-op */ };
        CorrectorMetricsDecorator decorated =
                new CorrectorMetricsDecorator(delegate, "TimeCorrector", metrics);

        decorated.correct(new ChatQueryContext(), new SemanticParseInfo());

        assertThat(registry.find(Nl2sqlMetricConstants.STAGE_DURATION).tag("stage", "corrector")
                .tag("corrector_name", "TimeCorrector").timer()).isNotNull();
    }

    @Test
    void recordsErrorOutcomeWhenDelegateThrows() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Nl2sqlMetrics metrics = new Nl2sqlMetrics(new TenantTagNormalizer(List.of(), 10, true));
        metrics.bindTo(registry);

        SemanticCorrector bad = (ctx, info) -> {
            throw new IllegalStateException("boom");
        };
        CorrectorMetricsDecorator decorated =
                new CorrectorMetricsDecorator(bad, "WhereCorrector", metrics);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> decorated.correct(new ChatQueryContext(), new SemanticParseInfo()));

        assertThat(registry.find(Nl2sqlMetricConstants.STAGE_OUTCOME_TOTAL).tag("outcome", "error")
                .counter().count()).isEqualTo(1.0);
    }
}
