package com.tencent.supersonic.auth.api.authentication.service;

import com.tencent.supersonic.auth.api.authentication.pojo.TenantUsage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for tracking tenant usage.
 */
public interface UsageTrackingService {

    /**
     * Record an API call for a tenant.
     *
     * @param tenantId the tenant ID
     */
    void recordApiCall(Long tenantId);

    /**
     * Record token usage for a tenant.
     *
     * @param tenantId the tenant ID
     * @param tokenCount the number of tokens used
     */
    void recordTokenUsage(Long tenantId, long tokenCount);

    /**
     * Record a query for a tenant.
     *
     * @param tenantId the tenant ID
     */
    void recordQuery(Long tenantId);

    /**
     * Record storage usage for a tenant.
     *
     * @param tenantId the tenant ID
     * @param bytes the number of bytes
     */
    void recordStorageUsage(Long tenantId, long bytes);

    /**
     * Record active user for a tenant.
     *
     * @param tenantId the tenant ID
     */
    void recordActiveUser(Long tenantId);

    /**
     * Get usage for a tenant on a specific date.
     *
     * @param tenantId the tenant ID
     * @param date the date
     * @return the usage if exists
     */
    Optional<TenantUsage> getUsage(Long tenantId, LocalDate date);

    /**
     * Get today's usage for a tenant.
     *
     * @param tenantId the tenant ID
     * @return the usage for today
     */
    TenantUsage getTodayUsage(Long tenantId);

    /**
     * Get usage for a tenant over a date range.
     *
     * @param tenantId the tenant ID
     * @param startDate the start date
     * @param endDate the end date
     * @return list of usage records
     */
    List<TenantUsage> getUsageRange(Long tenantId, LocalDate startDate, LocalDate endDate);

    /**
     * Get total API calls for a tenant today.
     *
     * @param tenantId the tenant ID
     * @return the API call count
     */
    int getTodayApiCalls(Long tenantId);

    /**
     * Get total tokens used for a tenant this month.
     *
     * @param tenantId the tenant ID
     * @return the token count
     */
    long getMonthlyTokenUsage(Long tenantId);

    /**
     * Get aggregate usage for a tenant over a month.
     *
     * @param tenantId the tenant ID
     * @param year the year
     * @param month the month
     * @return aggregated usage
     */
    TenantUsage getMonthlyUsage(Long tenantId, int year, int month);
}
