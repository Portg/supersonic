# Platform/Tenant RBAC 拆分迁移文档

## 概述

本次迁移将原有混合的系统管理菜单拆分为**平台管理 (Platform Admin)** 和 **租户管理 (Tenant Admin)** 两个独立的模块，实现清晰的权限边界划分。

## 架构设计

### 权限模型

```
┌─────────────────────────────────────────────────────────────────┐
│                        SuperSonic SaaS                          │
├─────────────────────────────────────────────────────────────────┤
│  平台级 (Platform Scope)              │  租户级 (Tenant Scope)   │
│  ─────────────────────                │  ────────────────────    │
│  • 租户管理                           │  • 组织架构管理           │
│  • 订阅计划管理                       │  • 成员管理              │
│  • 平台角色管理                       │  • 租户角色管理          │
│  • 平台权限管理                       │  • 租户权限管理          │
│  • 系统设置                           │  • 租户设置              │
│                                       │  • 用量统计              │
└─────────────────────────────────────────────────────────────────┘
```

### 菜单结构

| 一级菜单 | 二级菜单 | 路由 | 权限码 |
|---------|---------|------|--------|
| **平台管理** | 租户管理 | `/platform/tenants` | `PLATFORM_TENANT_MANAGE` |
| | 订阅计划 | `/platform/subscriptions` | `PLATFORM_SUBSCRIPTION` |
| | 平台角色 | `/platform/roles` | `PLATFORM_ROLE_MANAGE` |
| | 平台权限 | `/platform/permissions` | `PLATFORM_PERMISSION` |
| | 系统设置 | `/platform/settings` | `PLATFORM_SETTINGS` |
| **租户管理** | 组织架构 | `/tenant/organization` | `TENANT_ORG_MANAGE` |
| | 成员管理 | `/tenant/members` | `TENANT_MEMBER_MANAGE` |
| | 角色管理 | `/tenant/roles` | `TENANT_ROLE_MANAGE` |
| | 权限管理 | `/tenant/permissions` | `TENANT_PERMISSION` |
| | 租户设置 | `/tenant/settings` | `TENANT_SETTINGS` |
| | 用量统计 | `/tenant/usage` | `TENANT_USAGE_VIEW` |

## 变更清单

### 1. 前端路由配置

**文件**: `webapp/packages/supersonic-fe/config/routes.ts`

**变更内容**:
- 新增权限码常量，区分平台级和租户级
- 重构路由结构，拆分为 `/platform` 和 `/tenant` 两个顶级路由

```typescript
// 平台级权限
PLATFORM_ADMIN: 'PLATFORM_ADMIN',
PLATFORM_TENANT_MANAGE: 'PLATFORM_TENANT_MANAGE',
PLATFORM_SUBSCRIPTION: 'PLATFORM_SUBSCRIPTION',
PLATFORM_ROLE_MANAGE: 'PLATFORM_ROLE_MANAGE',
PLATFORM_PERMISSION: 'PLATFORM_PERMISSION',
PLATFORM_SETTINGS: 'PLATFORM_SETTINGS',

// 租户级权限
TENANT_ADMIN: 'TENANT_ADMIN',
TENANT_ORG_MANAGE: 'TENANT_ORG_MANAGE',
TENANT_MEMBER_MANAGE: 'TENANT_MEMBER_MANAGE',
TENANT_ROLE_MANAGE: 'TENANT_ROLE_MANAGE',
TENANT_PERMISSION: 'TENANT_PERMISSION',
TENANT_SETTINGS: 'TENANT_SETTINGS',
TENANT_USAGE_VIEW: 'TENANT_USAGE_VIEW',
```

### 2. 菜单国际化

**文件**: `webapp/packages/supersonic-fe/src/locales/zh-CN/menu.ts`

**新增翻译**:
```typescript
// 平台管理
'menu.platform': '平台管理',
'menu.platform.tenants': '租户管理',
'menu.platform.subscriptions': '订阅计划',
'menu.platform.platformRoles': '平台角色',
'menu.platform.platformPermissions': '平台权限',
'menu.platform.platformSettings': '系统设置',

// 租户管理
'menu.tenant': '租户管理',
'menu.tenant.organization': '组织架构',
'menu.tenant.members': '成员管理',
'menu.tenant.tenantRoles': '角色管理',
'menu.tenant.tenantPermissions': '权限管理',
'menu.tenant.tenantSettings': '租户设置',
'menu.tenant.usage': '用量统计',
```

