package com.tencent.supersonic.billing.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Data object for subscription plan table.
 */
@Data
@TableName("s2_subscription_plan")
public class SubscriptionPlanDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String description;

    private BigDecimal priceMonthly;

    private BigDecimal priceYearly;

    private Integer maxUsers;

    private Integer maxDatasets;

    private Integer maxModels;

    private Integer maxAgents;

    private Integer maxApiCallsPerDay;

    private Long maxTokensPerMonth;

    private String features;

    private Boolean isDefault;

    private String status;

    private Timestamp createdAt;

    private Timestamp updatedAt;
}
