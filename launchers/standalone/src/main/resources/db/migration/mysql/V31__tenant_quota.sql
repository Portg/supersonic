-- V31: Per-tenant concurrency quota overrides.
-- Default quota falls back to s2.tenant.quota.default.* in application.yaml when a tenant has no row.

CREATE TABLE IF NOT EXISTS `s2_tenant_quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL COMMENT 'FK to s2_tenant.id',
    `jdbc_concurrent` INT NOT NULL DEFAULT 10 COMMENT 'Max concurrent JDBC executions per instance',
    `llm_concurrent` INT NOT NULL DEFAULT 5 COMMENT 'Max concurrent LLM calls per instance (reserved; future use)',
    `monthly_query_count` BIGINT NOT NULL DEFAULT 0 COMMENT 'Max total queries per month (0 = unlimited; future use)',
    `acquire_timeout_ms` INT NOT NULL DEFAULT 2000 COMMENT 'Max wait for a JDBC permit before 429',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 = enforce, 0 = bypass for this tenant',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_quota_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Per-tenant concurrency quota overrides';

INSERT INTO `s2_permission` (`code`, `name`, `description`, `scope`, `type`, `path`, `status`, `created_by`)
VALUES
('PLATFORM_QUOTA', '租户配额管理', '管理租户并发配额', 'PLATFORM', 'MENU', '/platform/tenant-quotas', 1, 'system')
AS new_val ON DUPLICATE KEY UPDATE
    `name` = new_val.`name`,
    `description` = new_val.`description`,
    `scope` = new_val.`scope`,
    `type` = new_val.`type`,
    `path` = new_val.`path`;

INSERT INTO `s2_role_permission` (`role_id`, `permission_id`, `created_by`)
SELECT r.id, p.id, 'system'
FROM `s2_role` r, `s2_permission` p
WHERE r.code = 'PLATFORM_SUPER_ADMIN' AND p.code = 'PLATFORM_QUOTA'
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;
