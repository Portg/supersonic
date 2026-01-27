package com.tencent.supersonic.billing.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Tenant subscription entity representing a tenant's subscription to a plan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSubscription {

    private Long id;

    private Long tenantId;

    private Long planId;

    private String planName;

    private String status;

    private Timestamp startDate;

    private Timestamp endDate;

    private String billingCycle;

    private Boolean autoRenew;

    private String paymentMethod;

    private String paymentReference;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    /**
     * Check if subscription is active
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    /**
     * Check if subscription has expired
     */
    public boolean isExpired() {
        return endDate != null && endDate.before(new Timestamp(System.currentTimeMillis()));
    }
}
