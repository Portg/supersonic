package com.tencent.supersonic.billing.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

/**
 * Data object for tenant subscription table.
 */
@Data
@TableName("s2_tenant_subscription")
public class TenantSubscriptionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long planId;

    private String status;

    private Timestamp startDate;

    private Timestamp endDate;

    private String billingCycle;

    private Boolean autoRenew;

    private String paymentMethod;

    private String paymentReference;

    private Timestamp createdAt;

    private Timestamp updatedAt;
}
