package com.tencent.supersonic.common.outbox;

import com.tencent.supersonic.common.context.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = OutboxTestConfig.class)
@TestPropertySource(properties = {"s2.outbox.enabled=true", "s2.outbox.batch-size=50",
                "spring.datasource.url=jdbc:h2:mem:outbox-relay;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.flyway.enabled=false",
                "spring.sql.init.schema-locations=classpath:schema-h2.sql",
                "spring.sql.init.mode=always"})
class OutboxRelayTest {

    @Autowired
    private OutboxPublisher publisher;
    @Autowired
    private OutboxRelay relay;
    @Autowired
    private OutboxEventService service;
    @Autowired
    private OutboxTestListener listener;

    @BeforeEach
    void setUp() {
        service.getBaseMapper().delete(null);
        listener.received.clear();
        TenantContext.setTenantId(1L);
    }

    @Test
    void relay_dispatchesRowsAndMarksProcessed() {
        for (int i = 0; i < 5; i++) {
            publisher.publish(new OutboxPublisherTest.ToyEvent(this, "msg-" + i));
        }
        TenantContext.clear();

        relay.pollOnce();

        await().atMost(ofSeconds(2)).untilAsserted(() -> assertThat(listener.received).hasSize(5));
        assertThat(service.getBaseMapper().countUnprocessed()).isZero();
    }

    @Test
    void twoParallelRelays_doNotDoubleProcess() throws Exception {
        for (int i = 0; i < 20; i++) {
            publisher.publish(new OutboxPublisherTest.ToyEvent(this, "p-" + i));
        }
        TenantContext.clear();

        var executor = Executors.newFixedThreadPool(2);
        CompletableFuture<Void> a = CompletableFuture.runAsync(relay::pollOnce, executor);
        CompletableFuture<Void> b = CompletableFuture.runAsync(relay::pollOnce, executor);
        CompletableFuture.allOf(a, b).get();
        executor.shutdown();

        await().atMost(ofSeconds(3)).untilAsserted(() -> assertThat(listener.received).hasSize(20));
        assertThat(listener.received.stream().distinct().count()).isEqualTo(20);
    }
}
