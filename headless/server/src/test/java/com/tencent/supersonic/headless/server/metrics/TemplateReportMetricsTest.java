package com.tencent.supersonic.headless.server.metrics;

import com.tencent.supersonic.common.metrics.ReportMetricConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class TemplateReportMetricsTest {

    @Test
    void unboundMetricsAreNoop() {
        TemplateReportMetrics metrics = new TemplateReportMetrics();
        Timer.Sample sample = metrics.startTimer();

        assertThatNoException().isThrownBy(() -> {
            metrics.recordScheduleDispatch("success");
            metrics.recordScheduleRetryExhausted();
            metrics.recordExecution("success", "schedule", sample);
            metrics.recordDelivery("success", "feishu", 12);
            metrics.recordDeliveryRetry("failed", "mail", 25);
            metrics.recordExport("success", "xlsx", 30);
        });
    }

    @Test
    void boundMetricsRecordCountersAndTimersWithCommonTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TemplateReportMetrics metrics = new TemplateReportMetrics();
        metrics.bindTo(registry);

        Timer.Sample executionSample = metrics.startTimer();
        metrics.recordScheduleDispatch("success");
        metrics.recordScheduleRetryExhausted();
        metrics.recordExecution("success", "schedule", executionSample);
        metrics.recordDelivery("success", "feishu", 12);
        metrics.recordDeliveryRetry("failed", "mail", 25);
        metrics.recordExport("success", "xlsx", 30);

        assertCounter(registry, ReportMetricConstants.SCHEDULE_DISPATCH_TOTAL, 1.0, "result",
                "success");
        assertCounter(registry, ReportMetricConstants.SCHEDULE_RETRY_EXHAUSTED_TOTAL, 1.0);
        assertCounter(registry, ReportMetricConstants.EXECUTION_TOTAL, 1.0, "result", "success",
                "source", "schedule");
        assertTimer(registry, ReportMetricConstants.EXECUTION_DURATION, "result", "success",
                "source", "schedule");
        assertCounter(registry, ReportMetricConstants.DELIVERY_TOTAL, 1.0, "result", "success",
                "type", "feishu");
        assertCounter(registry, ReportMetricConstants.DELIVERY_RETRY_TOTAL, 1.0, "result", "failed",
                "type", "mail");
        assertTimer(registry, ReportMetricConstants.DELIVERY_DURATION, "result", "failed", "type",
                "mail", "retry", "true");
        assertCounter(registry, ReportMetricConstants.EXPORT_TOTAL, 1.0, "result", "success",
                "format", "xlsx");
        assertTimer(registry, ReportMetricConstants.EXPORT_DURATION, "result", "success", "format",
                "xlsx");
    }

    @Test
    void recordExecutionWithNullSampleIsNoop() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TemplateReportMetrics metrics = new TemplateReportMetrics();
        metrics.bindTo(registry);

        metrics.recordExecution("success", "manual", null);

        assertThat(registry.find(ReportMetricConstants.EXECUTION_TOTAL).counter()).isNull();
        assertThat(registry.find(ReportMetricConstants.EXECUTION_DURATION).timer()).isNull();
    }

    private static void assertCounter(SimpleMeterRegistry registry, String name, double count,
            String... tags) {
        Counter counter = registry.find(name).tags(tags)
                .tag(ReportMetricConstants.TagKeys.MODULE, ReportMetricConstants.MODULE)
                .tag("origin", "TemplateReportMetrics").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(count);
    }

    private static void assertTimer(SimpleMeterRegistry registry, String name, String... tags) {
        Timer timer = registry.find(name).tags(tags)
                .tag(ReportMetricConstants.TagKeys.MODULE, ReportMetricConstants.MODULE)
                .tag("origin", "TemplateReportMetrics").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }
}
