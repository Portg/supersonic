package com.tencent.supersonic.chat.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_agent")
public class AgentDO {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private String description;

    /** 0 offline, 1 online */
    private Integer status;

    private String examples;

    /** LLM模型名称 */
    private String model;

    private String createdBy;

    private Date createdAt;

    private String updatedBy;

    private Date updatedAt;

    private Integer enableSearch;

    private Integer enableFeedback;

    private String toolConfig;

    /** LLM配置JSON */
    private String llmConfig;

    private String chatModelConfig;

    private String visualConfig;

    private String admin;

    private String viewer;

    private String adminOrg;

    private String viewOrg;

    private Integer isOpen;

    /** Tenant ID for multi-tenancy */
    private Long tenantId;
}
