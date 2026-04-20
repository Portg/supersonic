package com.tencent.supersonic.common.llm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmPricingDO;
import com.tencent.supersonic.common.llm.persistence.mapper.LlmPricingDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class CostEstimatorImpl implements CostEstimator {

    private final LlmPricingDOMapper mapper;
    private final Cache<String, Optional<LlmPricingDO>> cache =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).maximumSize(500).build();

    public CostEstimatorImpl(LlmPricingDOMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public long estimate(String provider, String model, int inputTokens, int outputTokens) {
        if (provider == null || model == null) {
            return 0L;
        }
        Optional<LlmPricingDO> pricing =
                cache.get(key(provider, model), k -> load(provider, model));
        if (pricing.isEmpty()) {
            log.warn("No pricing entry for provider={} model={}; storing cost=0", provider, model);
            return 0L;
        }
        LlmPricingDO p = pricing.get();
        long inMicros = (long) inputTokens * p.getInputPricePer1kMicros() / 1000L;
        long outMicros = (long) outputTokens * p.getOutputPricePer1kMicros() / 1000L;
        return inMicros + outMicros;
    }

    @Override
    public void refresh(String provider, String model) {
        if (provider == null || provider.isBlank() || model == null || model.isBlank()) {
            return;
        }
        cache.invalidate(key(provider, model));
    }

    @Override
    public void refreshAll() {
        cache.invalidateAll();
    }

    private Optional<LlmPricingDO> load(String provider, String model) {
        LambdaQueryWrapper<LlmPricingDO> w = new LambdaQueryWrapper<>();
        w.eq(LlmPricingDO::getProvider, provider).eq(LlmPricingDO::getModel, model)
                .orderByDesc(LlmPricingDO::getEffectiveFrom).last("LIMIT 1");
        List<LlmPricingDO> list = mapper.selectList(w);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    private String key(String provider, String model) {
        return provider + "::" + model;
    }
}
