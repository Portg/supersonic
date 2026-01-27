SuperSonic的WebApp采用前后端分离架构，前端基于React构建，提供Chat BI和Headless BI两大核心功能的用户界面。

### 架构概览

WebApp主要由三个核心模块组成：

1. **supersonic-fe** - 主前端应用，基于React + Umi框架构建
2. **chat-sdk** - 聊天功能SDK，提供对话式查询的组件和API服务
3. **WebConfig** - Spring Boot Web配置，处理静态资源和路由 

### 主要功能模块

#### 1. 用户认证界面

- 登录页面提供用户名/密码认证 
- 支持用户注册功能
- 默认管理员账户：admin/123456

#### 2. 聊天查询系统

- **ChatItem组件**：管理完整的查询生命周期，包括解析、执行、结果展示 
- **ExecuteItem组件**：处理查询执行和结果展示，支持表格/图表切换 
- **流式响应**：支持大模型生成的流式文本总结

#### 3. API服务层

- **chatExecute()**：执行解析后的查询 
- **getExecuteSummary()**：获取查询结果的AI总结 
- **switchEntity()**：切换查询实体

### 技术特性

#### 1. 多环境支持

- 开发环境：`npm run start:osdev` (端口9000)
- 生产环境：`npm run build:os`
- 内部版本：`npm run build:inner`

#### 2. 组件化设计

- 基于Ant Design Pro组件库
- 模块化的聊天SDK，支持独立集成
- 响应式布局，支持移动端

#### 3. 实时交互

- 支持流式响应，实时显示AI生成内容
- 查询结果缓存机制
- 多轮对话上下文管理

### 部署架构

WebApp作为静态资源由Spring Boot服务提供：

- 静态资源路径：`/webapp/**` → `classpath:/webapp/`
- 根路径重定向：`/` → `/webapp/`
- SPA路由支持：所有非资源请求转发到index.html

开发模式下执行命令：sh ./start-fe-dev.sh

等任务启动完成后，在浏览器输入localhost:9000查看页面