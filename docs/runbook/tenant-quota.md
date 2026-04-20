# Runbook: Per-Tenant Concurrency Quota

## Feature flag

`s2.tenant.quota.enabled` — default **false**. Set to `true` in profile YAML or via env:

```
S2_TENANT_QUOTA_ENABLED=true
S2_TENANT_QUOTA_JDBC=10
S2_TENANT_QUOTA_LLM=5
S2_TENANT_QUOTA_ACQUIRE_MS=2000
```

## Per-tenant overrides

Insert/upsert rows in `s2_tenant_quota`, or use admin REST:

```
PUT /api/v1/admin/tenant-quotas/{tenantId}
{
  "jdbcConcurrent": 20,
  "acquireTimeoutMs": 3000,
  "enabled": true
}
```

Live refresh is automatic (controller calls `TenantQuotaService.refresh(tenantId)`).

## Symptoms

- 429 responses with `Retry-After` header → tenant has exceeded its cap.
- Metric `s2_tenant_jdbc_permits_waiting{tenantId=X}` > 0 consistently → permanent saturation; raise quota or investigate slow queries.
- Metric `s2_tenant_jdbc_permits_available{tenantId=X}` == 0 for >1 minute → starvation; check for hanging queries.

## Rollback

Set `s2.tenant.quota.enabled=false` and restart. All permits become no-op; no DB rollback needed.

## Limitations

- **Per-instance counters.** In a multi-pod deployment each pod enforces its own semaphore. A tenant with capacity 10 and 3 pods can achieve up to 30 global concurrent queries. Future: Redis-backed counters (out of scope for MVP).
- `llm_concurrent` column exists but is not yet wired (reserved for follow-up work).
- `monthly_query_count` is advisory only — not enforced in this MVP.

## Observability

Prometheus metrics:
- `s2_tenant_jdbc_permits_available{tenantId="X"}`
- `s2_tenant_jdbc_permits_waiting{tenantId="X"}`
- `s2_tenant_quota_known_tenants`

Recommended alert:
```
alert: TenantQuotaStarvation
expr: s2_tenant_jdbc_permits_available == 0 for 2m
```