### 3. 新增前端页面

#### 平台管理页面

| 页面 | 路径 | 功能 |
|------|------|------|
| TenantManagement | `pages/Platform/TenantManagement` | 租户 CRUD 管理 |
| SubscriptionManagement | `pages/Platform/SubscriptionManagement` | 订阅计划管理 |
| RoleManagement | `pages/Platform/RoleManagement` | 平台角色管理 |
| PermissionManagement | `pages/Platform/PermissionManagement` | 平台权限管理 |

#### 租户管理页面

| 页面 | 路径 | 功能 |
|------|------|------|
| MemberManagement | `pages/Tenant/MemberManagement` | 成员管理，分配组织和角色 |
| RoleManagement | `pages/Tenant/RoleManagement` | 租户角色管理 |
| PermissionManagement | `pages/Tenant/PermissionManagement` | 租户权限管理 |

### 4. 新增 API 服务

**文件**: `webapp/packages/supersonic-fe/src/services/platform.ts`

```typescript
// 租户管理
getAllTenants()
createTenant(data)
updateTenant(id, data)
deleteTenant(id)

// 订阅计划管理
getSubscriptionPlans()
createSubscriptionPlan(data)
updateSubscriptionPlan(id, data)
deleteSubscriptionPlan(id)

// 平台角色管理
getPlatformRoles()
createPlatformRole(data)
updatePlatformRole(id, data)
deletePlatformRole(id)
assignPermissionsToRole(roleId, permissionIds)

// 平台权限管理
getPlatformPermissions()
createPlatformPermission(data)
updatePlatformPermission(id, data)
deletePlatformPermission(id)
```

**文件**: `webapp/packages/supersonic-fe/src/services/tenant.ts` (新增部分)

```typescript
// 租户角色管理
getTenantRoles()
createTenantRole(data)
updateTenantRole(id, data)
deleteTenantRole(id)
assignPermissionsToTenantRole(roleId, permissionIds)
assignRoleToUser(data)

// 租户权限管理
getTenantPermissions()
createTenantPermission(data)
updateTenantPermission(id, data)
deleteTenantPermission(id)
```

### 5. 数据库迁移

**文件**:
- `db/migration/mysql/V9__platform_tenant_rbac.sql`
- `db/migration/postgresql/V9__platform_tenant_rbac.sql`

**变更内容**:

1. **s2_role 表** - 新增 `scope` 字段
   ```sql
   ALTER TABLE s2_role ADD COLUMN scope VARCHAR(20) DEFAULT 'TENANT';
   -- PLATFORM: 平台级角色
   -- TENANT: 租户级角色
   ```

2. **s2_permission 表** - 新增 `scope` 和 `type` 字段
   ```sql
   ALTER TABLE s2_permission ADD COLUMN scope VARCHAR(20) DEFAULT 'TENANT';
   ALTER TABLE s2_permission ADD COLUMN type VARCHAR(20) DEFAULT 'MENU';
   -- scope: PLATFORM/TENANT
   -- type: MENU/BUTTON/DATA/API
   ```

3. **预置权限数据**
   - 平台级权限: `PLATFORM_ADMIN`, `PLATFORM_TENANT_MANAGE`, `PLATFORM_SUBSCRIPTION`, `PLATFORM_ROLE_MANAGE`, `PLATFORM_PERMISSION`, `PLATFORM_SETTINGS`
   - 租户级权限: `TENANT_ADMIN`, `TENANT_ORG_MANAGE`, `TENANT_MEMBER_MANAGE`, `TENANT_ROLE_MANAGE`, `TENANT_PERMISSION`, `TENANT_SETTINGS`, `TENANT_USAGE_VIEW`

4. **预置角色数据**
   - 平台级角色: `platform_super_admin`, `platform_operator`
   - 租户级角色: `tenant_admin`, `analyst`, `viewer`

## 文件变更列表

### 新增文件

