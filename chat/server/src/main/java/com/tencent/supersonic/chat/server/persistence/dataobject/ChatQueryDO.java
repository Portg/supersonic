package com.tencent.supersonic.chat.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 对话查询数据对象 对应表: s2_chat_query
 */
@Data
@TableName("s2_chat_query")
public class ChatQueryDO {

    /** 问题ID */
    @TableId(type = IdType.AUTO)
    private Long questionId;

    /** 智能体ID */
    private Long agentId;

    /** */
    private Date createTime;

    /** */
    private String userName;

    /** */
    private Integer queryState;

    /** */
    private Long chatId;

    /** */
    private Integer score;

    /** */
    private String feedback;

    /** */
    private String queryText;

    /** */
    private String queryResult;

    private String similarQueries;

    private String parseTimeCost;
}
