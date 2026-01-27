package com.tencent.supersonic.headless.server.persistence.mapper;

import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.TagDO;
import com.tencent.supersonic.headless.server.pojo.TagFilter;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagCustomMapper {

    @Select("<script>" + "SELECT * FROM ("
            + "SELECT s2_tag.*, s2_dimension.model_id AS model_id, s2_dimension.sensitive_level AS sensitive_level, "
            + "s2_dimension.name AS name, s2_dimension.biz_name AS biz_name, s2_dimension.description AS description "
            + "FROM s2_tag JOIN s2_dimension ON s2_tag.item_id = s2_dimension.id "
            + "WHERE s2_dimension.status = 1 AND s2_tag.type = 'DIMENSION' " + "UNION "
            + "SELECT s2_tag.*, s2_metric.model_id AS model_id, s2_metric.sensitive_level AS sensitive_level, "
            + "s2_metric.name AS name, s2_metric.biz_name AS biz_name, s2_metric.description AS description "
            + "FROM s2_tag JOIN s2_metric ON s2_tag.item_id = s2_metric.id "
            + "WHERE s2_metric.status = 1 AND s2_tag.type = 'METRIC'" + ") t" + "<where>"
            + "<if test='tagDefineType != null'>AND type = #{tagDefineType}</if>"
            + "<if test='itemIds != null and itemIds.size() > 0'>"
            + "AND item_id IN <foreach collection='itemIds' item='itemId' open='(' separator=',' close=')'>#{itemId}</foreach>"
            + "</if>" + "<if test='ids != null and ids.size() > 0'>"
            + "AND id IN <foreach collection='ids' item='tagId' open='(' separator=',' close=')'>#{tagId}</foreach>"
            + "</if>" + "<if test='name != null and name != \"\"'>AND name = #{name}</if>"
            + "<if test='bizName != null and bizName != \"\"'>AND biz_name = #{bizName}</if>"
            + "<if test='modelIds != null and modelIds.size() > 0'>"
            + "AND model_id IN <foreach collection='modelIds' item='modelId' open='(' separator=',' close=')'>#{modelId}</foreach>"
            + "</if>" + "<if test='key != null and key != \"\"'>"
            + "AND (id LIKE CONCAT('%',#{key},'%') OR name LIKE CONCAT('%',#{key},'%') "
            + "OR biz_name LIKE CONCAT('%',#{key},'%') OR description LIKE CONCAT('%',#{key},'%'))"
            + "</if>"
            + "<if test='sensitiveLevel != null'>AND sensitive_level = #{sensitiveLevel}</if>"
            + "<if test='createdBy != null and createdBy != \"\"'>AND created_by = #{createdBy}</if>"
            + "</where>" + " ORDER BY updated_at DESC" + "</script>")
    List<TagResp> queryTagRespList(TagFilter tagFilter);

    @Select("<script>" + "SELECT * FROM s2_tag" + "<where>"
            + "<if test='itemIds != null and itemIds.size() > 0'>"
            + "AND item_id IN <foreach collection='itemIds' item='itemId' open='(' separator=',' close=')'>#{itemId}</foreach>"
            + "</if>" + "<if test='ids != null and ids.size() > 0'>"
            + "AND id IN <foreach collection='ids' item='tagId' open='(' separator=',' close=')'>#{tagId}</foreach>"
            + "</if>" + "<if test='tagDefineType != null'>AND type = #{tagDefineType}</if>"
            + "</where>" + "</script>")
    List<TagDO> getTagDOList(TagFilter tagFilter);

    @Delete("DELETE FROM s2_tag WHERE id = #{id}")
    Boolean deleteById(@Param("id") Long id);

    @Delete("<script>" + "DELETE FROM s2_tag WHERE id IN "
            + "<foreach collection='list' item='tagId' open='(' separator=',' close=')'>#{tagId}</foreach>"
            + "</script>")
    void deleteBatchByIds(@Param("list") List<Long> ids);

    @Delete("<script>" + "DELETE FROM s2_tag WHERE type = #{type} "
            + "<if test='itemIds != null and itemIds.size() > 0'>"
            + "AND item_id IN <foreach collection='itemIds' item='itemId' open='(' separator=',' close=')'>#{itemId}</foreach>"
            + "</if>" + "</script>")
    void deleteBatchByType(@Param("itemIds") List<Long> itemIds, @Param("type") String type);
}
