package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.llm.pojo.LlmUsageRecord;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
public class TokenCountingStreamingChatModel implements StreamingChatLanguageModel {

    private final StreamingChatLanguageModel delegate;
    private final LlmUsageRecorder recorder;
    private final String provider;
    private final String model;

    public TokenCountingStreamingChatModel(StreamingChatLanguageModel delegate,
            LlmUsageRecorder recorder, String provider, String model) {
        this.delegate = delegate;
        this.recorder = recorder;
        this.provider = provider;
        this.model = model;
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        Long tenantId = TenantContext.getTenantId();
        LlmCallContext.Frame ctx = LlmCallContext.get();
        long t0 = System.currentTimeMillis();

        delegate.generate(messages, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                handler.onNext(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                record(tenantId, ctx, response.tokenUsage(), System.currentTimeMillis() - t0, true,
                        null);
                handler.onComplete(response);
            }

            @Override
            public void onError(Throwable error) {
                record(tenantId, ctx, null, System.currentTimeMillis() - t0, false,
                        error.getClass().getSimpleName());
                handler.onError(error);
            }
        });
    }

    private void record(Long tenantId, LlmCallContext.Frame ctx, TokenUsage usage, long latencyMs,
            boolean success, String errorType) {
        int in = usage == null || usage.inputTokenCount() == null ? 0 : usage.inputTokenCount();
        int out = usage == null || usage.outputTokenCount() == null ? 0 : usage.outputTokenCount();
        int total = usage == null || usage.totalTokenCount() == null ? in + out
                : usage.totalTokenCount();

        LlmUsageRecord r = LlmUsageRecord.builder().tenantId(tenantId)
                .userId(ctx == null ? null : ctx.userId).provider(provider).model(model)
                .callType(ctx == null ? LlmCallType.UNKNOWN : ctx.callType).inputTokens(in)
                .outputTokens(out).totalTokens(total).requestId(ctx == null ? null : ctx.requestId)
                .traceId(ctx == null ? null : ctx.traceId).latencyMs((int) latencyMs)
                .success(success).errorType(errorType).createdAt(Instant.now()).build();
        try {
            recorder.record(r);
        } catch (Exception e) {
            log.warn("Failed to record streaming LLM usage (model={}): {}", model, e.getMessage());
        }
    }
}
