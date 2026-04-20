package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.llm.pojo.LlmUsageRecord;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
public class TokenCountingChatModel implements ChatLanguageModel {

    private final ChatLanguageModel delegate;
    private final LlmUsageRecorder recorder;
    private final String provider;
    private final String model;

    public TokenCountingChatModel(ChatLanguageModel delegate, LlmUsageRecorder recorder,
            String provider, String model) {
        this.delegate = delegate;
        this.recorder = recorder;
        this.provider = provider;
        this.model = model;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return invoke(() -> delegate.generate(messages));
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages,
            List<ToolSpecification> toolSpecifications) {
        return invoke(() -> delegate.generate(messages, toolSpecifications));
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages,
            ToolSpecification toolSpecification) {
        return invoke(() -> delegate.generate(messages, toolSpecification));
    }

    private Response<AiMessage> invoke(java.util.function.Supplier<Response<AiMessage>> call) {
        long t0 = System.currentTimeMillis();
        try {
            Response<AiMessage> resp = call.get();
            record(resp.tokenUsage(), System.currentTimeMillis() - t0, true, null);
            return resp;
        } catch (RuntimeException e) {
            record(null, System.currentTimeMillis() - t0, false, e.getClass().getSimpleName());
            throw e;
        }
    }

    private void record(TokenUsage usage, long latencyMs, boolean success, String errorType) {
        int in = usage == null || usage.inputTokenCount() == null ? 0 : usage.inputTokenCount();
        int out = usage == null || usage.outputTokenCount() == null ? 0 : usage.outputTokenCount();
        int total = usage == null || usage.totalTokenCount() == null ? in + out
                : usage.totalTokenCount();

        LlmCallContext.Frame ctx = LlmCallContext.get();
        LlmUsageRecord r = LlmUsageRecord.builder().tenantId(TenantContext.getTenantId())
                .userId(ctx == null ? null : ctx.userId).provider(provider).model(model)
                .callType(ctx == null ? LlmCallType.UNKNOWN : ctx.callType).inputTokens(in)
                .outputTokens(out).totalTokens(total).requestId(ctx == null ? null : ctx.requestId)
                .traceId(ctx == null ? null : ctx.traceId).latencyMs((int) latencyMs)
                .success(success).errorType(errorType).createdAt(Instant.now()).build();
        try {
            recorder.record(r);
        } catch (Exception e) {
            log.warn("Failed to record LLM usage (model={}): {}", model, e.getMessage());
        }
    }
}
