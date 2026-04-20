package dev.langchain4j.provider;

import com.tencent.supersonic.common.llm.LlmUsageRecorder;
import com.tencent.supersonic.common.llm.TokenCountingChatModel;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import com.tencent.supersonic.common.util.AESEncryptionUtil;
import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.dify.DifyAiChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
public class DifyModelFactory implements ModelFactory, InitializingBean {
    public static final String PROVIDER = "DIFY";

    public static final String DEFAULT_BASE_URL = "https://dify.com/v1/chat-messages";
    public static final String DEFAULT_MODEL_NAME = "demo-预留-可不填写";
    public static final String DEFAULT_EMBEDDING_MODEL_NAME = "all-minilm";

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
        ChatLanguageModel raw = DifyAiChatModel.builder().baseUrl(modelConfig.getBaseUrl())
                .apiKey(AESEncryptionUtil.aesDecryptECB(modelConfig.getApiKey()))
                .modelName(modelConfig.getModelName()).timeOut(modelConfig.getTimeOut()).build();
        LlmUsageRecorder recorder = getRecorder();
        if (recorder == null) {
            return raw;
        }
        return new TokenCountingChatModel(raw, recorder, PROVIDER, modelConfig.getModelName());
    }

    @Override
    public StreamingChatLanguageModel createChatStreamingModel(ChatModelConfig modelConfig) {
        throw new RuntimeException("待开发");
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        return OpenAiEmbeddingModel.builder().baseUrl(embeddingModelConfig.getBaseUrl())
                .apiKey(embeddingModelConfig.getApiKey())
                .modelName(embeddingModelConfig.getModelName())
                .maxRetries(embeddingModelConfig.getMaxRetries())
                .logRequests(embeddingModelConfig.getLogRequests())
                .logResponses(embeddingModelConfig.getLogResponses()).build();
    }

    @Override
    public void afterPropertiesSet() {
        ModelProvider.add(PROVIDER, this);
    }

    private LlmUsageRecorder getRecorder() {
        try {
            return ContextUtils.getBean(LlmUsageRecorder.class);
        } catch (Exception e) {
            return null;
        }
    }
}
