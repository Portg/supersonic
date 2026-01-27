# 语义模板 (Semantic Template) 使用指南

## 目录

- [功能概述](#功能概述)
- [核心概念](#核心概念)
- [快速开始](#快速开始)
- [API 接口文档](#api-接口文档)
- [模板配置说明](#模板配置说明)
- [前端页面使用](#前端页面使用)
- [数据库设计](#数据库设计)
- [常见问题](#常见问题)

---

## 功能概述

语义模板是 SuperSonic 提供的一键部署功能，允许用户通过预定义的模板快速创建完整的语义层结构，包括：

| 组件 | 说明 |
|------|------|
| Domain | 主题域，用于组织和管理语义模型 |
| Model | 语义模型，定义维度、度量和标识符 |
| Model Relations | 模型关系，定义模型间的关联关系 |
| Metrics | 指标，基于度量自动创建 |
| Dimensions | 维度，用于数据分析的切分角度 |
| DataSet | 数据集，聚合多个模型供查询使用 |
| Terms | 术语，定义业务词汇和别名 |
| Agent | 智能助手，可在 Chat 中直接使用 |

### 模板类型

- **内置模板**: 系统预置的 Demo 模板，所有租户可用
- **自定义模板**: 租户创建的专属模板，仅本租户可见

### 模板状态

| 状态 | 值 | 说明 |
|------|-----|------|
| 草稿 | 0 | 新建模板的初始状态，可编辑/删除 |
| 已部署 | 1 | 部署成功后的状态，不可编辑/删除 |

---

## 核心概念

### 多租户隔离

| 数据类型 | 隔离策略 |
|---------|---------|
| 内置模板 | `is_builtin=1`，所有租户可见、可使用 |
| 自定义模板 | `is_builtin=0, tenant_id=X`，仅本租户可见 |
| 部署记录 | 按 `tenant_id` 严格隔离 |
| 部署创建的对象 | 自动继承当前租户 ID |

### 权限控制

| 角色 | 权限 |
|-----|------|
| SaaS 管理员 | 管理内置模板，查看所有租户部署记录 |
| 租户管理员 | 使用内置模板，管理本租户自定义模板 |
| 普通用户 | 使用模板部署（需授权） |

---

## 快速开始

### 1. 访问模板页面

```
浏览器访问: http://your-domain/semantic-template
```

### 2. 选择模板并部署

1. 在「内置模板」或「自定义模板」区域选择一个模板
2. 点击「部署」按钮
3. 选择目标数据库
4. 填写配置参数（如表名等）
5. 勾选「自动创建 Agent」（推荐）
6. 点击「部署」执行

### 3. 在 Chat 中使用

部署成功后：
1. 访问 `/chat` 页面
2. 在 Agent 下拉列表中选择刚创建的 Agent
3. 开始自然语言查询

---

## API 接口文档

### 基础路径

```
/api/semantic/v1
```

### 模板管理 API

#### 获取模板列表

```http
GET /api/semantic/v1/templates
```

**响应示例:**
```json
{
  "code": 200,
  "data": {
    "builtinTemplates": [
      {
        "id": 1,
        "name": "访问统计模板",
        "bizName": "visits_template",
        "category": "VISITS",
        "status": 1,
        "isBuiltin": true
      }
    ],
    "customTemplates": [
      {
        "id": 2,
        "name": "我的模板",
        "bizName": "my_template",
        "category": "CUSTOM",
        "status": 0,
        "isBuiltin": false
      }
    ]
  }
}
```

#### 获取模板详情

```http
GET /api/semantic/v1/templates/{id}
```

#### 创建模板

```http
POST /api/semantic/v1/templates
Content-Type: application/json

{
  "name": "电商分析模板",
  "bizName": "ecommerce_template",
  "description": "用于电商数据分析",
  "category": "ECOMMERCE",
  "templateConfig": {
    "domain": { ... },
    "models": [ ... ],
    "dataSet": { ... }
  }
}
```

#### 更新模板 (PATCH)

```http
PATCH /api/semantic/v1/templates/{id}
Content-Type: application/json

{
  "name": "更新后的名称",
  "description": "更新后的描述"
}
```

> 注意: 只有草稿状态 (status=0) 的自定义模板可以编辑

#### 删除模板

```http
DELETE /api/semantic/v1/templates/{id}
```

> 注意: 只有草稿状态的自定义模板可以删除

### 部署 API

#### 预览部署

```http
POST /api/semantic/v1/templates/{id}:preview
Content-Type: application/json

{
  "databaseId": 1,
  "params": {
    "table_user": "s2_user_department",
    "table_pv_uv": "s2_pv_uv_statis"
  }
}
```

**响应示例:**
```json
{
  "code": 200,
  "data": {
    "domain": {
      "name": "产品数据域",
      "bizName": "supersonic"
    },
    "models": [
      { "name": "用户模型", "bizName": "user" }
    ],
    "agent": {
      "name": "产品助手",
      "examples": ["查询今日访问量"]
    }
  }
}
```

#### 执行部署

```http
POST /api/semantic/v1/templates/{id}:deploy
Content-Type: application/json

{
  "databaseId": 1,
  "params": {
    "table_user": "s2_user_department",
    "table_pv_uv": "s2_pv_uv_statis"
  }
}
```

**响应示例:**
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "templateId": 1,
    "templateName": "访问统计模板",
    "status": "SUCCESS",
    "resultDetail": {
      "domainId": 1,
      "domainName": "产品数据域",
      "models": [
        { "id": 1, "name": "用户模型", "bizName": "user" }
      ],
      "dataSetId": 1,
      "dataSetName": "产品数据集",
      "agentConfig": {
        "name": "产品助手",
        "description": "用于分析产品访问数据",
        "dataSetId": 1,
        "examples": ["查询今日访问量"]
      }
    }
  }
}
```

### 部署历史 API

#### 获取部署历史

```http
GET /api/semantic/v1/deployments
```

#### 获取部署详情

```http
GET /api/semantic/v1/deployments/{id}
```

### 管理员 API

#### 创建/更新内置模板

```http
POST /api/semantic/v1/admin/templates:builtin
Content-Type: application/json

{
  "name": "访问统计模板",
  "bizName": "visits_template",
  "category": "VISITS",
  "templateConfig": { ... }
}
```

#### 获取所有租户部署历史

```http
GET /api/semantic/v1/admin/deployments
```

---

## 模板配置说明

### SemanticTemplateConfig 完整结构

```json
{
  "domain": {
    "name": "主题域名称",
    "bizName": "domain_biz_name",
    "description": "描述",
    "viewers": ["user1"],
    "admins": ["admin1"],
    "isOpen": 0
  },
  "models": [
    {
      "name": "模型名称",
      "bizName": "model_biz_name",
      "description": "模型描述",
      "tableName": "${table_name}",
      "sqlQuery": "SELECT * FROM ${table_name}",
      "identifiers": [
        {
          "name": "用户ID",
          "bizName": "user_id",
          "fieldName": "user_id",
          "type": "primary"
        }
      ],
      "dimensions": [
        {
          "name": "日期",
          "bizName": "imp_date",
          "type": "partition_time",
          "expr": "imp_date"
        },
        {
          "name": "部门",
          "bizName": "department",
          "type": "categorical"
        }
      ],
      "measures": [
        {
          "name": "访问次数",
          "bizName": "pv",
          "aggOperator": "SUM",
          "expr": "pv",
          "createMetric": true
        }
      ]
    }
  ],
  "modelRelations": [
    {
      "fromModelBizName": "user",
      "toModelBizName": "visit",
      "joinType": "left join",
      "joinConditions": [
        {
          "leftField": "user_id",
          "rightField": "user_id",
          "operator": "EQUALS"
        }
      ]
    }
  ],
  "dataSet": {
    "name": "数据集名称",
    "bizName": "dataset_biz_name",
    "description": "数据集描述"
  },
  "agent": {
    "name": "智能助手名称",
    "description": "助手描述",
    "enableSearch": true,
    "examples": [
      "查询今日访问量",
      "按部门统计访问次数"
    ]
  },
  "terms": [
    {
      "name": "PV",
      "description": "页面浏览量",
      "alias": ["访问量", "浏览量"]
    }
  ],
  "configParams": [
    {
      "key": "table_name",
      "name": "数据表名",
      "type": "TABLE",
      "defaultValue": "s2_pv_uv_statis",
      "required": true,
      "description": "主数据表名称"
    }
  ]
}
```

### 参数占位符

模板配置中可以使用 `${paramKey}` 格式的占位符，在部署时会被替换为实际值：

```json
{
  "tableName": "${table_pv_uv}",
  "sqlQuery": "SELECT * FROM ${table_pv_uv} WHERE date >= '${start_date}'"
}
```

部署时传入参数：
```json
{
  "params": {
    "table_pv_uv": "my_pv_table",
    "start_date": "2024-01-01"
  }
}
```

### 维度类型

| 类型 | 说明 |
|------|------|
| `categorical` | 分类维度（默认） |
| `time` | 时间维度 |
| `partition_time` | 分区时间维度 |

### 聚合操作符

| 操作符 | 说明 |
|--------|------|
| `SUM` | 求和 |
| `AVG` | 平均值 |
| `COUNT` | 计数 |
| `MAX` | 最大值 |
| `MIN` | 最小值 |
| `COUNT_DISTINCT` | 去重计数 |

---

## 前端页面使用

### 页面路由

| 路由 | 说明 | 权限 |
|------|------|------|
| `/semantic-template` | 模板列表页 | `MENU_SEMANTIC_TEMPLATE` |

### 页面组件结构

```
SemanticTemplate/
├── index.tsx              # 模板列表主页面
├── DeployModal.tsx        # 部署配置弹窗
├── PreviewDrawer.tsx      # 预览抽屉
├── DeployHistory.tsx      # 部署历史组件
├── TemplateFormModal.tsx  # 模板创建/编辑弹窗
└── style.less             # 样式文件
```

### TypeScript 类型定义

```typescript
// services/semanticTemplate.ts

export interface SemanticTemplate {
  id: number;
  name: string;
  bizName: string;
  description?: string;
  category: string;
  templateConfig: SemanticTemplateConfig;
  previewImage?: string;
  status: number;           // 0=草稿, 1=已部署
  isBuiltin: boolean;
  tenantId: number;
  createdAt?: string;
  createdBy?: string;
}

export interface SemanticDeployParam {
  databaseId: number;
  params: Record<string, string>;
}

export interface SemanticDeployment {
  id: number;
  templateId: number;
  templateName?: string;
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';
  resultDetail?: SemanticDeployResult;
  errorMessage?: string;
  startTime?: string;
  endTime?: string;
  tenantId: number;
}
```

### 辅助函数

```typescript
import { TEMPLATE_STATUS, canEditTemplate, canDeleteTemplate } from '@/services/semanticTemplate';

// 检查模板是否可编辑
if (canEditTemplate(template)) {
  // 允许编辑
}

// 检查模板是否可删除
if (canDeleteTemplate(template)) {
  // 允许删除
}
```

---

## 数据库设计

### s2_semantic_template 表

```sql
CREATE TABLE s2_semantic_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    biz_name VARCHAR(100) NOT NULL COMMENT '模板代码',
    description VARCHAR(500) COMMENT '模板描述',
    category VARCHAR(50) NOT NULL COMMENT '模板类别',
    template_config LONGTEXT NOT NULL COMMENT 'JSON: 模板配置',
    preview_image VARCHAR(500) COMMENT '预览图URL',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-草稿 1-已部署',
    is_builtin TINYINT DEFAULT 0 COMMENT '是否内置: 0-否 1-是',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_biz_name (tenant_id, biz_name)
);
```

### s2_semantic_deployment 表

```sql
CREATE TABLE s2_semantic_deployment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_id BIGINT NOT NULL COMMENT '模板ID',
    template_name VARCHAR(100) COMMENT '模板名称快照',
    database_id BIGINT COMMENT '目标数据库ID',
    param_config TEXT COMMENT 'JSON: 部署参数',
    status VARCHAR(20) NOT NULL COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    result_detail LONGTEXT COMMENT 'JSON: 部署结果详情',
    error_message TEXT COMMENT '错误信息',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    PRIMARY KEY (id),
    KEY idx_template_id (template_id),
    KEY idx_tenant_id (tenant_id)
);
```

---

## 常见问题

### Q: 部署失败怎么办？

1. 检查目标数据库连接是否正常
2. 检查配置参数中的表名是否存在
3. 查看部署历史中的错误信息
4. 检查用户是否有足够的权限

### Q: 为什么看不到自定义模板？

自定义模板只对创建它的租户可见。请确保：
1. 登录的用户属于正确的租户
2. 模板的 `tenant_id` 与当前用户的租户 ID 一致

### Q: 如何手动创建 Agent？

如果部署时未勾选「自动创建 Agent」，可以手动创建：

1. 访问 `/agent` 页面
2. 点击「新建 Agent」
3. 在工具配置中添加 DataSet 类型的工具
4. 选择部署创建的数据集

### Q: 已部署的模板能修改吗？

不能。模板部署后状态变为「已部署」(status=1)，无法编辑或删除。
如需修改，建议：
1. 创建新的自定义模板
2. 或直接在语义模型页面修改已创建的对象

### Q: 内置模板从哪里来？

内置模板由系统在启动时自动初始化，配置来源于 Demo 类（如 `S2VisitsDemo`）。
SaaS 管理员可以通过 `/api/semantic/v1/admin/templates:builtin` 接口管理内置模板。

---

## 更新日志

| 版本 | 日期 | 更新内容 |
|------|------|---------|
| 1.0.0 | 2024-01 | 初始版本，支持模板管理和一键部署 |
| 1.1.0 | 2024-01 | 增加自动创建 Agent 功能 |
| 1.2.0 | 2024-01 | API 路径重构为 RESTful 风格 |
