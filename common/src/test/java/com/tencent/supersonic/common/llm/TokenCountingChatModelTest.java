package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.llm.pojo.LlmUsageRecord;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class TokenCountingChatModelTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        LlmCallContext.clear();
    }

    private ChatLanguageModel fakeDelegate() {
        ChatLanguageModel delegate = mock(ChatLanguageModel.class);
        Response<AiMessage> r = Response.from(AiMessage.from("hello"), new TokenUsage(42, 7, 49));
        when(delegate.generate(anyList())).thenReturn(r);
        return delegate;
    }

    @Test
    void capturesUsageWithTenantAndCallType() {
        LlmUsageRecorder recorder = mock(LlmUsageRecorder.class);
        TenantContext.setTenantId(7L);
        LlmCallContext.set(LlmCallType.NL2SQL, "req-1", "trace-1", "alice");

        TokenCountingChatModel wrapped =
                new TokenCountingChatModel(fakeDelegate(), recorder, "OPEN_AI", "gpt-4o-mini");

        Response<AiMessage> out = wrapped.generate(List.<ChatMessage>of(UserMessage.from("hi")));

        assertThat(out.content().text()).isEqualTo("hello");

        ArgumentCaptor<LlmUsageRecord> captor = ArgumentCaptor.forClass(LlmUsageRecord.class);
        verify(recorder).record(captor.capture());
        LlmUsageRecord rec = captor.getValue();
        assertThat(rec.getTenantId()).isEqualTo(7L);
        assertThat(rec.getProvider()).isEqualTo("OPEN_AI");
        assertThat(rec.getModel()).isEqualTo("gpt-4o-mini");
        assertThat(rec.getInputTokens()).isEqualTo(42);
        assertThat(rec.getOutputTokens()).isEqualTo(7);
        assertThat(rec.getTotalTokens()).isEqualTo(49);
        assertThat(rec.getCallType()).isEqualTo(LlmCallType.NL2SQL);
        assertThat(rec.getRequestId()).isEqualTo("req-1");
        assertThat(rec.getTraceId()).isEqualTo("trace-1");
        assertThat(rec.getUserId()).isEqualTo("alice");
        assertThat(rec.isSuccess()).isTrue();
    }

    @Test
    void recordsFailureAndRethrows() {
        LlmUsageRecorder recorder = mock(LlmUsageRecorder.class);
        ChatLanguageModel delegate = mock(ChatLanguageModel.class);
        when(delegate.generate(anyList())).thenThrow(new RuntimeException("boom"));
        TenantContext.setTenantId(7L);

        TokenCountingChatModel wrapped =
                new TokenCountingChatModel(delegate, recorder, "OPEN_AI", "gpt-4o-mini");

        try {
            wrapped.generate(List.<ChatMessage>of(UserMessage.from("hi")));
        } catch (RuntimeException expected) {
            // ok
        }

        ArgumentCaptor<LlmUsageRecord> captor = ArgumentCaptor.forClass(LlmUsageRecord.class);
        verify(recorder).record(captor.capture());
        LlmUsageRecord r = captor.getValue();
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getErrorType()).isEqualTo("RuntimeException");
    }

    @Test
    void nullTenantContextRecordsNullTenantWithoutFailing() {
        LlmUsageRecorder recorder = mock(LlmUsageRecorder.class);
        TokenCountingChatModel wrapped =
                new TokenCountingChatModel(fakeDelegate(), recorder, "OPEN_AI", "gpt-4o-mini");

        wrapped.generate(List.<ChatMessage>of(UserMessage.from("hi")));

        ArgumentCaptor<LlmUsageRecord> captor = ArgumentCaptor.forClass(LlmUsageRecord.class);
        verify(recorder).record(captor.capture());
        assertThat(captor.getValue().getTenantId()).isNull();
    }
}
