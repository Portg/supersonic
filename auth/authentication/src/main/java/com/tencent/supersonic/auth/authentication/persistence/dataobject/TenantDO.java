package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

/**
 * Data object for tenant table.
 */
@Data
@TableName("s2_tenant")
public class TenantDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String description;

    private String status;

    private Long planId;

    private String contactEmail;

    private String contactName;

    private String contactPhone;

    private String logoUrl;

    private String settings;

    private Integer maxUsers;

    private Integer maxDatasets;

    private Integer maxModels;

    private Integer maxAgents;

    private Integer maxApiCallsPerDay;

    private Long maxTokensPerMonth;

    private Timestamp createdAt;

    private String createdBy;

    private Timestamp updatedAt;

    private String updatedBy;
}
