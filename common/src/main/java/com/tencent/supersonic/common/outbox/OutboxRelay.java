package com.tencent.supersonic.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Polls {@code s2_outbox} for unprocessed rows, re-publishes as Spring ApplicationEvents, marks
 * {@code processed_at}. Cluster-safe via {@code SELECT ... FOR UPDATE SKIP LOCKED}.
 *
 * <p>
 * Poisoned rows (deserialization failures) are moved to {@code s2_outbox_dead}.
 */
@Component
@ConditionalOnProperty(prefix = "s2.outbox", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@Slf4j
public class OutboxRelay {

    private final OutboxEventService service;
    private final OutboxDeadMapper deadMapper;
    private final ApplicationEventPublisher springPublisher;
    private final OutboxProperties props;
    private final ObjectMapper mapper;
    private final String nodeId;

    public OutboxRelay(OutboxEventService service, OutboxDeadMapper deadMapper,
            ApplicationEventPublisher springPublisher, OutboxProperties props,
            ObjectMapper mapper) {
        this.service = service;
        this.deadMapper = deadMapper;
        this.springPublisher = springPublisher;
        this.props = props;
        this.mapper = mapper;
        this.nodeId = computeNodeId();
    }

    @Scheduled(fixedDelayString = "${s2.outbox.poll-interval-ms:2000}")
    public void poll() {
        try {
            pollOnce();
        } catch (Exception e) {
            log.warn("Outbox poll failed: {}", e.getMessage(), e);
        }
    }

    /** Visible for testing — drives one claim+dispatch cycle in the calling thread. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void pollOnce() {
        OutboxMapper m = service.getBaseMapper();
        List<OutboxEvent> batch = m.lockUnprocessed(props.getBatchSize());
        if (batch.isEmpty()) {
            return;
        }
        log.debug("Outbox relay claimed {} rows on node {}", batch.size(), nodeId);

        for (OutboxEvent row : batch) {
            try {
                ApplicationEvent event = deserialize(row);
                springPublisher.publishEvent(event);
                m.markProcessed(row.getId(), LocalDateTime.now(), nodeId);
            } catch (DeserializationException de) {
                handlePoisonedRow(row, de.getMessage());
            } catch (Exception e) {
                log.warn("Listener for outbox id={} failed: {}", row.getId(), e.getMessage(), e);
                int attempts = row.getAttempts() == null ? 0 : row.getAttempts();
                if (attempts + 1 >= props.getMaxAttempts()) {
                    handlePoisonedRow(row, "Listener failed after " + (attempts + 1) + " attempts: "
                            + e.getMessage());
                } else {
                    m.recordFailure(row.getId(), e.getMessage());
                }
            }
        }
    }

    private ApplicationEvent deserialize(OutboxEvent row) {
        try {
            Class<?> cls = Class.forName(row.getEventType());
            Object obj = mapper.readValue(row.getPayloadJson(), cls);
            if (!(obj instanceof ApplicationEvent ae)) {
                throw new DeserializationException(
                        "Row " + row.getId() + " payload is not ApplicationEvent: " + cls);
            }
            return ae;
        } catch (DeserializationException rethrow) {
            throw rethrow;
        } catch (Exception e) {
            throw new DeserializationException(
                    "Deserialize failed for row " + row.getId() + ": " + e.getMessage(), e);
        }
    }

    private void handlePoisonedRow(OutboxEvent row, String reason) {
        log.error("Moving outbox row id={} type={} to dead table: {}", row.getId(),
                row.getEventType(), reason);
        OutboxDeadEvent dead = new OutboxDeadEvent();
        dead.setOriginalId(row.getId());
        dead.setAggregateType(row.getAggregateType());
        dead.setAggregateId(row.getAggregateId());
        dead.setEventType(row.getEventType());
        dead.setPayloadJson(row.getPayloadJson());
        dead.setTenantId(row.getTenantId());
        dead.setFailureReason(reason);
        dead.setAttempts(row.getAttempts() == null ? 0 : row.getAttempts());
        dead.setCreatedAt(row.getCreatedAt());
        dead.setDiedAt(LocalDateTime.now());
        deadMapper.insert(dead);
        service.getBaseMapper().markProcessed(row.getId(), LocalDateTime.now(), nodeId);
    }

    private String computeNodeId() {
        try {
            return InetAddress.getLocalHost().getHostName() + ":"
                    + ManagementFactory.getRuntimeMXBean().getName();
        } catch (Exception e) {
            return "unknown:" + System.nanoTime();
        }
    }

    private static final class DeserializationException extends RuntimeException {

        DeserializationException(String msg) {
            super(msg);
        }

        DeserializationException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
