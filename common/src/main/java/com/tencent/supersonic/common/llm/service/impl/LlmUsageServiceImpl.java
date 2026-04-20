package com.tencent.supersonic.common.llm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import com.tencent.supersonic.common.llm.persistence.mapper.LlmUsageDOMapper;
import com.tencent.supersonic.common.llm.service.LlmUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LlmUsageServiceImpl implements LlmUsageService {

    private final LlmUsageDOMapper mapper;
    private static final int BATCH_SIZE = 500;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchInsert(List<LlmUsageDO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (int from = 0; from < records.size(); from += BATCH_SIZE) {
            int to = Math.min(from + BATCH_SIZE, records.size());
            mapper.batchInsert(records.subList(from, to));
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
