package com.tencent.supersonic.headless.server.metrics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.common.metrics.AbstractMeterBinder;
import com.tencent.supersonic.common.metrics.ReportMetricConstants;
import com.tencent.supersonic.headless.server.persistence.dataobject.ExportTaskDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryRecordDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ExportTaskMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryConfigMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryRecordMapper;
import com.tencent.supersonic.headless.server.pojo.DeliveryStatus;
import com.tencent.supersonic.headless.server.pojo.ExportTaskStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Unified MeterBinder for the template-report subsystem.
 *
 * <p>
 * Registers P0 gauges during {@link #doBindTo} and exposes helper methods for recording counters
 * and timers. All meters share the same {@code commonTags()} (module + origin) inherited from
 * {@link AbstractMeterBinder}, so Prometheus output is consistent across gauges, counters and
 * timers.
 * </p>
 */
@Component
public class TemplateReportMeterBinder extends AbstractMeterBinder {

    private static final MeterRegistry NOOP_REGISTRY = new SimpleMeterRegistry();

    private final ExportTaskMapper exportTaskMapper;
    private final ReportDeliveryRecordMapper deliveryRecordMapper;
    private final ReportDeliveryConfigMapper deliveryConfigMapper;

    public TemplateReportMeterBinder(ExportTaskMapper exportTaskMapper,
            ReportDeliveryRecordMapper deliveryRecordMapper,
            ReportDeliveryConfigMapper deliveryConfigMapper) {
        super(Tags.of(ReportMetricConstants.TagKeys.MODULE, ReportMetricConstants.MODULE));
        this.exportTaskMapper = exportTaskMapper;
        this.deliveryRecordMapper = deliveryRecordMapper;
        this.deliveryConfigMapper = deliveryConfigMapper;
    }

    @Override
    protected void doBindTo(MeterRegistry registry) {
        Gauge.builder(ReportMetricConstants.EXPORT_PENDING, exportTaskMapper,
                this::countExportPending).description("Count of pending export tasks")
                .tags(commonTags()).strongReference(true).register(registry);

        Gauge.builder(ReportMetricConstants.DELIVERY_RETRY_PENDING, deliveryRecordMapper,
                this::countRetryPending)
                .description("Count of failed delivery records waiting for retry")
                .tags(commonTags()).strongReference(true).register(registry);

        Gauge.builder(ReportMetricConstants.DELIVERY_CONFIG_DISABLED, deliveryConfigMapper,
                this::countDisabledConfigs)
                .description("Count of disabled delivery channel configs").tags(commonTags())
                .strongReference(true).register(registry);
    }

    // ---- counter / timer helpers ----

    public Timer.Sample startTimer() {
        MeterRegistry reg = getRegistry();
        return Timer.start(reg != null ? reg : NOOP_REGISTRY);
    }

    public void recordScheduleDispatch(String result) {
        if (!hasRegistry()) {
            return;
        }
        counter(ReportMetricConstants.SCHEDULE_DISPATCH_TOTAL, ReportMetricConstants.TagKeys.RESULT,
                result).increment();
    }

    public void recordScheduleRetryExhausted() {
        if (!hasRegistry()) {
            return;
        }
        counter(ReportMetricConstants.SCHEDULE_RETRY_EXHAUSTED_TOTAL).increment();
    }

    public void recordExecution(String result, String source, Timer.Sample sample) {
        if (!hasRegistry() || sample == null) {
            return;
        }
        counter(ReportMetricConstants.EXECUTION_TOTAL, ReportMetricConstants.TagKeys.RESULT, result,
                ReportMetricConstants.TagKeys.SOURCE, source).increment();
        sample.stop(timer(ReportMetricConstants.EXECUTION_DURATION,
                ReportMetricConstants.TagKeys.RESULT, result, ReportMetricConstants.TagKeys.SOURCE,
                source));
    }

    public void recordDelivery(String result, String type, long durationMs) {
        if (!hasRegistry()) {
            return;
        }
        counter(ReportMetricConstants.DELIVERY_TOTAL, ReportMetricConstants.TagKeys.RESULT, result,
                ReportMetricConstants.TagKeys.TYPE, type).increment();
        timer(ReportMetricConstants.DELIVERY_DURATION, ReportMetricConstants.TagKeys.RESULT, result,
                ReportMetricConstants.TagKeys.TYPE, type).record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordDeliveryRetry(String result, String type, long durationMs) {
        if (!hasRegistry()) {
            return;
        }
        counter(ReportMetricConstants.DELIVERY_RETRY_TOTAL, ReportMetricConstants.TagKeys.RESULT,
                result, ReportMetricConstants.TagKeys.TYPE, type).increment();
        timer(ReportMetricConstants.DELIVERY_DURATION, ReportMetricConstants.TagKeys.RESULT, result,
                ReportMetricConstants.TagKeys.TYPE, type, "retry", "true").record(durationMs,
                        TimeUnit.MILLISECONDS);
    }

    public void recordExport(String result, String format, long durationMs) {
        if (!hasRegistry()) {
            return;
        }
        counter(ReportMetricConstants.EXPORT_TOTAL, ReportMetricConstants.TagKeys.RESULT, result,
                ReportMetricConstants.TagKeys.FORMAT, format).increment();
        timer(ReportMetricConstants.EXPORT_DURATION, ReportMetricConstants.TagKeys.RESULT, result,
                ReportMetricConstants.TagKeys.FORMAT, format).record(durationMs,
                        TimeUnit.MILLISECONDS);
    }

    // ---- internal helpers ----

    private Counter counter(String name, String... tags) {
        return Counter.builder(name).tags(commonTags().and(tags)).register(getRegistry());
    }

    private Timer timer(String name, String... tags) {
        return Timer.builder(name).tags(commonTags().and(tags)).register(getRegistry());
    }

    // ---- gauge value suppliers ----

    private double countExportPending(ExportTaskMapper mapper) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<ExportTaskDO>()
                .eq(ExportTaskDO::getStatus, ExportTaskStatus.PENDING.name()));
        return Objects.requireNonNullElse(count, 0L);
    }

    private double countRetryPending(ReportDeliveryRecordMapper mapper) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<ReportDeliveryRecordDO>()
                .eq(ReportDeliveryRecordDO::getStatus, DeliveryStatus.FAILED.name())
                .isNotNull(ReportDeliveryRecordDO::getNextRetryAt));
        return Objects.requireNonNullElse(count, 0L);
    }

    private double countDisabledConfigs(ReportDeliveryConfigMapper mapper) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<ReportDeliveryConfigDO>()
                .eq(ReportDeliveryConfigDO::getEnabled, false));
        return Objects.requireNonNullElse(count, 0L);
    }
}
