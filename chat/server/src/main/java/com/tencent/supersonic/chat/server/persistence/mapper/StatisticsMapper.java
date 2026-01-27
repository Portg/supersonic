package com.tencent.supersonic.chat.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.chat.server.persistence.dataobject.StatisticsDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StatisticsMapper extends BaseMapper<StatisticsDO> {

    @Insert("<script>"
            + "INSERT INTO s2_chat_statistics (question_id, chat_id, user_name, query_text, interface_name, cost, type, create_time) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.questionId}, #{item.chatId}, #{item.userName}, #{item.queryText}, #{item.interfaceName}, #{item.cost}, #{item.type}, #{item.createTime})"
            + "</foreach>" + "</script>")
    boolean batchSaveStatistics(@Param("list") List<StatisticsDO> list);
}
