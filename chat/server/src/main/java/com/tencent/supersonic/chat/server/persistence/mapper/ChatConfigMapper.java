package com.tencent.supersonic.chat.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.chat.server.config.ChatConfigFilterInternal;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatConfigDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatConfigMapper extends BaseMapper<ChatConfigDO> {

    @Select("<script>" + "SELECT * FROM s2_chat_config" + "<where>"
            + "<if test='id != null'>id = #{id}</if>"
            + "<if test='modelId != null'>AND model_id = #{modelId}</if>"
            + "<if test='status != null'>AND status = #{status}</if>" + "</where>" + "</script>")
    List<ChatConfigDO> search(ChatConfigFilterInternal filterInternal);

    @Select("SELECT * FROM s2_chat_config WHERE model_id = #{modelId} AND status != 3 ORDER BY updated_at DESC LIMIT 1")
    ChatConfigDO fetchConfigByModelId(@Param("modelId") Long modelId);
}
