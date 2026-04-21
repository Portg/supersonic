package com.tencent.supersonic.common.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OutboxTestConfig.class)
@TestPropertySource(properties = {"s2.outbox.enabled=true", "s2.outbox.retention-days=7",
                "spring.datasource.url=jdbc:h2:mem:outbox-retention;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.flyway.enabled=false",
                "spring.sql.init.schema-locations=classpath:schema-h2.sql",
                "spring.sql.init.mode=always"})
class OutboxRetentionTaskTest {

    @Autowired
    private OutboxEventService service;
    @Autowired
    private OutboxRetentionTask task;

    @BeforeEach
    void setUp() {
        service.getBaseMapper().delete(null);
    }

    @Test
    void deletesOnlyProcessedRowsOlderThanRetention() {
        OutboxEvent old = row(LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(10));
        OutboxEvent fresh = row(LocalDateTime.now(), LocalDateTime.now());
        OutboxEvent unprocessedOld = row(LocalDateTime.now().minusDays(10), null);
        service.saveBatch(List.of(old, fresh, unprocessedOld));

        task.runCleanup();

        assertThat(service.count()).isEqualTo(2); // fresh + unprocessedOld survive
    }

    private static OutboxEvent row(LocalDateTime created, LocalDateTime processed) {
        OutboxEvent e = new OutboxEvent();
        e.setAggregateType("X");
        e.setEventType("X");
        e.setPayloadJson("{}");
        e.setTenantId(1L);
        e.setCreatedAt(created);
        e.setProcessedAt(processed);
        e.setAttempts(0);
        return e;
    }
}
