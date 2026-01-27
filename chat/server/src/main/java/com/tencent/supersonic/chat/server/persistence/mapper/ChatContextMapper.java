package com.tencent.supersonic.chat.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatContextDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChatContextMapper extends BaseMapper<ChatContextDO> {

    @Select("SELECT * FROM s2_chat_context WHERE chat_id = #{chatId} LIMIT 1")
    ChatContextDO getContextByChatId(@Param("chatId") Integer chatId);
}
