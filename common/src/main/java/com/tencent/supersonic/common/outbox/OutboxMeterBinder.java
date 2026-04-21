package com.tencent.supersonic.common.outbox;

import com.tencent.supersonic.common.metrics.AbstractMeterBinder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Exposes:
 * <ul>
 * <li>{@code s2_outbox_unprocessed_count} — gauge of NULL processed_at rows</li>
 * <li>{@code s2_outbox_lag_seconds} — age in seconds of the oldest unprocessed row</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "s2.outbox", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class OutboxMeterBinder extends AbstractMeterBinder {

    private final OutboxEventService service;

    public OutboxMeterBinder(OutboxEventService service) {
        super(Tags.of("subsystem", "outbox"));
        this.service = service;
    }

    @Override
    protected void doBindTo(MeterRegistry registry) {
        registry.gauge("s2_outbox_unprocessed_count", commonTags(), this,
                OutboxMeterBinder::unprocessed);
        registry.gauge("s2_outbox_lag_seconds", commonTags(), this, OutboxMeterBinder::lagSeconds);
    }

    double unprocessed() {
        try {
            return service.getBaseMapper().countUnprocessed();
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    double lagSeconds() {
        try {
            LocalDateTime oldest = service.getBaseMapper().oldestUnprocessedCreatedAt();
            if (oldest == null) {
                return 0.0;
            }
            return Duration.between(oldest, LocalDateTime.now()).toSeconds();
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}