```
webapp/packages/supersonic-fe/
├── src/
│   ├── pages/
│   │   ├── Platform/
│   │   │   ├── TenantManagement/
│   │   │   │   ├── index.tsx
│   │   │   │   └── style.less
│   │   │   ├── SubscriptionManagement/
│   │   │   │   ├── index.tsx
│   │   │   │   └── style.less
│   │   │   ├── RoleManagement/
│   │   │   │   ├── index.tsx
│   │   │   │   └── style.less
│   │   │   └── PermissionManagement/
│   │   │       ├── index.tsx
│   │   │       └── style.less
│   │   └── Tenant/
│   │       ├── MemberManagement/
│   │       │   ├── index.tsx
│   │       │   └── style.less
│   │       ├── RoleManagement/
│   │       │   ├── index.tsx
│   │       │   └── style.less
│   │       └── PermissionManagement/
│   │           ├── index.tsx
│   │           └── style.less
│   └── services/
│       └── platform.ts

launchers/standalone/src/main/resources/db/migration/
├── mysql/
│   └── V9__platform_tenant_rbac.sql
└── postgresql/
    └── V9__platform_tenant_rbac.sql
```

### 修改文件

```
webapp/packages/supersonic-fe/
├── config/
│   └── routes.ts                    # 路由重构
└── src/
    ├── locales/zh-CN/
    │   └── menu.ts                  # 菜单翻译
    └── services/
        └── tenant.ts                # 新增租户角色权限API

launchers/standalone/src/main/resources/db/
├── schema-h2.sql                    # H2 主 Schema
├── schema-mysql.sql                 # MySQL 主 Schema
├── schema-postgres.sql              # PostgreSQL 主 Schema
├── data-h2.sql                      # H2 初始化数据
├── data-mysql.sql                   # MySQL 初始化数据
└── data-postgres.sql                # PostgreSQL 初始化数据
```

### 主 Schema 变更

**s2_role 表新增字段:**
```sql
`scope` VARCHAR(20) DEFAULT 'TENANT'  -- 作用域: PLATFORM/TENANT
```

**s2_permission 表新增字段:**
```sql
`scope` VARCHAR(20) DEFAULT 'TENANT'  -- 作用域: PLATFORM/TENANT
`created_by` VARCHAR(100) DEFAULT NULL -- 创建人
```

### 初始化数据变更

**s2_role 表新增数据:**
| ID | name | code | scope | tenant_id |
|----|------|------|-------|-----------|
| 1 | 系统管理员 | ADMIN | TENANT | 1 |
| 2 | 分析师 | ANALYST | TENANT | 1 |
| 3 | 查看者 | VIEWER | TENANT | 1 |
| 4 | 租户管理员 | TENANT_ADMIN | TENANT | 1 |
| 5 | SaaS管理员 | SAAS_ADMIN | PLATFORM | NULL |
| 6 | 平台超级管理员 | PLATFORM_SUPER_ADMIN | PLATFORM | NULL |
| 7 | 平台运营 | PLATFORM_OPERATOR | PLATFORM | NULL |

**s2_permission 表新增权限:**
| ID | code | scope | 说明 |
|----|------|-------|------|
| 100 | PLATFORM_ADMIN | PLATFORM | 平台管理入口 |
| 101 | PLATFORM_TENANT_MANAGE | PLATFORM | 管理所有租户 |
| 102 | PLATFORM_SUBSCRIPTION | PLATFORM | 管理订阅计划 |
| 103 | PLATFORM_ROLE_MANAGE | PLATFORM | 管理平台级角色 |
| 104 | PLATFORM_PERMISSION | PLATFORM | 管理平台级权限 |
| 105 | PLATFORM_SETTINGS | PLATFORM | 系统全局配置 |
| 110 | TENANT_ADMIN | TENANT | 租户管理入口 |
| 111 | TENANT_ORG_MANAGE | TENANT | 组织架构管理 |
| 112 | TENANT_MEMBER_MANAGE | TENANT | 成员管理 |
| 113 | TENANT_ROLE_MANAGE | TENANT | 角色管理 |
| 114 | TENANT_PERMISSION | TENANT | 权限管理 |
| 115 | TENANT_SETTINGS | TENANT | 租户设置 |
| 116 | TENANT_USAGE_VIEW | TENANT | 用量统计 |

