package com.tencent.supersonic.chat.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
@TableName("s2_chat_config")
public class ChatConfigDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long modelId;

    private String chatDetailConfig;

    private String chatAggConfig;

    private String recommendedQuestions;

    private Integer status;

    private String llmExamples;

    /** 租户ID */
    private Long tenantId;

    /** 创建人 */
    private String createdBy;

    /** 更新人 */
    private String updatedBy;

    /** 创建时间 */
    private Date createdAt;

    /** 更新时间 */
    private Date updatedAt;
}
