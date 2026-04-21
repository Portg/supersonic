package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportScheduleMapper;
import com.tencent.supersonic.headless.server.service.impl.ReportExecutionContextBuilder;
import com.tencent.supersonic.headless.server.service.impl.ReportExecutionOrchestrator;
import com.tencent.supersonic.headless.server.service.impl.ReportScheduleDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ReportScheduleDispatcherPropertyTest {

    @Test
    void dispatcherShouldHonorLegacyConcurrencyPropertyKey() {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                    "s2.report.schedule.max-concurrent-per-tenant=7");
            context.register(TestConfig.class);
            context.refresh();

            ReportScheduleDispatcher dispatcher = context.getBean(ReportScheduleDispatcher.class);
            assertEquals(7, ReflectionTestUtils.getField(dispatcher, "maxConcurrentPerTenant"));
        }
    }

    @Test
    void dispatcherShouldPreferNewConcurrencyPropertyKeyWhenBothPresent() {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                    "s2.report.schedule.max-concurrent-per-tenant=7",
                    "s2.report.max-concurrent-per-tenant=4");
            context.register(TestConfig.class);
            context.refresh();

            ReportScheduleDispatcher dispatcher = context.getBean(ReportScheduleDispatcher.class);
            assertEquals(4, ReflectionTestUtils.getField(dispatcher, "maxConcurrentPerTenant"));
        }
    }

    @Configuration
    static class TestConfig {

        @Bean
        static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        ReportScheduleDispatcher reportScheduleDispatcher() {
            return new ReportScheduleDispatcher(mock(ReportScheduleMapper.class),
                    mock(ReportExecutionMapper.class), mock(ReportExecutionOrchestrator.class),
                    mock(ReportExecutionContextBuilder.class));
        }
    }
}