**s2_role_permission 新增关联:**
- `PLATFORM_SUPER_ADMIN` (id=6) → 所有平台级权限 (100-105, 11, 40-43)
- `PLATFORM_OPERATOR` (id=7) → 部分平台权限 (100, 101, 102)
- `TENANT_ADMIN` (id=4) → 新增租户级权限 (110-116)
- `SAAS_ADMIN` (id=5) → 新增平台级权限 (100-105)

## 后端待实现

以下后端 API 需要对应实现:

### 平台管理 API (`/api/platform/`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/platform/tenants` | 获取所有租户列表 |
| POST | `/platform/tenants` | 创建租户 |
| PUT | `/platform/tenants/{id}` | 更新租户 |
| DELETE | `/platform/tenants/{id}` | 删除租户 |
| GET | `/platform/subscriptions` | 获取订阅计划列表 |
| POST | `/platform/subscriptions` | 创建订阅计划 |
| PUT | `/platform/subscriptions/{id}` | 更新订阅计划 |
| DELETE | `/platform/subscriptions/{id}` | 删除订阅计划 |
| GET | `/platform/roles` | 获取平台角色列表 |
| POST | `/platform/roles` | 创建平台角色 |
| PUT | `/platform/roles/{id}` | 更新平台角色 |
| DELETE | `/platform/roles/{id}` | 删除平台角色 |
| POST | `/platform/roles/{id}/permissions` | 为角色分配权限 |
| GET | `/platform/permissions` | 获取平台权限列表 |
| POST | `/platform/permissions` | 创建平台权限 |
| PUT | `/platform/permissions/{id}` | 更新平台权限 |
| DELETE | `/platform/permissions/{id}` | 删除平台权限 |

### 租户管理 API (`/api/tenant/`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/tenant/roles` | 获取当前租户角色列表 |
| POST | `/tenant/roles` | 创建租户角色 |
| PUT | `/tenant/roles/{id}` | 更新租户角色 |
| DELETE | `/tenant/roles/{id}` | 删除租户角色 |
| POST | `/tenant/roles/{id}/permissions` | 为角色分配权限 |
| POST | `/tenant/users/roles` | 为用户分配角色 |
| GET | `/tenant/permissions` | 获取当前租户权限列表 |
| POST | `/tenant/permissions` | 创建租户权限 |
| PUT | `/tenant/permissions/{id}` | 更新租户权限 |
| DELETE | `/tenant/permissions/{id}` | 删除租户权限 |

## 迁移步骤

1. **执行数据库迁移**
   ```bash
   # Flyway 会自动执行 V9__platform_tenant_rbac.sql
   ```

2. **部署后端代码**
   - 实现上述 API 端点
   - 更新权限拦截器，支持 scope 字段

3. **部署前端代码**
   - 新页面和路由已准备就绪

4. **验证**
   - 使用平台超级管理员账号验证平台管理功能
   - 使用租户管理员账号验证租户管理功能

## 权限验证流程

```
用户请求 → 权限拦截器
              │
              ├─ 检查用户角色
              │
              ├─ 获取角色的 scope (PLATFORM/TENANT)
              │
              ├─ 匹配请求的权限码
              │
              └─ 验证权限是否匹配
                    │
                    ├─ 平台级请求 → 需要 PLATFORM scope 的角色
                    │
                    └─ 租户级请求 → 需要 TENANT scope 的角色 + 租户ID匹配
```

## 注意事项

1. **数据隔离**: 租户级操作必须验证 `tenant_id` 匹配
2. **权限继承**: 平台超级管理员默认拥有所有平台权限
3. **租户初始化**: 新租户创建时自动创建默认角色和权限
4. **向后兼容**: 原有无 scope 的数据默认为 TENANT 级别

---

## 2024-01 Bug 修复与功能完善

### 1. 前端 API 响应处理修复

**问题**: 访问角色/权限管理页面时出现 `TypeError: Kn.some is not a function` 错误

**原因**: 后端 `ResponseAdvice` 自动将响应包装为 `ResultData`，但前端服务中的 `wrapResponse()` 函数又包装了一次，导致双重包装：
```javascript
// 错误的响应结构
{ code: 200, data: { code: 200, data: [...] } }
```

**修复文件**:
- `webapp/packages/supersonic-fe/src/services/platform.ts`
- `webapp/packages/supersonic-fe/src/services/tenant.ts`
- `webapp/packages/supersonic-fe/src/services/role.ts`

