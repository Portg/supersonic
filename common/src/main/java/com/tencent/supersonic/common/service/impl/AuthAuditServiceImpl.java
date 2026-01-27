package com.tencent.supersonic.common.service.impl;

import com.tencent.supersonic.common.pojo.enums.AuthChangeType;
import com.tencent.supersonic.common.service.AuthAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of AuthAuditService that logs authorization changes. Uses structured logging
 * format for easy parsing by log aggregation systems.
 */
@Slf4j
@Service
public class AuthAuditServiceImpl implements AuthAuditService {

    private static final String AUTH_CHANGE_LOG_PREFIX = "[AUTH_AUDIT]";
    private static final String ACCESS_LOG_PREFIX = "[ACCESS_AUDIT]";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void logAuthChange(AuthChangeType changeType, String resourceType, Long resourceId,
            Integer groupId, String operator, String oldValue, String newValue,
            String description) {
        String timestamp = LocalDateTime.now().format(FORMATTER);

        // Structured log format for easy parsing
        log.info(
                "{} timestamp={} changeType={} resourceType={} resourceId={} groupId={} "
                        + "operator={} description=\"{}\" oldValue={} newValue={}",
                AUTH_CHANGE_LOG_PREFIX, timestamp, changeType.getCode(), resourceType, resourceId,
                groupId, operator, description != null ? description : "",
                oldValue != null ? oldValue : "null", newValue != null ? newValue : "null");
    }

    @Override
    public void logRowPermissionAccess(String resourceType, Long resourceId, String user,
            String filters) {
        String timestamp = LocalDateTime.now().format(FORMATTER);

        log.info(
                "{} timestamp={} eventType=ROW_PERMISSION_APPLIED resourceType={} resourceId={} "
                        + "user={} filters={}",
                ACCESS_LOG_PREFIX, timestamp, resourceType, resourceId, user,
                filters != null ? filters : "none");
    }

    @Override
    public void logSensitiveFieldAccess(String resourceType, Long resourceId, String user,
            String sensitiveFields, String sensitiveLevel) {
        String timestamp = LocalDateTime.now().format(FORMATTER);

        log.info(
                "{} timestamp={} eventType=SENSITIVE_FIELD_ACCESS resourceType={} resourceId={} "
                        + "user={} sensitiveLevel={} fields={}",
                ACCESS_LOG_PREFIX, timestamp, resourceType, resourceId, user, sensitiveLevel,
                sensitiveFields);
    }
}
