package com.tencent.supersonic.common.llm;

public final class LlmCallContext {

    public static final class Frame {
        public final LlmCallType callType;
        public final String requestId;
        public final String traceId;
        public final String userId;

        public Frame(LlmCallType callType, String requestId, String traceId, String userId) {
            this.callType = callType;
            this.requestId = requestId;
            this.traceId = traceId;
            this.userId = userId;
        }
    }

    public static final class Scope implements AutoCloseable {
        private final Frame previous;
        private boolean closed;

        private Scope(Frame previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

    public static void set(LlmCallType callType, String requestId, String traceId, String userId) {
        CURRENT.set(new Frame(callType, requestId, traceId, userId));
    }

    public static Scope open(LlmCallType callType, String requestId, String traceId,
            String userId) {
        Frame previous = CURRENT.get();
        set(callType, requestId, traceId, userId);
        return new Scope(previous);
    }

    public static Frame get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    private LlmCallContext() {}
}