**修复内容**: 移除 `wrapResponse()` 辅助函数，直接返回请求结果

### 2. 角色表单完善

**问题**: 角色管理表单缺少作用域(scope)、类型(isSystem)、状态(status)字段

**修复文件**:
- `webapp/packages/supersonic-fe/src/pages/Platform/RoleManagement/index.tsx`
- `webapp/packages/supersonic-fe/src/pages/Tenant/RoleManagement/index.tsx`

**新增字段**:
```typescript
// 表单新增
<Form.Item label="作用域">
  <Select value="PLATFORM" disabled>
    <Select.Option value="PLATFORM">平台级</Select.Option>
  </Select>
</Form.Item>
<Form.Item name="isSystem" label="类型" valuePropName="checked">
  <Switch checkedChildren="系统" unCheckedChildren="自定义" />
</Form.Item>
<Form.Item name="status" label="状态" valuePropName="checked">
  <Switch checkedChildren="启用" unCheckedChildren="禁用" />
</Form.Item>
```

### 3. 角色权限配置加载

**问题**: 配置角色权限时不能预选当前已有权限

**新增 API**:
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/auth/role/{roleId}/permission-ids` | 获取角色的权限ID列表 |

**修复文件**:
- `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/rest/RoleController.java`
- `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/service/RoleService.java`
- `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/service/RoleServiceImpl.java`

**前端修改**: `handleConfigPermissions` 函数现在会先调用 API 加载当前权限

### 4. 成员管理功能完善

**问题**: 成员管理的组织、角色、状态显示不正确，分配组织/角色时不能预选当前值

#### 4.1 后端修改

**User.java 新增字段**:
```java
/** User status: 1=enabled, 0=disabled */
private Integer status;

/** Primary organization ID */
private Long organizationId;

/** Primary organization name */
private String organizationName;

/** Role IDs */
private List<Long> roleIds;

/** Role names */
private List<String> roleNames;
```

**新增 Mapper** - `UserRoleDOMapper.java`:
```java
@Mapper
public interface UserRoleDOMapper {
    @Select("SELECT role_id FROM s2_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT r.name FROM s2_role r INNER JOIN s2_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<String> selectRoleNamesByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM s2_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Insert("<script>INSERT INTO s2_user_role (user_id, role_id, created_at, created_by) VALUES "
            + "<foreach collection='roleIds' item='roleId' separator=','>"
            + "(#{userId}, #{roleId}, CURRENT_TIMESTAMP, #{createdBy})</foreach></script>")
    int batchInsert(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds, @Param("createdBy") String createdBy);
}
```

**DefaultUserAdaptor.convert() 更新**: 自动填充用户的组织和角色信息

**新增 API**:
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/user/role` | 为用户分配角色 |
| GET | `/api/auth/user/{userId}/role-ids` | 获取用户的角色ID列表 |

**UserService 新增方法**:
```java
void assignRolesToUser(Long userId, List<Long> roleIds, String operator);
List<Long> getUserRoleIds(Long userId);
```

#### 4.2 前端修改

**MemberManagement/index.tsx**:
- 更新 `Member` 接口，添加 `organizationId` 和 `roleIds` 字段
- `handleAssignOrg()` 预选当前组织
- `handleAssignRole()` 预选当前角色
- 表格列使用 `roleNames` 显示角色名称

### 5. 多租户 SQL 拦截器修复

**问题**: 用户登录时出现 SQL 语法错误
```sql
INNER JOIN s2_user_role ur ON rp.role_id = ur.role_id AND ur.tenant_id = 1 ON rp.role_id = ur.role_id
```

**原因**:
1. `s2_permission`、`s2_role_permission`、`s2_user_role` 表没有 `tenant_id` 列
2. `TenantSqlInterceptor` 错误地为这些表添加租户条件
3. JSQLParser 的 `setOnExpression` 导致重复 ON 子句

**修复文件**: `common/src/main/java/com/tencent/supersonic/common/config/TenantConfig.java`

**修复内容**: 更新 `excludedTables` 列表
```java
private List<String> excludedTables = Arrays.asList(
        // System-level tables
        "s2_tenant", "s2_subscription_plan",
        // RBAC association tables without tenant_id
        "s2_permission", "s2_role_permission", "s2_user_role");
```

