package com.tencent.supersonic.feishu.server.metrics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.common.metrics.AbstractMeterBinder;
import com.tencent.supersonic.feishu.server.persistence.dataobject.FeishuUserMappingDO;
import com.tencent.supersonic.feishu.server.persistence.mapper.FeishuUserMappingMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Unified MeterBinder for the Feishu module.
 *
 * <p>
 * Registers gauges, pre-creates counters for rate-limiting / rejection, and provides timer/counter
 * helpers used by both {@code FeishuBotService} and the AOP aspect ({@code FeishuMetricsAspect}).
 * </p>
 */
@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
@Component
public class FeishuMeterBinder extends AbstractMeterBinder {

    private final FeishuUserMappingMapper userMappingMapper;
    private volatile MeterRegistry registry;
    private Counter rateLimitCounter;
    private Counter rejectionCounter;

    public FeishuMeterBinder(FeishuUserMappingMapper userMappingMapper) {
        super(Tags.of("module", "feishu"));
        this.userMappingMapper = userMappingMapper;
    }

    @Override
    protected void doBindTo(MeterRegistry registry) {
        this.registry = registry;
        rateLimitCounter =
                Counter.builder("feishu.rate_limit.hits").tags(commonTags()).register(registry);
        rejectionCounter =
                Counter.builder("feishu.executor.rejections").tags(commonTags()).register(registry);
        Gauge.builder("feishu.mapping.pending", userMappingMapper, this::countPending)
                .tags(commonTags()).strongReference(true).register(registry);
    }

    // ---- direct counter helpers (called by FeishuBotService) ----

    public void incrementRateLimitHit() {
        if (rateLimitCounter != null) {
            rateLimitCounter.increment();
        }
    }

    public void incrementExecutorRejection() {
        if (rejectionCounter != null) {
            rejectionCounter.increment();
        }
    }

    // ---- timer / counter helpers (called by FeishuMetricsAspect) ----

    public Timer.Sample startSample() {
        return registry != null ? Timer.start(registry) : null;
    }

    public void stopQueryTimer(Timer.Sample sample, String handler, String status) {
        if (sample != null && registry != null) {
            sample.stop(Timer.builder("feishu.query.duration").tags(commonTags())
                    .tag("handler", handler).tag("status", status).register(registry));
        }
    }

    public void incrementMessageSent(String type) {
        if (registry != null) {
            Counter.builder("feishu.message.sent").tags(commonTags()).tag("type", type)
                    .register(registry).increment();
        }
    }

    public void incrementMessageSendError(String type) {
        if (registry != null) {
            Counter.builder("feishu.message.send.errors").tags(commonTags()).tag("type", type)
                    .register(registry).increment();
        }
    }

    private double countPending(FeishuUserMappingMapper mapper) {
        return mapper.selectCount(new LambdaQueryWrapper<FeishuUserMappingDO>()
                .eq(FeishuUserMappingDO::getStatus, 0));
    }
}
