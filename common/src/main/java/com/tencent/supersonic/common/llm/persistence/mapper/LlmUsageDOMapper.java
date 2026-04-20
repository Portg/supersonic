package com.tencent.supersonic.common.llm.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Mapper
public interface LlmUsageDOMapper extends BaseMapper<LlmUsageDO> {

    @Select("SELECT COALESCE(SUM(total_tokens), 0) FROM s2_llm_usage "
            + "WHERE tenant_id = #{tenantId} "
            + "AND created_at >= #{start} AND created_at < #{endExclusive}")
    long sumTokens(@Param("tenantId") Long tenantId, @Param("start") Timestamp start,
            @Param("endExclusive") Timestamp endExclusive);

    @Select("SELECT DATE(created_at) AS day, SUM(total_tokens) AS tokens, "
            + "SUM(estimated_cost_micros) AS cost "
            + "FROM s2_llm_usage WHERE tenant_id = #{tenantId} "
            + "AND created_at >= #{start} AND created_at < #{endExclusive} "
            + "GROUP BY DATE(created_at) ORDER BY day")
    List<Map<String, Object>> dailyAggregates(@Param("tenantId") Long tenantId,
            @Param("start") Timestamp start, @Param("endExclusive") Timestamp endExclusive);
}
