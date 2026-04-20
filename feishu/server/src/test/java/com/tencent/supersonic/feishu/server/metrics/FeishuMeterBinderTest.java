package com.tencent.supersonic.feishu.server.metrics;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.tencent.supersonic.feishu.server.persistence.mapper.FeishuUserMappingMapper;
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

class FeishuMeterBinderTest {

    private static final String ORIGIN_VALUE = "FeishuMeterBinder";
    private static final String MODULE_KEY = "module";
    private static final String MODULE_VALUE = "feishu";

    @Test
    void unboundMetricsAreNoop() {
        FeishuMeterBinder binder = new FeishuMeterBinder(mock(FeishuUserMappingMapper.class));

        assertThatNoException().isThrownBy(() -> {
            binder.incrementRateLimitHit();
            binder.incrementExecutorRejection();
            binder.incrementMessageSent("text");
            binder.incrementMessageSendError("card");
            binder.stopQueryTimer(binder.startSample(), "TextHandler", "success");
        });
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void bindToRegistersGaugeAndPreBuiltCounters() {
        FeishuUserMappingMapper mapper = mock(FeishuUserMappingMapper.class);
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(4L);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        FeishuMeterBinder binder = new FeishuMeterBinder(mapper);

        binder.bindTo(registry);

        Gauge pending = registry.find("feishu.mapping.pending").tag(MODULE_KEY, MODULE_VALUE)
                .tag("origin", ORIGIN_VALUE).gauge();
        assertThat(pending).isNotNull();
        assertThat(pending.value()).isEqualTo(4.0);

        Counter rateLimit = registry.find("feishu.rate_limit.hits").tag(MODULE_KEY, MODULE_VALUE)
                .tag("origin", ORIGIN_VALUE).counter();
        Counter rejection = registry.find("feishu.executor.rejections")
                .tag(MODULE_KEY, MODULE_VALUE).tag("origin", ORIGIN_VALUE).counter();
        assertThat(rateLimit).isNotNull();
        assertThat(rejection).isNotNull();
    }

    @Test
    void counterHelpersIncrementWithCommonTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        FeishuMeterBinder binder = new FeishuMeterBinder(mock(FeishuUserMappingMapper.class));
        binder.bindTo(registry);

        binder.incrementRateLimitHit();
        binder.incrementExecutorRejection();
        binder.incrementMessageSent("text");
        binder.incrementMessageSendError("card");

        assertThat(registry.find("feishu.rate_limit.hits").tag(MODULE_KEY, MODULE_VALUE)
                .tag("origin", ORIGIN_VALUE).counter().count()).isEqualTo(1.0);
        assertThat(registry.find("feishu.executor.rejections").tag(MODULE_KEY, MODULE_VALUE)
                .tag("origin", ORIGIN_VALUE).counter().count()).isEqualTo(1.0);
        assertThat(registry.find("feishu.message.sent").tag(MODULE_KEY, MODULE_VALUE)
                .tag("origin", ORIGIN_VALUE).tag("type", "text").counter().count()).isEqualTo(1.0);
        assertThat(registry.find("feishu.message.send.errors").tag(MODULE_KEY, MODULE_VALUE)
                .tag("origin", ORIGIN_VALUE).tag("type", "card").counter().count()).isEqualTo(1.0);
    }

    @Test
    void queryTimerRecordsDurationWithHandlerAndStatus() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        FeishuMeterBinder binder = new FeishuMeterBinder(mock(FeishuUserMappingMapper.class));
        binder.bindTo(registry);

        Timer.Sample sample = binder.startSample();
        assertThat(sample).isNotNull();
        binder.stopQueryTimer(sample, "TextHandler", "success");

        Timer timer = registry.find("feishu.query.duration").tag(MODULE_KEY, MODULE_VALUE)
                .tag("origin", ORIGIN_VALUE).tag("handler", "TextHandler").tag("status", "success")
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }
}
