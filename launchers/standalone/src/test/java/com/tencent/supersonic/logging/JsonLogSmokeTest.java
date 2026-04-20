package com.tencent.supersonic.logging;

import com.tencent.supersonic.common.metrics.QueryTraceContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JsonLogSmokeTest {

    private static final Logger log = LoggerFactory.getLogger(JsonLogSmokeTest.class);

    @Test
    void logLinesContainQueryTraceIdWhenScopeIsOpen() {
        try (QueryTraceContext.Scope s = QueryTraceContext.open()) {
            log.info("smoke test: should have queryTraceId={}", s.traceId());
        }
        // Visual check: verify MDC is set and cleared correctly
        // No assertions — this is a logging smoke test (visual verification)
    }
}
