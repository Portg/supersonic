package com.tencent.supersonic.headless.server.persistence.mapper;

import com.tencent.supersonic.headless.server.persistence.dataobject.ModelDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ModelDOCustomMapper {

    @Update("<script>" + "<foreach collection='list' item='model' separator=';'>"
            + "UPDATE s2_model SET status = #{model.status}, updated_at = #{model.updatedAt}, "
            + "updated_by = #{model.updatedBy} WHERE id = #{model.id}" + "</foreach>" + "</script>")
    void batchUpdateStatus(@Param("list") List<ModelDO> modelDOS);
}
