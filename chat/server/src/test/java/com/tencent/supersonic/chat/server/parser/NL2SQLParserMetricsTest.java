package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants;
import com.tencent.supersonic.common.metrics.Nl2sqlMetrics;
import com.tencent.supersonic.common.metrics.TenantTagNormalizer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NL2SQLParserMetricsTest {

    @Test
    void stageTimerProducesSampleWithParserTag() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TenantTagNormalizer normalizer = new TenantTagNormalizer(List.of("acme"), 10, true);
        Nl2sqlMetrics metrics = new Nl2sqlMetrics(registry, normalizer);

        try (Nl2sqlMetrics.StageTimer t =
                metrics.startStage("rule_parse", "acme", "agent-1", "NL2SQLParser")) {
            // simulate rule parse stage
        }

        assertThat(registry.find(Nl2sqlMetricConstants.STAGE_DURATION).tag("stage", "rule_parse")
                .tag("parser_name", "NL2SQLParser").timer()).isNotNull();
    }
}
