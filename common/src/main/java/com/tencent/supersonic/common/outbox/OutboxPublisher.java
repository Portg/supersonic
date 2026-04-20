package com.tencent.supersonic.common.outbox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tencent.supersonic.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Durable cross-module event publisher.
 *
 * <p>
 * Writes the serialized event into {@code s2_outbox} inside the caller's transaction. The event is
 * NOT dispatched to Spring listeners immediately — {@link OutboxRelay} polls the table and
 * re-publishes asynchronously. If {@code s2.outbox.enabled=false}, falls back to synchronous Spring
 * publish (rollback path for operational safety).
 */
@Component
@Slf4j
public class OutboxPublisher {

    private final OutboxEventService service;
    private final ApplicationEventPublisher springPublisher;
    private final OutboxProperties props;
    private final ObjectMapper mapper;

    /** Mixin that suppresses the unserializable {@code source} field from Spring events. */
    @JsonIgnoreProperties({"source"})
    abstract static class ApplicationEventMixin {
    }

    public OutboxPublisher(OutboxEventService service, ApplicationEventPublisher springPublisher,
            OutboxProperties props, ObjectMapper mapper) {
        this.service = service;
        this.springPublisher = springPublisher;
        this.props = props;
        // Make a private copy so we don't mutate the shared application mapper.
        this.mapper = mapper.copy().addMixIn(ApplicationEvent.class, ApplicationEventMixin.class)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void publish(ApplicationEvent event) {
        if (!props.isEnabled()) {
            springPublisher.publishEvent(event);
            return;
        }

        String json;
        try {
            json = mapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to serialize outbox event " + event.getClass().getName(), e);
        }

        OutboxEvent row = new OutboxEvent();
        row.setAggregateType(deriveAggregateType(event));
        row.setAggregateId(deriveAggregateId(event));
        row.setEventType(event.getClass().getName());
        row.setPayloadJson(json);
        Long tenantId = TenantContext.getTenantId();
        row.setTenantId(tenantId == null ? 1L : tenantId);
        row.setCreatedAt(LocalDateTime.now());
        row.setAttempts(0);

        service.save(row);
        log.debug("Outbox row id={} type={} tenant={}", row.getId(), row.getEventType(),
                row.getTenantId());
    }

    private String deriveAggregateType(ApplicationEvent event) {
        return event.getClass().getSimpleName();
    }

    private String deriveAggregateId(ApplicationEvent event) {
        try {
            var m = event.getClass().getMethod("getId");
            Object v = m.invoke(event);
            return v == null ? null : v.toString();
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
