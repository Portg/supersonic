package com.tencent.supersonic.common.llm.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Mapper
public interface LlmUsageDOMapper extends BaseMapper<LlmUsageDO> {

    @Insert({"<script>",
                    "INSERT INTO s2_llm_usage (tenant_id, user_id, model, provider, call_type,",
                    "input_tokens, output_tokens, total_tokens, estimated_cost_micros, request_id,",
                    "trace_id, latency_ms, success, error_type, created_at) VALUES",
                    "<foreach collection='list' item='item' separator=','>",
                    "(#{item.tenantId}, #{item.userId}, #{item.model}, #{item.provider},",
                    "#{item.callType}, #{item.inputTokens}, #{item.outputTokens}, #{item.totalTokens},",
                    "#{item.estimatedCostMicros}, #{item.requestId}, #{item.traceId},",
                    "#{item.latencyMs},",
                    "<choose><when test='item.success == null'>DEFAULT</when>",
                    "<when test='item.success'>1</when><otherwise>0</otherwise></choose>,",
                    "#{item.errorType}, #{item.createdAt})", "</foreach>", "</script>"})
    void batchInsert(@Param("list") List<LlmUsageDO> records);

    @Select("SELECT COALESCE(SUM(total_tokens), 0) FROM s2_llm_usage "
            + "WHERE tenant_id = #{tenantId} "
            + "AND created_at >= #{start} AND created_at < #{endExclusive}")
    long sumTokens(@Param("tenantId") Long tenantId, @Param("start") Timestamp start,
            @Param("endExclusive") Timestamp endExclusive);

    @Select("SELECT CAST(created_at AS DATE) AS day, SUM(total_tokens) AS tokens, "
            + "SUM(estimated_cost_micros) AS cost "
            + "FROM s2_llm_usage WHERE tenant_id = #{tenantId} "
            + "AND created_at >= #{start} AND created_at < #{endExclusive} "
            + "GROUP BY CAST(created_at AS DATE) ORDER BY day")
    List<Map<String, Object>> dailyAggregates(@Param("tenantId") Long tenantId,
            @Param("start") Timestamp start, @Param("endExclusive") Timestamp endExclusive);
}
