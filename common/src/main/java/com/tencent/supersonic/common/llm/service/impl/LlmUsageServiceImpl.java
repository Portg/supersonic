package com.tencent.supersonic.common.llm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import com.tencent.supersonic.common.llm.persistence.mapper.LlmUsageDOMapper;
import com.tencent.supersonic.common.llm.service.LlmUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmUsageServiceImpl implements LlmUsageService {

    private final LlmUsageDOMapper mapper;

    @Override
    public void batchInsert(List<LlmUsageDO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        if (records.size() > 50) {
            log.warn(
                    "LlmUsageServiceImpl.batchInsert: inserting {} records individually - consider batch SQL",
                    records.size());
        }
        for (LlmUsageDO r : records) {
            mapper.insert(r);
        }
    }

    @Override
    public IPage<LlmUsageDO> query(Long tenantId, LocalDate from, LocalDate to, String model,
            String callType, int page, int size) {
        LambdaQueryWrapper<LlmUsageDO> w = new LambdaQueryWrapper<>();
        w.eq(LlmUsageDO::getTenantId, tenantId)
                .ge(from != null, LlmUsageDO::getCreatedAt, toTimestamp(from))
                .lt(to != null, LlmUsageDO::getCreatedAt,
                        toTimestamp(to == null ? null : to.plusDays(1)))
                .eq(model != null && !model.isBlank(), LlmUsageDO::getModel, model)
                .eq(callType != null && !callType.isBlank(), LlmUsageDO::getCallType, callType)
                .orderByDesc(LlmUsageDO::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    public long sumTokens(Long tenantId, LocalDate from, LocalDate to) {
        return mapper.sumTokens(tenantId, toTimestamp(from), toTimestamp(to.plusDays(1)));
    }

    @Override
    public List<Map<String, Object>> dailyAggregates(Long tenantId, LocalDate from, LocalDate to) {
        return mapper.dailyAggregates(tenantId, toTimestamp(from), toTimestamp(to.plusDays(1)));
    }

    private Timestamp toTimestamp(LocalDate d) {
        if (d == null)
            return null;
        return new Timestamp(d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
