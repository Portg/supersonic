package com.tencent.supersonic.common.llm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface LlmUsageService {
    void batchInsert(List<LlmUsageDO> records);

    IPage<LlmUsageDO> query(Long tenantId, LocalDate from, LocalDate to, String model,
            String callType, int page, int size);

    long sumTokens(Long tenantId, LocalDate from, LocalDate to);

    List<Map<String, Object>> dailyAggregates(Long tenantId, LocalDate from, LocalDate to);
}
