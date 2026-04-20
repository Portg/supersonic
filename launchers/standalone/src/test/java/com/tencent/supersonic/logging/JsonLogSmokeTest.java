package com.tencent.supersonic.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tencent.supersonic.common.metrics.QueryTraceContext;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class JsonLogSmokeTest {

    private static final Logger log = (Logger) LoggerFactory.getLogger(JsonLogSmokeTest.class);

    @Test
    void logLinesContainQueryTraceIdWhenScopeIsOpen() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        log.addAppender(appender);
        try (QueryTraceContext.Scope s = QueryTraceContext.open()) {
            log.info("smoke test: should have queryTraceId={}", s.traceId());
            assertThat(appender.list).hasSize(1);
            ILoggingEvent event = appender.list.get(0);
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getMDCPropertyMap()).containsEntry(QueryTraceContext.KEY, s.traceId());
        } finally {
            log.detachAppender(appender);
        }
    }
}