### 新增文件清单

```
auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/persistence/mapper/
└── UserRoleDOMapper.java              # 用户-角色关联查询
```

### 修改文件清单

```
# 后端
auth/api/src/main/java/com/tencent/supersonic/auth/api/authentication/service/UserService.java
auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/adaptor/DefaultUserAdaptor.java
auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/rest/UserController.java
auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/rest/RoleController.java
auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/service/UserServiceImpl.java
auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/service/RoleService.java
auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/service/RoleServiceImpl.java
common/src/main/java/com/tencent/supersonic/common/pojo/User.java
common/src/main/java/com/tencent/supersonic/common/config/TenantConfig.java

# 前端
webapp/packages/supersonic-fe/src/services/platform.ts
webapp/packages/supersonic-fe/src/services/tenant.ts
webapp/packages/supersonic-fe/src/services/role.ts
webapp/packages/supersonic-fe/src/pages/Platform/RoleManagement/index.tsx
webapp/packages/supersonic-fe/src/pages/Tenant/RoleManagement/index.tsx
webapp/packages/supersonic-fe/src/pages/Tenant/MemberManagement/index.tsx
```

### 6. 语义模板权限控制修复

**问题**: 语义模板页面未对部署按钮、部署历史按钮和新建模板按钮进行权限控制，导致没有相应权限的用户（如 jack）也能看到这些操作按钮。

**原因分析**:
1. `SemanticTemplate/index.tsx` 页面只做了菜单级别的权限控制（`MENU_SEMANTIC_TEMPLATE`）
2. 但未对具体操作按钮进行 API 级别的权限检查

**修复文件**:
- `webapp/packages/supersonic-fe/config/routes.ts`
- `webapp/packages/supersonic-fe/src/pages/SemanticTemplate/index.tsx`

**修复内容**:

1. **routes.ts** - 新增语义模板操作权限码（与数据库 s2_permission 表对应）:
```typescript
// ========== 语义模板操作权限 ==========
API_TEMPLATE_VIEW: 'API_TEMPLATE_VIEW',       // 查看模板
API_TEMPLATE_CREATE: 'API_TEMPLATE_CREATE',   // 创建模板
API_TEMPLATE_UPDATE: 'API_TEMPLATE_UPDATE',   // 编辑模板
API_TEMPLATE_DELETE: 'API_TEMPLATE_DELETE',   // 删除模板
API_TEMPLATE_DEPLOY: 'API_TEMPLATE_DEPLOY',   // 部署模板
```

2. **SemanticTemplate/index.tsx** - 添加权限检查:
```typescript
import { ROUTE_AUTH_CODES } from '../../../../config/routes';

// 权限检查
const permissions = currentUser?.permissions || [];
const canDeploy = isSaasAdmin || permissions.includes(ROUTE_AUTH_CODES.API_TEMPLATE_DEPLOY);
const canCreate = isSaasAdmin || permissions.includes(ROUTE_AUTH_CODES.API_TEMPLATE_CREATE);

// 部署按钮
{canDeploy ? (
  <Button type="link" key="deploy" onClick={() => handleDeploy(template)}>
    <RocketOutlined /> 部署
  </Button>
) : (
  <span key="deploy" />
)}

// 部署历史按钮
{canDeploy && (
  <Button onClick={() => setHistoryVisible(true)}>
    <HistoryOutlined /> 部署历史
  </Button>
)}

// 新建模板按钮
{canCreate && (
  <Button type="primary" onClick={handleCreate}>
    <PlusOutlined /> 新建模板
  </Button>
)}
```

**相关数据库权限**（已存在于 s2_permission 表）:
| ID | 名称 | 权限码 | 类型 | 说明 |
|----|------|--------|------|------|
| 70 | 模板查看 | API_TEMPLATE_VIEW | API | 查看语义模板 |
| 71 | 模板创建 | API_TEMPLATE_CREATE | API | 创建语义模板 |
| 72 | 模板编辑 | API_TEMPLATE_UPDATE | API | 编辑语义模板 |
| 73 | 模板删除 | API_TEMPLATE_DELETE | API | 删除语义模板 |
| 74 | 模板部署 | API_TEMPLATE_DEPLOY | API | 部署语义模板 |

