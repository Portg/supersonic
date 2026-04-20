package com.tencent.supersonic.common.metrics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QueryTraceContextTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void openSetsMdcAndCloseRestoresPrevious() {
        MDC.put(QueryTraceContext.KEY, "pre-existing");
        try (QueryTraceContext.Scope s = QueryTraceContext.open()) {
            assertThat(MDC.get(QueryTraceContext.KEY)).isNotEqualTo("pre-existing");
            assertThat(s.traceId()).startsWith("q_");
        }
        assertThat(MDC.get(QueryTraceContext.KEY)).isEqualTo("pre-existing");
    }

    @Test
    void openSetsMdcAndCloseClearsWhenNoPrevious() {
        try (QueryTraceContext.Scope s = QueryTraceContext.open()) {
            assertThat(MDC.get(QueryTraceContext.KEY)).isEqualTo(s.traceId());
        }
        assertThat(MDC.get(QueryTraceContext.KEY)).isNull();
    }

    @Test
    void currentReturnsEmptyWhenUnset() {
        assertThat(QueryTraceContext.current()).isEmpty();
    }

    @Test
    void snapshotCarriesTraceAcrossBoundary() throws Exception {
        try (QueryTraceContext.Scope outer = QueryTraceContext.open()) {
            Map<String, String> snap = QueryTraceContext.snapshot();
            MDC.remove(QueryTraceContext.KEY);
            QueryTraceContext.restore(snap);
            assertThat(MDC.get(QueryTraceContext.KEY)).isEqualTo(outer.traceId());
        }
    }
}
