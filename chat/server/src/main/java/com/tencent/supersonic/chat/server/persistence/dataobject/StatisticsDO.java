package com.tencent.supersonic.chat.server.persistence.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 对话统计数据对象 对应表: s2_chat_statistics
 */
@Data
@Builder
@NoArgsConstructor
@Getter
@AllArgsConstructor
public class StatisticsDO {

    /** ID */
    private Long id;

    /** 问题ID */
    private Long questionId;

    /** 对话ID */
    private Long chatId;

    /** 创建时间 */
    private Date createTime;

    /** 查询文本 */
    private String queryText;

    /** 用户名 */
    private String userName;

    /** 接口名称 */
    private String interfaceName;

    /** 耗时(ms) */
    private Integer cost;

    /** 类型 */
    private Integer type;

    /** 租户ID */
    private Long tenantId;
}
