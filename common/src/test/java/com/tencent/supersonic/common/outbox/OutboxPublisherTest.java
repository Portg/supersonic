package com.tencent.supersonic.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.common.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherTest {

    public static class ToyEvent extends ApplicationEvent {

        public String payload;

        public ToyEvent(Object source, String payload) {
            super(source);
            this.payload = payload;
        }

        public ToyEvent() {
            super("test");
        } // needed for Jackson deserialization
    }

    private OutboxEventService service;
    private ApplicationEventPublisher springPublisher;
    private OutboxPublisher publisher;
    private OutboxProperties props;
    private final List<OutboxEvent> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = mock(OutboxEventService.class);
        springPublisher = mock(ApplicationEventPublisher.class);
        props = new OutboxProperties();
        props.setEnabled(true);
        ObjectMapper mapper = new ObjectMapper();
        publisher = new OutboxPublisher(service, springPublisher, props, mapper);

        when(service.save(any(OutboxEvent.class))).thenAnswer(inv -> {
            saved.add(inv.getArgument(0));
            return true;
        });
        TenantContext.setTenantId(42L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void publish_serializesEventAndInsertsRow() {
        publisher.publish(new ToyEvent(this, "hello"));

        assertThat(saved).hasSize(1);
        OutboxEvent row = saved.get(0);
        assertThat(row.getEventType()).isEqualTo(ToyEvent.class.getName());
        assertThat(row.getTenantId()).isEqualTo(42L);
        assertThat(row.getPayloadJson()).contains("hello");
        assertThat(row.getProcessedAt()).isNull();
        verify(springPublisher, never()).publishEvent(any());
    }

    @Test
    void publish_whenDisabled_fallsBackToSyncPublish() {
        props.setEnabled(false);
        ToyEvent event = new ToyEvent(this, "bye");

        publisher.publish(event);

        verify(springPublisher).publishEvent(event);
        assertThat(saved).isEmpty();
    }

    @Test
    void publish_nullTenant_defaultsToOne() {
        TenantContext.clear();
        publisher.publish(new ToyEvent(this, "no-tenant"));
        assertThat(saved.get(0).getTenantId()).isEqualTo(1L);
    }
}
