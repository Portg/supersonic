# Billing 模块实现变更说明

## 1. 概述

将订阅管理功能从 `auth` 模块中拆分，独立为 `billing` 模块，同时修复了聊天对话的多租户隔离缺陷和语义模板的权限控制缺陷。

### 变更范围

| 变更类型 | 涉及模块 | 说明 |
|---------|---------|------|
| 新增模块 | `billing/api`, `billing/server` | 订阅计划 & 租户订阅管理 |
| 接口迁移 | `auth` → `billing` | 订阅相关代码从 auth 迁出 |
| 缺陷修复 | `chat` | 聊天对话多租户隔离 |
| 缺陷修复 | `webapp/SemanticTemplate` | 编辑/删除权限检查缺失 |
| 数据库迁移 | `db/migration/V10` | API 路径更新 & 权限记录 |
| 前端适配 | `webapp` | 订阅 API 路径切换、权限控制 |

---

## 2. Billing 模块架构

### 2.1 模块结构

```
billing/
├── pom.xml
├── api/                              # 接口层（POJO + Service 接口）
│   ├── pom.xml
│   └── src/main/java/.../billing/api/
│       ├── pojo/
│       │   ├── SubscriptionPlan.java       # 订阅计划
│       │   └── TenantSubscription.java     # 租户订阅
│       ├── request/
│       │   └── SubscriptionRequest.java    # 请求 DTO
│       └── service/
│           └── SubscriptionService.java    # 服务接口
└── server/                           # 实现层（Controller + Service 实现 + 持久层）
    ├── pom.xml
    └── src/main/java/.../billing/server/
        ├── persistence/
        │   ├── dataobject/
        │   │   ├── SubscriptionPlanDO.java
        │   │   └── TenantSubscriptionDO.java
        │   └── mapper/
        │       ├── SubscriptionPlanDOMapper.java
        │       └── TenantSubscriptionDOMapper.java
        ├── rest/
        │   ├── SubscriptionPlanController.java
        │   └── TenantSubscriptionController.java
        └── service/impl/
            └── SubscriptionServiceImpl.java
```

### 2.2 依赖关系

```
billing-api
├── spring-boot-starter (provided)
├── lombok
├── jakarta-validation-api
└── common

billing-server
├── billing-api
├── spring-boot-starter-jdbc
├── spring-boot-starter-web
├── common
├── auth-api               (TenantContext)
└── spring-security-core   (scope: provided, 仅编译时用于 @PreAuthorize)
```

> **注意**：`spring-security-core` 使用 `provided` scope，不会触发 Spring Security 自动配置（避免覆盖原有登录页面）。运行时由 auth 模块提供安全上下文。

### 2.3 父 POM 注册

```xml
<!-- /pom.xml -->
<modules>
    <module>common</module>
    <module>auth</module>
    <module>billing</module>   <!-- 新增 -->
    <module>chat</module>
    <module>headless</module>
    <module>launchers</module>
</modules>

<!-- /launchers/standalone/pom.xml -->
<dependency>
    <groupId>com.tencent.supersonic</groupId>
    <artifactId>billing-server</artifactId>
    <version>${project.version}</version>
</dependency>
```

---

## 3. REST API 设计

遵循 Google RESTful API 设计规范，URL 路径包含 API 版本号 `/v1/`。

### 3.1 订阅计划管理

**Base Path**: `/api/v1/subscription-plans`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/subscription-plans` | 公开 | 获取激活的计划列表 |
| GET | `/api/v1/subscription-plans/all` | ADMIN / PLATFORM_ADMIN | 获取所有计划（含非激活） |
| GET | `/api/v1/subscription-plans/{planId}` | 公开 | 获取指定计划详情 |
| GET | `/api/v1/subscription-plans/default` | 公开 | 获取默认计划 |
| POST | `/api/v1/subscription-plans` | ADMIN / PLATFORM_ADMIN | 创建计划 |
| PUT | `/api/v1/subscription-plans/{planId}` | ADMIN / PLATFORM_ADMIN | 更新计划 |
| DELETE | `/api/v1/subscription-plans/{planId}` | ADMIN / PLATFORM_ADMIN | 删除计划（软删除） |

### 3.2 租户订阅 — 自助服务

**Base Path**: `/api/v1/my-subscription`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/my-subscription` | 获取当前租户的订阅 |
| GET | `/api/v1/my-subscription/history` | 获取订阅历史 |
| PUT | `/api/v1/my-subscription` | 更改订阅计划 |
| DELETE | `/api/v1/my-subscription` | 取消订阅 |

