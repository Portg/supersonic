package com.tencent.supersonic.headless.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.headless.api.pojo.ItemDateFilter;
import com.tencent.supersonic.headless.server.persistence.dataobject.DateInfoDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DateInfoMapper extends BaseMapper<DateInfoDO> {

    @Select("<script>" + "SELECT e.* FROM s2_available_date_info e " + "INNER JOIN ("
            + "SELECT item_id, MAX(created_at) AS created_at FROM s2_available_date_info "
            + "WHERE `type` = #{type} " + "<if test='itemIds != null and itemIds.size() > 0'>"
            + "AND item_id IN <foreach collection='itemIds' item='item' open='(' separator=',' close=')'>#{item}</foreach>"
            + "</if> " + "GROUP BY item_id"
            + ") t ON e.item_id = t.item_id AND e.created_at = t.created_at" + "</script>")
    List<DateInfoDO> getDateInfos(ItemDateFilter itemDateFilter);
}
