package com.tencent.supersonic.common.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 对话模型实例数据对象 对应表: s2_chat_model
 */
@Data
@TableName("s2_chat_model")
public class ChatModelDO {

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 配置信息 */
    private String config;

    /** 创建时间 */
    private Date createdAt;

    /** 创建人 */
    private String createdBy;

    /** 更新时间 */
    private Date updatedAt;

    /** 更新人 */
    private String updatedBy;

    /** 管理员 */
    private String admin;

    /** 查看者 */
    private String viewer;

    /** 是否公开 */
    private Integer isOpen;

    /** 租户ID */
    private Long tenantId;
}
