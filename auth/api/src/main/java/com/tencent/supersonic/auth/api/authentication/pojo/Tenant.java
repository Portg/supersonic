package com.tencent.supersonic.auth.api.authentication.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Tenant entity representing a tenant in the multi-tenant system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

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

    /**
     * Check if a resource limit is unlimited (-1 means unlimited)
     */
    public boolean isUnlimited(Integer limit) {
        return limit == null || limit == -1;
    }

    /**
     * Check if token limit is unlimited
     */
    public boolean hasUnlimitedTokens() {
        return maxTokensPerMonth == null || maxTokensPerMonth == -1;
    }
}
