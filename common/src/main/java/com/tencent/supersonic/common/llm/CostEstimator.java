package com.tencent.supersonic.common.llm;

public interface CostEstimator {
    long estimate(String provider, String model, int inputTokens, int outputTokens);

    default void refresh(String provider, String model) {}

    default void refreshAll() {}
}
