---
status: active
module: webapp
key-files:
  - webapp/packages/supersonic-fe/src/utils/queryClient.ts
  - webapp/packages/supersonic-fe/src/utils/queryKeys.ts
  - webapp/packages/supersonic-fe/src/hooks/queries/*.ts
  - webapp/packages/supersonic-fe/src/components/QueryBoundary/index.tsx
  - webapp/packages/supersonic-fe/src/app.tsx (rootContainer)
---

# Frontend Data Layer (React Query)

## 边界

- **传输层** 仍然使用 `umi-request`（见 `src/services/request.ts`）。React Query 负责缓存、去重、重试、错误 UX；不替换 API 调用。
- **单向依赖**：`pages/*` → `hooks/queries/<domain>` → `services/<domain>` → `services/request`。`hooks/queries/*` 不允许直接调用 `request`，必须复用 service 导出的函数。
- **租户隔离**：Query keys 第一段为 `t:<tenantId>`，从 `localStorage.X-Tenant-Id` 读取（`queryKeys.ts:readTenantId`）。切换租户时推荐 reload，因为 umi-request 的拦截器基于 `localStorage` 在请求时读取。

## 主链路

```
<QueryClientProvider>                      // src/app.tsx rootContainer
    │
    └── page component
            │
            ├── useXxxQuery()              // src/hooks/queries/<domain>.ts
            │     └── queryFn: getXxxList() // src/services/<domain>.ts
            │           └── request(...)    // src/services/request.ts (umi-request + headers)
            │
            └── useXxxMutation()
                  ├── mutationFn: updateXxx()
                  └── onSuccess/onMutate → queryClient.invalidateQueries / setQueryData
```

全局错误处理：`QueryCache.onError` + `MutationCache.onError` 调用 antd `notification.error`；401/403 由 `responseInterceptor` 直接重定向到 `/login`，不重复弹窗。

## 默认值

| 配置 | 值 | 原因 |
|------|-----|------|
| `staleTime` | 30s | 业务工具，非实时看板；避免重复请求 |
| `gcTime` | 5min | 默认 |
| `refetchOnWindowFocus` | false | 避免后台标签页切回时打断用户 |
| `retry` | 一次，跳过 4xx | 4xx 是业务错误，重试无意义 |
| Devtools | `REACT_APP_ENV !== 'prod'` | 生产环境剥离 |

## 迁移策略

见 `docs/superpowers/plans/2026-04-17-p2-10-rollout.md`。按页逐个迁移，每页一次 commit，可单独回滚。

## 测试

- 单测：`renderHook` + `QueryClientProvider` wrapper；在 hook 层 mock `src/services/*`。
- 代表性测试：`tests/unit/reportScheduleQuery.test.js`、`tests/unit/scheduleOptimisticDelete.test.js`。
- 多 hook 共享同一 `QueryClient` 时，必须使用同一 `renderHook` 调用（同一 React 树），否则跨 React 根的 `act` 无法同步状态更新。

## 注意事项

- **不要** 在页面里再写 `try/catch` + `message.error`——全局 handler 已处理。若确需页面内错误 UI，在 hook 调用处传 `meta: { silent: true }` 并自行处理 `query.error`。
- **不要** 把 `queryClient` 直接 import 到组件里手动 `fetchQuery`；使用 hook。
- **不要** 在 mutation 成功后手动 `refetch`——改用 `invalidateQueries`。
- **优化更新** 只在"用户会注意延迟"的地方使用（如列表删除）。一般更新保持简单 invalidate。
- SSE / 流式响应（如 `ChatPage`）不适合 React Query，保持现有模式。
