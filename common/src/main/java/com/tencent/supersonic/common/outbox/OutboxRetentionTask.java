package com.tencent.supersonic.common.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(prefix = "s2.outbox", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@Slf4j
public class OutboxRetentionTask {

    private final OutboxEventService service;
    private final OutboxProperties props;

    public OutboxRetentionTask(OutboxEventService service, OutboxProperties props) {
        this.service = service;
        this.props = props;
    }

    /** Runs daily at 03:15 local time. */
    @Scheduled(cron = "0 15 3 * * *")
    public void runCleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(props.getRetentionDays());
        int n = service.getBaseMapper().deleteProcessedBefore(cutoff);
        if (n > 0) {
            log.info("Outbox TTL cleanup: deleted {} rows processed before {}", n, cutoff);
        }
    }
}
