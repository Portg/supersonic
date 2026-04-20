package com.tencent.supersonic.common.metrics;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared infrastructure for async metrics operations (e.g. Pushgateway push). Keeps metrics
 * recording off the caller's thread when the operation involves I/O (network push, file write).
 */
public final class MicrometerUtils {

    static final int CORE_SIZE = Integer.getInteger("s2.metrics.async.core-size", 1);

    private static final ScheduledExecutorService EXECUTOR =
            new ScheduledThreadPoolExecutor(CORE_SIZE, new MetricsThreadFactory());

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(EXECUTOR::shutdownNow, "metrics-shutdown"));
    }

    private MicrometerUtils() {}

    /**
     * Submits a metrics task for asynchronous execution. Use for I/O-bound recording (e.g.
     * Pushgateway push) to avoid blocking the caller's thread.
     */
    public static void async(Runnable task) {
        if (task != null) {
            EXECUTOR.execute(task);
        }
    }

    public static ScheduledExecutorService getScheduledExecutor() {
        return EXECUTOR;
    }

    private static final class MetricsThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "micrometer-async-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }
}
