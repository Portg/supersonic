package com.tencent.supersonic.headless.core.translator.corrector.audit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyAuditLoggerTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setup() {
        logger = (Logger) LoggerFactory.getLogger(PolicyAuditLogger.AUDIT_LOGGER);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void emitsJsonLineWithExpectedFields() {
        PolicyAuditLogger pal = new PolicyAuditLogger();
        pal.log(new PolicyAuditEntry("P1", "alice", "row", "SELECT * FROM t",
                "SELECT * FROM t WHERE region='APAC'", "sha256:abc"));
        List<ILoggingEvent> events = appender.list;
        assertEquals(1, events.size());
        String msg = events.get(0).getFormattedMessage();
        assertTrue(msg.contains("\"policyId\":\"P1\""));
        assertTrue(msg.contains("\"user\":\"alice\""));
        assertTrue(msg.contains("\"policyType\":\"row\""));
        assertTrue(msg.contains("\"sqlDigest\":\"sha256:abc\""));
    }
}
