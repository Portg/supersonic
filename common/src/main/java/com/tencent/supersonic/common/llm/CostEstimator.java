package com.tencent.supersonic.common.llm;

public interface CostEstimator {
    long estimate(String provider, String model, int inputTokens, int outputTokens);
}
