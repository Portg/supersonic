package com.tencent.supersonic.headless.server.metrics;

import com.tencent.supersonic.common.metrics.AbstractMeterBinder;
import com.tencent.supersonic.common.metrics.ReportMetricConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Helper methods for template-report counters and timers. Extends {@link AbstractMeterBinder} so
 * that the registry is injected via {@code bindTo()} rather than constructor injection, keeping the
 * bean available even before Micrometer auto-configuration completes.
 */
@Component
public class TemplateReportMetrics extends AbstractMeterBinder {

    private static final MeterRegistry NOOP_REGISTRY = new SimpleMeterRegistry();

    @Override
    protected void doBindTo(MeterRegistry registry) {
        // Dynamic metrics only — no static registrations at bind time.
    }

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

    private Counter counter(String name, String... tags) {
        return Counter.builder(name).tags(withModule(tags)).register(getRegistry());
    }

    private Timer timer(String name, String... tags) {
        return Timer.builder(name).tags(withModule(tags)).register(getRegistry());
    }

    private String[] withModule(String... tags) {
        String[] merged = new String[tags.length + 2];
        merged[0] = ReportMetricConstants.TagKeys.MODULE;
        merged[1] = ReportMetricConstants.MODULE;
        System.arraycopy(tags, 0, merged, 2, tags.length);
        return merged;
    }
}
