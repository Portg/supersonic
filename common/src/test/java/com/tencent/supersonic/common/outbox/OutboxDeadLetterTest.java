package com.tencent.supersonic.common.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OutboxTestConfig.class)
@TestPropertySource(properties = {"s2.outbox.enabled=true",
                "spring.datasource.url=jdbc:h2:mem:outbox-dead;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.flyway.enabled=false",
                "spring.sql.init.schema-locations=classpath:schema-h2.sql",
                "spring.sql.init.mode=always"})
class OutboxDeadLetterTest {

    @Autowired
    private OutboxEventService service;
    @Autowired
    private OutboxDeadMapper deadMapper;
    @Autowired
    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        service.getBaseMapper().delete(null);
        deadMapper.delete(null);
    }

    @Test
    void undeserializableRow_moveToDeadTable() {
        OutboxEvent bad = new OutboxEvent();
        bad.setAggregateType("X");
        bad.setEventType("com.example.DoesNotExist"); // ClassNotFoundException
        bad.setPayloadJson("{}");
        bad.setTenantId(1L);
        bad.setCreatedAt(LocalDateTime.now());
        bad.setAttempts(0);
        service.save(bad);

        relay.pollOnce();

        // Original row marked processed, NOT re-selected
        assertThat(service.getBaseMapper().countUnprocessed()).isZero();
        // Dead row exists referencing originalId
        assertThat(deadMapper.selectCount(null)).isEqualTo(1L);
        OutboxDeadEvent dead = deadMapper.selectList(null).get(0);
        assertThat(dead.getOriginalId()).isEqualTo(bad.getId());
        assertThat(dead.getFailureReason()).contains("DoesNotExist");
    }

    @Test
    void malformedJson_moveToDeadTable() {
        OutboxEvent bad = new OutboxEvent();
        bad.setAggregateType("X");
        bad.setEventType(OutboxPublisherTest.ToyEvent.class.getName());
        bad.setPayloadJson("{not json");
        bad.setTenantId(1L);
        bad.setCreatedAt(LocalDateTime.now());
        bad.setAttempts(0);
        service.save(bad);

        relay.pollOnce();

        assertThat(deadMapper.selectCount(null)).isGreaterThanOrEqualTo(1L);
    }
}
