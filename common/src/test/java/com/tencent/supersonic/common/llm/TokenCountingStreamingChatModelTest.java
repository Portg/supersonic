package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.llm.pojo.LlmUsageRecord;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TokenCountingStreamingChatModelTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        LlmCallContext.clear();
    }

    @Test
    void capturesUsageOnStreamCompleteAndForwardsCallbacks() {
        LlmUsageRecorder recorder = mock(LlmUsageRecorder.class);
        TenantContext.setTenantId(7L);
        LlmCallContext.set(LlmCallType.DATA_INTERPRET, "req-s1", "trace-s1", "bob");

        StreamingChatLanguageModel delegate = new StreamingChatLanguageModel() {
            @Override
            public void generate(List<ChatMessage> messages,
                    StreamingResponseHandler<AiMessage> handler) {
                handler.onNext("hel");
                handler.onNext("lo");
                handler.onComplete(
                        Response.from(AiMessage.from("hello"), new TokenUsage(10, 2, 12)));
            }
        };

        TokenCountingStreamingChatModel wrapped =
                new TokenCountingStreamingChatModel(delegate, recorder, "OPEN_AI", "gpt-4o-mini");

        StringBuilder sb = new StringBuilder();
        boolean[] done = {false};
        wrapped.generate(List.<ChatMessage>of(UserMessage.from("hi")),
                new StreamingResponseHandler<>() {
                    @Override
                    public void onNext(String token) {
                        sb.append(token);
                    }

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        done[0] = true;
                    }

                    @Override
                    public void onError(Throwable error) {}
                });

        assertThat(sb.toString()).isEqualTo("hello");
        assertThat(done[0]).isTrue();

        ArgumentCaptor<LlmUsageRecord> captor = ArgumentCaptor.forClass(LlmUsageRecord.class);
        verify(recorder).record(captor.capture());
        LlmUsageRecord rec = captor.getValue();
        assertThat(rec.getInputTokens()).isEqualTo(10);
        assertThat(rec.getOutputTokens()).isEqualTo(2);
        assertThat(rec.getTotalTokens()).isEqualTo(12);
        assertThat(rec.getCallType()).isEqualTo(LlmCallType.DATA_INTERPRET);
        assertThat(rec.isSuccess()).isTrue();
    }

    @Test
    void recordsFailureOnStreamError() {
        LlmUsageRecorder recorder = mock(LlmUsageRecorder.class);

        StreamingChatLanguageModel delegate = new StreamingChatLanguageModel() {
            @Override
            public void generate(List<ChatMessage> messages,
                    StreamingResponseHandler<AiMessage> handler) {
                handler.onError(new RuntimeException("boom"));
            }
        };

        TokenCountingStreamingChatModel wrapped =
                new TokenCountingStreamingChatModel(delegate, recorder, "OPEN_AI", "gpt-4o-mini");

        wrapped.generate(List.<ChatMessage>of(UserMessage.from("hi")),
                new StreamingResponseHandler<>() {
                    @Override
                    public void onNext(String token) {}

                    @Override
                    public void onComplete(Response<AiMessage> response) {}

                    @Override
                    public void onError(Throwable error) {}
                });

        ArgumentCaptor<LlmUsageRecord> captor = ArgumentCaptor.forClass(LlmUsageRecord.class);
        verify(recorder).record(captor.capture());
        assertThat(captor.getValue().isSuccess()).isFalse();
        assertThat(captor.getValue().getErrorType()).isEqualTo("RuntimeException");
    }
}