**请求体**（PUT）：

```json
{
  "planId": 2,
  "billingCycle": "MONTHLY"
}
```

### 3.3 租户订阅 — 管理员操作

**Base Path**: `/api/v1/tenants/{tenantId}/subscription`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/tenants/{tenantId}/subscription` | ADMIN / PLATFORM_ADMIN | 获取指定租户的订阅 |
| GET | `/api/v1/tenants/{tenantId}/subscriptions` | ADMIN / PLATFORM_ADMIN | 获取订阅历史 |
| PUT | `/api/v1/tenants/{tenantId}/subscription` | ADMIN / PLATFORM_ADMIN | 分配订阅 |
| DELETE | `/api/v1/tenants/{tenantId}/subscription` | ADMIN / PLATFORM_ADMIN | 取消订阅 |

---

## 4. 数据库变更

### 4.1 数据表

**`s2_subscription_plan`** — 订阅计划表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| name | VARCHAR(100) | 计划名称 |
| code | VARCHAR(50) UNIQUE | 计划编码（FREE / BASIC / PRO / ENTERPRISE） |
| price_monthly | DECIMAL(10,2) | 月付价格 |
| price_yearly | DECIMAL(10,2) | 年付价格 |
| max_users | INT | 最大用户数（-1=无限） |
| max_datasets | INT | 最大数据集数 |
| max_models | INT | 最大模型数 |
| max_agents | INT | 最大智能体数 |
| max_api_calls_per_day | INT | 每日 API 调用上限 |
| max_tokens_per_month | BIGINT | 每月 Token 上限 |
| features | TEXT | 特性列表（JSON） |
| is_default | TINYINT | 是否默认计划 |
| status | VARCHAR(20) | 状态（ACTIVE / INACTIVE / DELETED） |

**`s2_tenant_subscription`** — 租户订阅表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| tenant_id | BIGINT | 租户 ID |
| plan_id | BIGINT | 计划 ID |
| status | VARCHAR(20) | 状态（ACTIVE / CANCELLED） |
| start_date | DATETIME | 开始日期 |
| end_date | DATETIME | 到期日期 |
| billing_cycle | VARCHAR(20) | 计费周期（MONTHLY / YEARLY） |
| auto_renew | TINYINT | 是否自动续费 |

### 4.2 默认计划数据

| ID | 编码 | 名称 | 月付 | 用户上限 | 数据集 | 模型 | 智能体 | API/日 | Token/月 |
|----|------|------|------|---------|--------|------|--------|--------|---------|
| 1 | FREE | 免费版 | $0 | 3 | 5 | 10 | 2 | 1,000 | 100K |
| 2 | BASIC | 基础版 | $29 | 10 | 20 | 50 | 10 | 10,000 | 1M |
| 3 | PRO | 专业版 | $99 | 50 | 100 | 200 | 50 | 100,000 | 10M |
| 4 | ENTERPRISE | 企业版 | $299 | 无限 | 无限 | 无限 | 无限 | 无限 | 无限 |

### 4.3 迁移脚本 (V10)

路径：`db/migration/{mysql,postgresql}/V10__billing_api_paths.sql`

- 更新 `API_ADMIN_SUBSCRIPTION` 权限路径：`/api/auth/admin/subscription` → `/api/v1/subscription-plans`
- 新增权限：`API_ADMIN_TENANT_SUBSCRIPTION`（管理员租户订阅）
- 新增权限：`API_MY_SUBSCRIPTION`（自助订阅管理）

---

## 5. 前端变更

### 5.1 订阅 API 服务

新增 `src/services/subscription.ts`，所有 API 统一使用 `/api/v1/` 前缀。

```typescript
// 计划列表（公开）
getSubscriptionPlans()                        // GET /api/v1/subscription-plans

// 计划管理（管理员）
createSubscriptionPlan(data)                  // POST /api/v1/subscription-plans
updateSubscriptionPlan(planId, data)          // PUT  /api/v1/subscription-plans/{planId}
deleteSubscriptionPlan(planId)                // DELETE /api/v1/subscription-plans/{planId}

// 自助订阅
getCurrentSubscription()                      // GET    /api/v1/my-subscription
changeSubscription(data)                      // PUT    /api/v1/my-subscription
cancelSubscription()                          // DELETE /api/v1/my-subscription

