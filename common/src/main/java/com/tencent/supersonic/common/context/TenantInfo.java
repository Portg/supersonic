package com.tencent.supersonic.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TenantInfo holds detailed information about the current tenant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantInfo {

    /**
     * Unique tenant identifier
     */
    private Long id;

    /**
     * Tenant name
     */
    private String name;

    /**
     * Tenant code (unique business identifier)
     */
    private String code;

    /**
     * Subscription plan type (FREE, BASIC, PRO, ENTERPRISE)
     */
    private String planType;

    /**
     * Tenant status (ACTIVE, SUSPENDED, DELETED)
     */
    private String status;
}
