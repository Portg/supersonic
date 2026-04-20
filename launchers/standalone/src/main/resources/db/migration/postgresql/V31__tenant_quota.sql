-- V31: Per-tenant concurrency quota overrides (PostgreSQL dialect).

CREATE TABLE IF NOT EXISTS s2_tenant_quota (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    jdbc_concurrent INT NOT NULL DEFAULT 10,
    llm_concurrent INT NOT NULL DEFAULT 5,
    monthly_query_count BIGINT NOT NULL DEFAULT 0,
    acquire_timeout_ms INT NOT NULL DEFAULT 2000,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_quota_tenant_id UNIQUE (tenant_id)
);

COMMENT ON TABLE s2_tenant_quota IS 'Per-tenant concurrency quota overrides';
COMMENT ON COLUMN s2_tenant_quota.jdbc_concurrent IS 'Max concurrent JDBC executions per instance';
COMMENT ON COLUMN s2_tenant_quota.llm_concurrent IS 'Max concurrent LLM calls per instance (reserved)';
COMMENT ON COLUMN s2_tenant_quota.monthly_query_count IS 'Max total queries per month (0 = unlimited)';
COMMENT ON COLUMN s2_tenant_quota.enabled IS 'true = enforce, false = bypass for this tenant';

INSERT INTO s2_permission (code, name, description, scope, type, path, status, created_by)
VALUES
('PLATFORM_QUOTA', '租户配额管理', '管理租户并发配额', 'PLATFORM', 'MENU', '/platform/tenant-quotas', 1, 'system')
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    scope = EXCLUDED.scope,
    type = EXCLUDED.type,
    path = EXCLUDED.path;

INSERT INTO s2_role_permission (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM s2_role r, s2_permission p
WHERE r.code = 'PLATFORM_SUPER_ADMIN' AND p.code = 'PLATFORM_QUOTA'
ON CONFLICT (role_id, permission_id) DO NOTHING;
