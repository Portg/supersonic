# React Query Conversion Recipe

Migrate one `useEffect`-fetch at a time. Never rewrite the service layer.

## 4-Step Recipe

### 1. Leave the service function alone

Example service already in `src/services/reportSchedule.ts`:
```ts
export function getScheduleList(params: { current?: number; pageSize?: number }) {
  return request(BASE, { method: 'GET', params });
}
```

### 2. Create a `useXxxQuery` hook under `src/hooks/queries/<domain>.ts`

```ts
import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/utils/queryKeys';
import { getScheduleList } from '@/services/reportSchedule';

export function useScheduleListQuery(params: { current: number; pageSize: number }) {
  return useQuery({
    queryKey: queryKeys.reportSchedule.list(params),
    queryFn: () => getScheduleList(params),
    placeholderData: (prev) => prev, // keep last page while new page loads
  });
}
```

### 3. Replace `useEffect` + `useState` in the page

BEFORE:
```tsx
const [data, setData] = useState([]);
const [loading, setLoading] = useState(false);
useEffect(() => { (async () => {
  setLoading(true);
  try { setData((await getScheduleList({ current: 1, pageSize: 20 })).records); }
  finally { setLoading(false); }
})(); }, []);
```

AFTER:
```tsx
const [pagination, setPagination] = useState({ current: 1, pageSize: 20 });
const { data: resp, isLoading, isFetching } = useScheduleListQuery(pagination);
const rows = resp?.records ?? [];
```

### 4. Render loading/error consistently

Use `<QueryBoundary query={query}>` or pass `loading={isLoading}` to antd `Table`. Global errors are already surfaced by `queryClient`'s `QueryCache.onError` — DO NOT also call `message.error` locally unless the hook opts out via `meta: { silent: true }`.

## Mutation Recipe

```ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/utils/queryKeys';
import { createSchedule, updateSchedule } from '@/services/reportSchedule';

export function useScheduleSaveMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id?: number; values: Partial<ReportSchedule> }) =>
      v.id ? updateSchedule(v.id, v.values) : createSchedule(v.values),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.reportSchedule.all() }),
  });
}
```

## Using QueryBoundary for non-Table contents

```tsx
const q = useScheduleDetailQuery(id);
return <QueryBoundary query={q}><ScheduleDetailView data={q.data} /></QueryBoundary>;
```
