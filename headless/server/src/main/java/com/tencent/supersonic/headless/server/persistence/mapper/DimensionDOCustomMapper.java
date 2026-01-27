package com.tencent.supersonic.headless.server.persistence.mapper;

import com.tencent.supersonic.headless.server.persistence.dataobject.DimensionDO;
import com.tencent.supersonic.headless.server.pojo.DimensionsFilter;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DimensionDOCustomMapper {

    @Insert("<script>"
            + "INSERT INTO s2_dimension (name, biz_name, description, status, model_id, type, type_params, expr, "
            + "created_at, created_by, updated_by, updated_at, semantic_type, sensitive_level, is_tag, ext) VALUES "
            + "<foreach collection='list' item='dimension' separator=','>"
            + "(#{dimension.name}, #{dimension.bizName}, #{dimension.description}, #{dimension.status}, "
            + "#{dimension.modelId}, #{dimension.type}, #{dimension.typeParams}, #{dimension.expr}, "
            + "#{dimension.createdAt}, #{dimension.createdBy}, #{dimension.updatedBy}, #{dimension.updatedAt}, "
            + "#{dimension.semanticType}, #{dimension.sensitiveLevel}, #{dimension.isTag}, #{dimension.ext})"
            + "</foreach>" + "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void batchInsert(@Param("list") List<DimensionDO> dimensionDOS);

    @Update("<script>" + "<foreach collection='list' item='dimension' separator=';'>"
            + "UPDATE s2_dimension SET status = #{dimension.status}, updated_by = #{dimension.updatedBy}, "
            + "updated_at = #{dimension.updatedAt} WHERE id = #{dimension.id}" + "</foreach>"
            + "</script>")
    void batchUpdateStatus(@Param("list") List<DimensionDO> dimensionDOS);

    @Update("<script>" + "<foreach collection='list' item='dimension' separator=';'>"
            + "UPDATE s2_dimension SET "
            + "<if test='dimension.name != null and dimension.name != \"\"'>name = #{dimension.name},</if>"
            + "<if test='dimension.bizName != null and dimension.bizName != \"\"'>biz_name = #{dimension.bizName},</if>"
            + "<if test='dimension.description != null and dimension.description != \"\"'>description = #{dimension.description},</if>"
            + "<if test='dimension.status != null'>status = #{dimension.status},</if>"
            + "<if test='dimension.modelId != null'>model_id = #{dimension.modelId},</if>"
            + "<if test='dimension.type != null and dimension.type != \"\"'>type = #{dimension.type},</if>"
            + "<if test='dimension.typeParams != null and dimension.typeParams != \"\"'>type_params = #{dimension.typeParams},</if>"
            + "<if test='dimension.createdAt != null'>created_at = #{dimension.createdAt},</if>"
            + "<if test='dimension.createdBy != null and dimension.createdBy != \"\"'>created_by = #{dimension.createdBy},</if>"
            + "<if test='dimension.semanticType != null and dimension.semanticType != \"\"'>semantic_type = #{dimension.semanticType},</if>"
            + "<if test='dimension.sensitiveLevel != null'>sensitive_level = #{dimension.sensitiveLevel},</if>"
            + "<if test='dimension.expr != null and dimension.expr != \"\"'>expr = #{dimension.expr},</if>"
            + "<if test='dimension.updatedBy != null and dimension.updatedBy != \"\"'>updated_by = #{dimension.updatedBy},</if>"
            + "<if test='dimension.updatedAt != null'>updated_at = #{dimension.updatedAt}</if>"
            + " WHERE id = #{dimension.id}" + "</foreach>" + "</script>")
    void batchUpdate(@Param("list") List<DimensionDO> dimensionDOS);

    @Select("<script>" + "SELECT * FROM s2_dimension WHERE status != 3 "
            + "<if test='modelIds != null and modelIds.size() > 0'>"
            + "AND model_id IN <foreach collection='modelIds' item='model' open='(' separator=',' close=')'>#{model}</foreach>"
            + "</if>" + "<if test='dimensionIds != null and dimensionIds.size() > 0'>"
            + "AND id IN <foreach collection='dimensionIds' item='dimensionId' open='(' separator=',' close=')'>#{dimensionId}</foreach>"
            + "</if>" + "<if test='dimensionNames != null and dimensionNames.size() > 0'>"
            + "AND ((name IN <foreach collection='dimensionNames' item='dimensionName' open='(' separator=',' close=')'>#{dimensionName}</foreach>) "
            + "OR (biz_name IN <foreach collection='dimensionNames' item='dimensionName' open='(' separator=',' close=')'>#{dimensionName}</foreach>))"
            + "</if>" + "</script>")
    List<DimensionDO> queryDimensions(DimensionsFilter dimensionsFilter);
}