**配置说明**: 若要让用户看到部署按钮和部署历史，需在角色管理中为其角色分配 `API_TEMPLATE_DEPLOY` 权限；若要让用户创建模板，需分配 `API_TEMPLATE_CREATE` 权限。

### 7. 部署历史多租户隔离

**问题**: 部署历史页面没有区分管理员和普通用户，所有用户都只能看到当前租户的部署历史。

**需求**:
1. 管理员（superAdmin 或 isAdmin=1）可以查看所有租户的部署历史
2. 普通租户用户只能看本租户的部署历史

**修复文件**: `webapp/packages/supersonic-fe/src/pages/SemanticTemplate/DeployHistory.tsx`

**修复内容**:

1. 根据用户角色选择不同的 API：
```typescript
import { useModel } from '@umijs/max';
import { getDeploymentHistory, getAllDeploymentHistory, SemanticDeployment } from '@/services/semanticTemplate';

const { initialState } = useModel('@@initialState');
const currentUser = initialState?.currentUser;
const isSaasAdmin = currentUser?.superAdmin || currentUser?.isAdmin === 1;

// 管理员使用 getAllDeploymentHistory 查看所有租户的部署历史
// 普通用户使用 getDeploymentHistory 只看本租户的部署历史
const res = isSaasAdmin
  ? await getAllDeploymentHistory()
  : await getDeploymentHistory();
```

2. 为管理员添加"租户ID"列：
```typescript
const columns = [
  // 管理员可以看到租户ID列
  ...(isSaasAdmin
    ? [{ title: '租户ID', dataIndex: 'tenantId', key: 'tenantId', width: 80 }]
    : []),
  // ... 其他列
];
```

**后端 API**（已实现）:
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/semantic/v1/deployments` | 获取当前租户的部署历史 |
| GET | `/api/semantic/v1/admin/deployments` | 获取所有租户的部署历史（仅超级管理员）|

### 8. 成员角色分配显示数字问题

**问题**: 在成员管理页面分配角色时，对于同时拥有租户级角色和平台级角色的用户（如 admin），Select 组件显示角色名称和数字混合（如"系统管理员"和"6"）。

**原因分析**:
1. admin 用户拥有两个角色：ID=1（系统管理员，scope=TENANT）和 ID=6（平台超级管理员，scope=PLATFORM）
2. `getTenantRoles()` 只返回租户级角色（scope=TENANT），不包含 ID=6
3. Select 组件找不到 ID=6 的角色定义，直接显示数字

**修复文件**:
- `webapp/packages/supersonic-fe/src/pages/Tenant/MemberManagement/index.tsx`
- `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/persistence/mapper/UserRoleDOMapper.java`
- `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/service/UserServiceImpl.java`

**修复内容**:

1. **前端过滤**：在打开角色分配对话框时，只显示在租户角色列表中存在的角色ID，过滤掉平台级角色：
```typescript
const handleAssignRole = (member: Member) => {
  // 只显示在租户角色列表中存在的角色，过滤掉平台级角色
  if (member.roleIds && member.roleIds.length > 0) {
    const tenantRoleIds = roles.map((r) => r.id);
    const filteredRoleIds = member.roleIds.filter((id) => tenantRoleIds.includes(id));
    setSelectedRoles(filteredRoleIds);
  }
};
```

2. **后端保护**：新增 `deleteTenantRolesByUserId` 方法，只删除租户级角色，保留平台级角色：
```java
// UserRoleDOMapper.java
@Delete("DELETE FROM s2_user_role WHERE user_id = #{userId} AND role_id IN "
        + "(SELECT id FROM s2_role WHERE scope = 'TENANT' OR scope IS NULL)")
int deleteTenantRolesByUserId(@Param("userId") Long userId);

// UserServiceImpl.java
public void assignRolesToUser(Long userId, List<Long> roleIds, String operator) {
    // 只删除用户的租户级角色，保留平台级角色
    userRoleDOMapper.deleteTenantRolesByUserId(userId);
    if (!CollectionUtils.isEmpty(roleIds)) {
        userRoleDOMapper.batchInsert(userId, roleIds, operator);
    }
}
```

**设计原则**: 租户管理员只能管理租户级角色，不能看到或影响用户的平台级角色。平台级角色只能通过平台管理页面进行管理。
