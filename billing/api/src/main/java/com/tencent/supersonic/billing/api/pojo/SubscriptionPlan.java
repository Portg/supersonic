package com.tencent.supersonic.billing.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Subscription plan entity representing different pricing tiers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {

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

    /**
     * Check if this is a free plan
     */
    public boolean isFree() {
        return "FREE".equals(code)
                || (priceMonthly != null && priceMonthly.compareTo(BigDecimal.ZERO) == 0);
    }

    /**
     * Check if a limit is unlimited (-1 means unlimited)
     */
    public boolean isUnlimited(Integer limit) {
        return limit == null || limit == -1;
    }
}
