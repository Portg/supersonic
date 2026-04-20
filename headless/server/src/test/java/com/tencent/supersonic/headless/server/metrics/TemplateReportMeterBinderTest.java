package com.tencent.supersonic.headless.server.metrics;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.tencent.supersonic.common.metrics.ReportMetricConstants;
import com.tencent.supersonic.headless.server.persistence.mapper.ExportTaskMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryConfigMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryRecordMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemplateReportMeterBinderTest {

    private static final String ORIGIN_VALUE = "TemplateReportMeterBinder";

    @Test
    void unboundMetricsAreNoop() {
        TemplateReportMeterBinder metrics = buildBinder(0L, 0L, 0L);
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
        TemplateReportMeterBinder metrics = buildBinder(0L, 0L, 0L);
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
        TemplateReportMeterBinder metrics = buildBinder(0L, 0L, 0L);
        metrics.bindTo(registry);

        metrics.recordExecution("success", "manual", null);

        assertThat(registry.find(ReportMetricConstants.EXECUTION_TOTAL).counter()).isNull();
        assertThat(registry.find(ReportMetricConstants.EXECUTION_DURATION).timer()).isNull();
    }

    @Test
    void gaugesReflectMapperCounts() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TemplateReportMeterBinder metrics = buildBinder(7L, 3L, 2L);
        metrics.bindTo(registry);

        assertGauge(registry, ReportMetricConstants.EXPORT_PENDING, 7.0);
        assertGauge(registry, ReportMetricConstants.DELIVERY_RETRY_PENDING, 3.0);
        assertGauge(registry, ReportMetricConstants.DELIVERY_CONFIG_DISABLED, 2.0);
    }

    @Test
    void gaugesTolerateNullMapperCount() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TemplateReportMeterBinder metrics = buildBinder(null, null, null);
        metrics.bindTo(registry);

        assertGauge(registry, ReportMetricConstants.EXPORT_PENDING, 0.0);
        assertGauge(registry, ReportMetricConstants.DELIVERY_RETRY_PENDING, 0.0);
        assertGauge(registry, ReportMetricConstants.DELIVERY_CONFIG_DISABLED, 0.0);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TemplateReportMeterBinder buildBinder(Long exportPending, Long retryPending,
            Long disabledConfigs) {
        ExportTaskMapper exportMapper = mock(ExportTaskMapper.class);
        ReportDeliveryRecordMapper recordMapper = mock(ReportDeliveryRecordMapper.class);
        ReportDeliveryConfigMapper configMapper = mock(ReportDeliveryConfigMapper.class);
        when(exportMapper.selectCount(any(Wrapper.class))).thenReturn(exportPending);
        when(recordMapper.selectCount(any(Wrapper.class))).thenReturn(retryPending);
        when(configMapper.selectCount(any(Wrapper.class))).thenReturn(disabledConfigs);
        return new TemplateReportMeterBinder(exportMapper, recordMapper, configMapper);
    }

    private static void assertCounter(SimpleMeterRegistry registry, String name, double count,
            String... tags) {
        Counter counter = registry.find(name).tags(tags)
                .tag(ReportMetricConstants.TagKeys.MODULE, ReportMetricConstants.MODULE)
                .tag("origin", ORIGIN_VALUE).counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(count);
    }

    private static void assertTimer(SimpleMeterRegistry registry, String name, String... tags) {
        Timer timer = registry.find(name).tags(tags)
                .tag(ReportMetricConstants.TagKeys.MODULE, ReportMetricConstants.MODULE)
                .tag("origin", ORIGIN_VALUE).timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    private static void assertGauge(SimpleMeterRegistry registry, String name, double expected) {
        Gauge gauge = registry.find(name)
                .tag(ReportMetricConstants.TagKeys.MODULE, ReportMetricConstants.MODULE)
                .tag("origin", ORIGIN_VALUE).gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(expected);
    }
}
