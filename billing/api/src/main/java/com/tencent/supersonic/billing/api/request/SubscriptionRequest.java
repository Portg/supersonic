package com.tencent.supersonic.billing.api.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for subscription operations. Used for both self-service subscription changes and
 * admin subscription assignments.
 */
@Data
public class SubscriptionRequest {

    /**
     * The subscription plan ID to subscribe to.
     */
    @NotNull(message = "planId is required")
    private Long planId;

    /**
     * Billing cycle: MONTHLY or YEARLY. Defaults to MONTHLY if not specified.
     */
    private String billingCycle = "MONTHLY";
}
