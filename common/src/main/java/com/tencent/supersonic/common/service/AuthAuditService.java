package com.tencent.supersonic.common.service;

import com.tencent.supersonic.common.pojo.enums.AuthChangeType;

/**
 * Service interface for auditing authorization changes.
 */
public interface AuthAuditService {

    /**
     * Logs an authorization change event.
     *
     * @param changeType Type of change (CREATE, UPDATE, DELETE, GRANT, REVOKE)
     * @param resourceType Type of resource (MODEL, DATASET)
     * @param resourceId ID of the resource
     * @param groupId ID of the auth group
     * @param operator User who made the change
     * @param oldValue Previous value (JSON string, null for CREATE)
     * @param newValue New value (JSON string, null for DELETE)
     * @param description Additional description
     */
    void logAuthChange(AuthChangeType changeType, String resourceType, Long resourceId,
            Integer groupId, String operator, String oldValue, String newValue, String description);

    /**
     * Logs a row permission access event.
     *
     * @param resourceType Type of resource
     * @param resourceId ID of the resource
     * @param user User who accessed
     * @param filters Applied row filters
     */
    void logRowPermissionAccess(String resourceType, Long resourceId, String user, String filters);

    /**
     * Logs a column permission access event for sensitive fields.
     *
     * @param resourceType Type of resource
     * @param resourceId ID of the resource
     * @param user User who accessed
     * @param sensitiveFields Fields accessed
     * @param sensitiveLevel Sensitivity level (MID/HIGH)
     */
    void logSensitiveFieldAccess(String resourceType, Long resourceId, String user,
            String sensitiveFields, String sensitiveLevel);
}
