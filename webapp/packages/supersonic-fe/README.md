# Ant Design Pro

This project is initialized with [Ant Design Pro](https://pro.ant.design). Follow is the quick guide for how to use.

## Environment Prepare

使用 **Node.js 18+**（推荐与仓库 [`webapp/.nvmrc`](../../.nvmrc) 一致，例如 `nvm use`）。低于 18 时 Umi / ESLint 等可能无法正常运行。

Install `node_modules`:

```bash
npm install
```

or

```bash
yarn
```

## Provided Scripts

Ant Design Pro provides some useful script to help you quick start and build with web project, code style check and test.

Scripts provided in `package.json`. It's safe to modify or add additional script:

### Start project

```bash
npm start
```

### Build project

```bash
npm run build
```

### Check code style

```bash
npm run lint
```

You can also use script to auto fix some lint error:

```bash
npm run lint:fix
```

### Test code

```bash
npm test
```

## More

You can view full document on our [official website](https://pro.ant.design). And welcome any feedback in our [github](https://github.com/ant-design/ant-design-pro).

#### 踩坑

1.antd `Select`组件如果默认不选中时默认值不是`undefeated`，则不显示 placeholder

## Data Fetching with React Query

Since P2-10, all new remote reads go through **TanStack Query v5**. See:

- `src/utils/queryClient.ts` — shared `QueryClient` with `staleTime: 30s`, `refetchOnWindowFocus: false`, 4xx-skipping retry, and global `notification.error` for 5xx/network errors.
- `src/utils/queryKeys.ts` — typed `queryKeys` factory namespaced by `tenantId` from `localStorage`.
- `src/hooks/queries/<domain>.ts` — `useXxxQuery` / `useXxxMutation` hooks.
- `src/hooks/queries/README.md` — 4-step conversion recipe with before/after example.
- `src/components/QueryBoundary/index.tsx` — `<QueryBoundary>` for `Skeleton` / `Result` rendering.

DevTools are enabled automatically when `REACT_APP_ENV !== 'prod'`.

### Writing a new query

```ts
export function useMyListQuery(params: MyParams) {
  return useQuery({
    queryKey: queryKeys.myDomain.list(params),
    queryFn: () => getMyList(params),
    placeholderData: (prev) => prev,
  });
}
```

### Writing a new mutation

```ts
const qc = useQueryClient();
const save = useMutation({
  mutationFn: (v) => v.id ? update(v.id, v.values) : create(v.values),
  onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.myDomain.all() }),
});
```

DO NOT call `message.error` manually on failure — the global handler already does it. Opt out with `meta: { silent: true }` on the hook if a page needs inline error UI instead.

### Tests

Query hooks are tested with `@testing-library/react`'s `renderHook` wrapped in a fresh `QueryClient`. See `tests/unit/reportScheduleQuery.test.js` for the canonical pattern.

Run: `pnpm run test:unit` (jsdom + Jest via `scripts/run-unit-tests.cjs`).