// 管理员租户订阅
getTenantSubscription(tenantId)               // GET /api/v1/tenants/{tenantId}/subscription
updateTenantSubscription(tenantId, data)      // PUT /api/v1/tenants/{tenantId}/subscription
```

### 5.2 租户设置页面

`src/pages/TenantSettings/index.tsx` 新增：

- 当前订阅信息展示（计划名称、状态、计费周期、到期时间）
- "更改计划"弹窗（Radio 选择新计划）
- "取消订阅"确认操作

---

## 6. 缺陷修复

### 6.1 聊天对话多租户隔离

**问题**：`s2_chat` 表有 `tenant_id` 字段，但查询/创建/修改/删除操作未使用该字段。不同租户的同名用户（如都叫 `tom`）能看到彼此的对话。

**涉及文件**：

| 文件 | 变更 |
|------|------|
| `ChatMapper.java` | INSERT 加入 `tenant_id`；SELECT/UPDATE/DELETE 加入 `AND tenant_id = #{tenantId}` |
| `ChatRepository.java` | 接口方法增加 `Long tenantId` 参数 |
| `ChatRepositoryImpl.java` | 实现层传递 `tenantId` |
| `ChatManageService.java` | 方法签名从 `String userName` 改为 `User user` |
| `ChatManageServiceImpl.java` | 创建对话写入 `tenantId`；新增 `getEffectiveTenantId(User)` |
| `ChatController.java` | 直接传递 `User` 对象 |

**租户 ID 解析优先级**：

```
1. TenantContext.getTenantId()    ← 拦截器注入
2. user.getTenantId()             ← 用户对象
3. DEFAULT_TENANT_ID (1L)        ← 兜底
```

### 6.2 语义模板权限控制

**问题**：`canEditTemplate()` / `canDeleteTemplate()` 仅检查模板状态（是否草稿、是否内置），未检查用户权限（`API_TEMPLATE_UPDATE` / `API_TEMPLATE_DELETE`）。VIEWER 角色的用户也能看到编辑/删除按钮。

**修复**（`src/pages/SemanticTemplate/index.tsx`）：

```typescript
// 修复前：仅模板状态检查
const canEdit = canEditTemplate(template);

// 修复后：模板状态 + 用户权限双重检查
const canUpdate = isSaasAdmin || permissions.includes(ROUTE_AUTH_CODES.API_TEMPLATE_UPDATE);
const canDeletePerm = isSaasAdmin || permissions.includes(ROUTE_AUTH_CODES.API_TEMPLATE_DELETE);
const canEdit = canEditTemplate(template) && canUpdate;
const canDelete = canDeleteTemplate(template) && canDeletePerm;
```

无操作权限时，菜单项直接不显示（而非 disabled）。

---

## 7. 权限矩阵

### 7.1 订阅相关

| 操作 | VIEWER | ANALYST | ADMIN | TENANT_ADMIN | SAAS_ADMIN |
|------|--------|---------|-------|-------------|------------|
| 查看计划列表 | - | - | 可 | 可 | 可 |
| 管理计划 CRUD | - | - | 可 | - | 可 |
| 查看自己的订阅 | - | - | 可 | 可 | 可 |
| 更改/取消自己的订阅 | - | - | 可 | 可 | 可 |
| 管理租户订阅 | - | - | 可 | - | 可 |

### 7.2 语义模板

| 操作 | VIEWER | ANALYST | ADMIN | TENANT_ADMIN | SAAS_ADMIN |
|------|--------|---------|-------|-------------|------------|
| 查看模板 | 不可见 | 可 | 可 | 可 | 可 |
| 创建模板 | - | 不可 | 可 | 可 | 可 |
| 编辑模板 | - | 不可 | 可 | 可 | 可 |
| 删除模板 | - | 不可 | 可 | 可 | 可 |
| 部署模板 | - | 可 | 可 | 可 | 可 |

---

## 8. 兼容性说明

| 项目 | 说明 |
|------|------|
| 旧数据兼容 | `s2_chat` 中已有记录的 `tenant_id` 默认为 1，新记录自动写入正确的租户 ID |
| API 路径 | 旧路径 `/api/auth/subscription/*` 废弃，新路径 `/api/v1/subscription-plans` 和 `/api/v1/my-subscription` |
| 数据库迁移 | V10 脚本自动更新权限记录中的路径 |
| Spring Security | `billing-server` 仅引入 `spring-security-core`（provided scope），不触发自动配置 |
