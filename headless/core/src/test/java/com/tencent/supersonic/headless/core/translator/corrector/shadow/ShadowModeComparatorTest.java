package com.tencent.supersonic.headless.core.translator.corrector.shadow;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowModeComparatorTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setup() {
        logger = (Logger) LoggerFactory.getLogger(ShadowModeComparator.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void logsWarnWhenOldAndNewDiffer() {
        new ShadowModeComparator().compare("old: SELECT a FROM t WHERE x=1",
                "new: SELECT a FROM t WHERE x=1 AND region='APAC'", "alice");
        assertEquals(1, appender.list.size());
        assertEquals(Level.WARN, appender.list.get(0).getLevel());
        assertTrue(appender.list.get(0).getFormattedMessage().contains("shadow-diff"));
    }

    @Test
    void silentWhenIdentical() {
        new ShadowModeComparator().compare("SELECT 1", "SELECT 1", "alice");
        assertEquals(0, appender.list.size());
    }
}
