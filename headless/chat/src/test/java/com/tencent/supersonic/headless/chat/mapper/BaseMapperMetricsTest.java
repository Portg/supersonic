package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants;
import com.tencent.supersonic.common.metrics.Nl2sqlMetrics;
import com.tencent.supersonic.common.metrics.TenantTagNormalizer;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseMapperMetricsTest {

    private SimpleMeterRegistry registry;
    private Nl2sqlMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new Nl2sqlMetrics(new TenantTagNormalizer(List.of(), 50, true));
        metrics.bindTo(registry);
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBean(Nl2sqlMetrics.class)).thenReturn(metrics);
        new ContextUtils().setApplicationContext(ctx);
    }

    @Test
    void mapperExecutionEmitsStageMetricWithMapperTag() {
        BaseMapper mapper = new BaseMapper() {
            @Override
            public void doMap(ChatQueryContext c) { /* no-op */ }
        };
        ChatQueryContext ctx = new ChatQueryContext();

        mapper.map(ctx);

        assertThat(registry.find(Nl2sqlMetricConstants.STAGE_DURATION).tag("stage", "mapper")
                .tag("mapper_name", mapper.getClass().getSimpleName()).timer()).isNotNull();
    }
}
