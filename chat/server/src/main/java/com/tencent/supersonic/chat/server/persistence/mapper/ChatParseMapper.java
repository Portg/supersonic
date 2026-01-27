package com.tencent.supersonic.chat.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatParseDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ChatParseMapper extends BaseMapper<ChatParseDO> {

    @Insert("<script>"
            + "INSERT INTO s2_chat_parse (question_id, chat_id, parse_id, create_time, query_text, user_name, parse_info, is_candidate) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.questionId}, #{item.chatId}, #{item.parseId}, #{item.createTime}, #{item.queryText}, #{item.userName}, #{item.parseInfo}, #{item.isCandidate})"
            + "</foreach>" + "</script>")
    boolean batchSaveParseInfo(@Param("list") List<ChatParseDO> list);

    @Update("UPDATE s2_chat_parse SET parse_info = #{parseInfo} WHERE question_id = #{questionId} AND parse_id = #{parseId}")
    boolean updateParseInfo(ChatParseDO chatParseDO);

    @Select("SELECT * FROM s2_chat_parse WHERE question_id = #{questionId} AND parse_id = #{parseId} LIMIT 1")
    ChatParseDO getParseInfo(@Param("questionId") Long questionId, @Param("parseId") int parseId);

    @Select("<script>" + "SELECT * FROM s2_chat_parse WHERE question_id IN "
            + "<foreach collection='list' item='questionId' open='(' separator=',' close=')'>"
            + "#{questionId}" + "</foreach>" + "</script>")
    List<ChatParseDO> getParseInfoList(@Param("list") List<Long> questionIds);

    @Select("SELECT * FROM s2_chat_parse WHERE chat_id = #{chatId} ORDER BY question_id DESC LIMIT 10")
    List<ChatParseDO> getContextualParseInfo(@Param("chatId") Integer chatId);
}
