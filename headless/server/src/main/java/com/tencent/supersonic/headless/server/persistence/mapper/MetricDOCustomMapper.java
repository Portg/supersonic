package com.tencent.supersonic.headless.server.persistence.mapper;

import com.tencent.supersonic.headless.server.persistence.dataobject.MetricDO;
import com.tencent.supersonic.headless.server.pojo.MetricsFilter;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MetricDOCustomMapper {

    @Insert("<script>"
            + "INSERT INTO s2_metric (model_id, name, biz_name, description, type, status, sensitive_level, "
            + "created_at, created_by, updated_at, updated_by, type_params, define_type, is_publish) VALUES "
            + "<foreach collection='list' item='metric' separator=','>"
            + "(#{metric.modelId}, #{metric.name}, #{metric.bizName}, #{metric.description}, #{metric.type}, "
            + "#{metric.status}, #{metric.sensitiveLevel}, #{metric.createdAt}, #{metric.createdBy}, "
            + "#{metric.updatedAt}, #{metric.updatedBy}, #{metric.typeParams}, #{metric.defineType}, #{metric.isPublish})"
            + "</foreach>" + "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void batchInsert(@Param("list") List<MetricDO> metricDOS);

    @Update("<script>" + "<foreach collection='list' item='metric' separator=';'>"
            + "UPDATE s2_metric SET status = #{metric.status}, updated_at = #{metric.updatedAt}, "
            + "updated_by = #{metric.updatedBy} WHERE id = #{metric.id}" + "</foreach>"
            + "</script>")
    void batchUpdateStatus(@Param("list") List<MetricDO> metricDOS);

    @Update("<script>" + "<foreach collection='list' item='metric' separator=';'>"
            + "UPDATE s2_metric SET "
            + "<if test='metric.name != null and metric.name != \"\"'>name = #{metric.name},</if>"
            + "<if test='metric.bizName != null and metric.bizName != \"\"'>biz_name = #{metric.bizName},</if>"
            + "<if test='metric.description != null and metric.description != \"\"'>description = #{metric.description},</if>"
            + "<if test='metric.status != null'>status = #{metric.status},</if>"
            + "<if test='metric.modelId != null'>model_id = #{metric.modelId},</if>"
            + "<if test='metric.type != null and metric.type != \"\"'>type = #{metric.type},</if>"
            + "<if test='metric.typeParams != null and metric.typeParams != \"\"'>type_params = #{metric.typeParams},</if>"
            + "<if test='metric.createdAt != null'>created_at = #{metric.createdAt},</if>"
            + "<if test='metric.createdBy != null and metric.createdBy != \"\"'>created_by = #{metric.createdBy},</if>"
            + "<if test='metric.sensitiveLevel != null'>sensitive_level = #{metric.sensitiveLevel},</if>"
            + "<if test='metric.updatedBy != null and metric.updatedBy != \"\"'>updated_by = #{metric.updatedBy},</if>"
            + "<if test='metric.updatedAt != null'>updated_at = #{metric.updatedAt}</if>"
            + " WHERE id = #{metric.id}" + "</foreach>" + "</script>")
    void batchUpdate(@Param("list") List<MetricDO> metricDOS);

    @Update("<script>" + "<foreach collection='list' item='metric' separator=';'>"
            + "UPDATE s2_metric SET is_publish = 1, updated_at = #{metric.updatedAt}, "
            + "updated_by = #{metric.updatedBy} WHERE id = #{metric.id}" + "</foreach>"
            + "</script>")
    void batchPublish(@Param("list") List<MetricDO> metricDOS);

    @Update("<script>" + "<foreach collection='list' item='metric' separator=';'>"
            + "UPDATE s2_metric SET is_publish = 0, updated_at = #{metric.updatedAt}, "
            + "updated_by = #{metric.updatedBy} WHERE id = #{metric.id}" + "</foreach>"
            + "</script>")
    void batchUnPublish(@Param("list") List<MetricDO> metricDOS);

    @Update("<script>" + "<foreach collection='list' item='metric' separator=';'>"
            + "UPDATE s2_metric SET classifications = #{metric.classifications}, updated_at = #{metric.updatedAt}, "
            + "updated_by = #{metric.updatedBy} WHERE id = #{metric.id}" + "</foreach>"
            + "</script>")
    void updateClassificationsBatch(@Param("list") List<MetricDO> metricDOS);

    @Select("<script>" + "SELECT * FROM s2_metric WHERE status != 3 "
            + "<if test='modelIds != null and modelIds.size() > 0'>"
            + "AND model_id IN <foreach collection='modelIds' item='model' open='(' separator=',' close=')'>#{model}</foreach>"
            + "</if>" + "<if test='metricIds != null and metricIds.size() > 0'>"
            + "AND id IN <foreach collection='metricIds' item='metricId' open='(' separator=',' close=')'>#{metricId}</foreach>"
            + "</if>" + "<if test='metricNames != null and metricNames.size() > 0'>"
            + "AND ((name IN <foreach collection='metricNames' item='metricName' open='(' separator=',' close=')'>#{metricName}</foreach>) "
            + "OR (biz_name IN <foreach collection='metricNames' item='metricName' open='(' separator=',' close=')'>#{metricName}</foreach>))"
            + "</if>" + "</script>")
    List<MetricDO> queryMetrics(MetricsFilter metricsFilter);
}
