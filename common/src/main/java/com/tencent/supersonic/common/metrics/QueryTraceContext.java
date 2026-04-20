package com.tencent.supersonic.common.metrics;

import org.slf4j.MDC;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class QueryTraceContext {

    public static final String KEY = "queryTraceId";
    public static final String PREFIX = "q_";

    private QueryTraceContext() {}

    public static Scope open() {
        String previous = MDC.get(KEY);
        String id = PREFIX + UUID.randomUUID().toString().replace("-", "");
        MDC.put(KEY, id);
        return new Scope(id, previous);
    }

    public static Scope open(String id) {
        String previous = MDC.get(KEY);
        MDC.put(KEY, id);
        return new Scope(id, previous);
    }

    public static Optional<String> current() {
        return Optional.ofNullable(MDC.get(KEY));
    }

    public static Map<String, String> snapshot() {
        Map<String, String> snap = new HashMap<>();
        String v = MDC.get(KEY);
        if (v != null) {
            snap.put(KEY, v);
        }
        return Collections.unmodifiableMap(snap);
    }

    public static void restore(Map<String, String> snapshot) {
        if (snapshot == null) {
            return;
        }
        String v = snapshot.get(KEY);
        if (v != null) {
            MDC.put(KEY, v);
        }
    }

    public static Runnable wrap(Runnable task) {
        Map<String, String> snap = snapshot();
        return () -> {
            String prev = MDC.get(KEY);
            try {
                restore(snap);
                task.run();
            } finally {
                if (prev == null) {
                    MDC.remove(KEY);
                } else {
                    MDC.put(KEY, prev);
                }
            }
        };
    }

    public static final class Scope implements AutoCloseable {
        private final String traceId;
        private final String previous;

        Scope(String traceId, String previous) {
            this.traceId = traceId;
            this.previous = previous;
        }

        public String traceId() {
            return traceId;
        }

        @Override
        public void close() {
            if (previous == null) {
                MDC.remove(KEY);
            } else {
                MDC.put(KEY, previous);
            }
        }
    }
}
