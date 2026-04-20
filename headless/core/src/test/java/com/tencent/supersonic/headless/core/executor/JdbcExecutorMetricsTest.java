package com.tencent.supersonic.headless.core.executor;

import com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants;
import com.tencent.supersonic.common.metrics.Nl2sqlMetrics;
import com.tencent.supersonic.common.metrics.TenantTagNormalizer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcExecutorMetricsTest {

    @Test
    void recordDbPublishesTimerAndSummary() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Nl2sqlMetrics metrics = new Nl2sqlMetrics(new TenantTagNormalizer(List.of(), 10, true));
        metrics.bindTo(registry);

        metrics.recordDb("mysql", Duration.ofMillis(100), 2048,
                Nl2sqlMetricConstants.OUTCOME_SUCCESS, "acme");

        assertThat(registry.find(Nl2sqlMetricConstants.DB_DURATION).tag("db_type", "mysql").timer())
                .isNotNull();
        assertThat(registry.find(Nl2sqlMetricConstants.DB_ROWS_RETURNED).tag("db_type", "mysql")
                .summary().totalAmount()).isEqualTo(2048);
    }
}
