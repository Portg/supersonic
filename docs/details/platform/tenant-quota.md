---
status: implemented
module: common, auth, headless
key-files:
  - common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaService.java
  - common/src/main/java/com/tencent/supersonic/common/quota/InMemoryTenantQuotaService.java
  - common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaFilter.java
  - headless/core/src/main/java/com/tencent/supersonic/headless/core/utils/SqlUtils.java
  - auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/persistence/repository/TenantQuotaRepository.java
---

# 租户并发配额与连接池隔离（P1-5）

## 目标
避免单个大租户打满全局 HikariCP/Druid 池。

## 实现
- 以租户 ID 为 key 的 `ConcurrentHashMap<Long, Semaphore>`。
- 公平信号量，`tryAcquire(timeoutMs)`，超时抛 `TooManyRequestsException`。
- 只在 `SqlUtils#queryInternal` 这个唯一汇聚点 acquire/release；HTTP 层不重复计数。
- 过滤器将异常转 HTTP 429 + `Retry-After`。
- `TenantContext` 为空时回退到 `s2.tenant.quota.fallback-tenant-id`。
- 单租户覆盖 `enabled=false` 表示该租户 bypass，不回退默认限额。
- Micrometer 指标 `s2_tenant_jdbc_permits_available` / `s2_tenant_jdbc_permits_waiting`。

## 配置
`s2.tenant.quota.enabled=true` 开启；默认值走 YAML，每租户覆盖走 `s2_tenant_quota`。

## 局限
计数器仅在单实例范围内生效；多实例未共享。后续可引入 Redis 令牌桶。
