package com.tencent.supersonic.common.outbox;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "s2.outbox")
public class OutboxProperties {

    /** Master switch. When false, OutboxPublisher falls back to synchronous publish. */
    private boolean enabled = true;

    /** Relay polling interval (ms). */
    private long pollIntervalMs = 2000L;

    /** Max rows to claim per poll. */
    private int batchSize = 100;

    /** Retention for processed rows (days). TTL job deletes older. */
    private int retentionDays = 7;

    /** After this many attempts, the row is moved to s2_outbox_dead. */
    private int maxAttempts = 5;
}
