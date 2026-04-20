package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.llm.event.LlmUsageEvent;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import com.tencent.supersonic.common.llm.pojo.LlmUsageRecord;
import com.tencent.supersonic.common.llm.service.LlmUsageService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class LlmUsageRecorder {

    private final LlmUsageService service;
    private final ApplicationEventPublisher publisher;
    private final CostEstimator estimator;
    private final BlockingQueue<LlmUsageRecord> queue;
    private final int flushSize;
    private final ExecutorService flusher = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "llm-usage-flusher");
        t.setDaemon(true);
        return t;
    });
    private final AtomicLong dropCount = new AtomicLong();

    public LlmUsageRecorder(LlmUsageService service, ApplicationEventPublisher publisher,
            CostEstimator estimator, @Value("${s2.llm.usage.queue-capacity:10000}") int capacity,
            @Value("${s2.llm.usage.flush-size:100}") int flushSize) {
        this.service = service;
        this.publisher = publisher;
        this.estimator = estimator;
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.flushSize = flushSize;
    }

    public void record(LlmUsageRecord r) {
        if (r.getEstimatedCostMicros() == 0) {
            r.setEstimatedCostMicros(estimator.estimate(r.getProvider(), r.getModel(),
                    r.getInputTokens(), r.getOutputTokens()));
        }
        if (!queue.offer(r)) {
            long n = dropCount.incrementAndGet();
            if (n % 100 == 1) {
                log.warn("LlmUsageRecorder queue full, dropped {} records so far", n);
            }
            return;
        }
        if (queue.size() >= flushSize) {
            flusher.submit(this::flushNow);
        }
    }

    @Scheduled(fixedDelayString = "${s2.llm.usage.flush-interval-ms:5000}")
    public void scheduledFlush() {
        flushNow();
    }

    @PreDestroy
    public void shutdown() {
        flushNow();
        flusher.shutdown();
    }

    public synchronized void flushNow() {
        if (queue.isEmpty()) {
            return;
        }
        List<LlmUsageRecord> batch = new ArrayList<>(queue.size());
        queue.drainTo(batch);
        if (batch.isEmpty()) {
            return;
        }
        List<LlmUsageDO> dos = batch.stream().map(this::toDO).toList();
        try {
            service.batchInsert(dos);
            publisher.publishEvent(new LlmUsageEvent(this, dos));
        } catch (Exception e) {
            log.error("LlmUsageRecorder flush failed, {} records lost", dos.size(), e);
        }
    }

    private LlmUsageDO toDO(LlmUsageRecord r) {
        LlmUsageDO d = new LlmUsageDO();
        d.setTenantId(r.getTenantId());
        d.setUserId(r.getUserId());
        d.setProvider(r.getProvider());
        d.setModel(r.getModel());
        d.setCallType(
                r.getCallType() == null ? LlmCallType.UNKNOWN.name() : r.getCallType().name());
        d.setInputTokens(r.getInputTokens());
        d.setOutputTokens(r.getOutputTokens());
        d.setTotalTokens(r.getTotalTokens());
        d.setEstimatedCostMicros(r.getEstimatedCostMicros());
        d.setRequestId(r.getRequestId());
        d.setTraceId(r.getTraceId());
        d.setLatencyMs(r.getLatencyMs());
        d.setSuccess(r.isSuccess());
        d.setErrorType(r.getErrorType());
        d.setCreatedAt(r.getCreatedAt() == null ? new Timestamp(System.currentTimeMillis())
                : Timestamp.from(r.getCreatedAt()));
        return d;
    }
}
