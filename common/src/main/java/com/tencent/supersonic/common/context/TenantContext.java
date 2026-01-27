package com.tencent.supersonic.common.context;

/**
 * TenantContext holds the current tenant information in a ThreadLocal. This allows tenant-specific
 * data isolation in multi-tenant environments.
 */
public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * Sets the current tenant ID for the executing thread.
     *
     * @param tenantId the tenant ID to set
     */
    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Gets the current tenant ID from the executing thread.
     *
     * @return the current tenant ID, or null if not set
     */
    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Gets the current tenant ID, returning a default value if not set.
     *
     * @param defaultTenantId the default tenant ID to return if not set
     * @return the current tenant ID or the default value
     */
    public static Long getTenantIdOrDefault(Long defaultTenantId) {
        Long tenantId = CURRENT_TENANT.get();
        return tenantId != null ? tenantId : defaultTenantId;
    }

    /**
     * Checks if a tenant context is currently set.
     *
     * @return true if tenant context is set, false otherwise
     */
    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Clears the current tenant context from the executing thread. Should be called after request
     * processing is complete to prevent memory leaks.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
