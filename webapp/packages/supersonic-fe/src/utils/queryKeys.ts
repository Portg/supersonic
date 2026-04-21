function readTenantId(defaultId = 1): number {
  try {
    const raw = localStorage.getItem('X-Tenant-Id');
    if (raw) { const n = parseInt(raw, 10); return isNaN(n) ? defaultId : n; }
  } catch { /* non-browser env */ }
  return defaultId;
}

function tenantNs(tenantId?: number): readonly [string] {
  const id = tenantId ?? readTenantId(1);
  return [`t:${id}`] as const;
}

export function createQueryKeys(tenantId?: number) {
  const ns = tenantNs(tenantId);
  return {
    reportSchedule: {
      all: () => [...ns, 'reportSchedule'] as const,
      list: (params: { current?: number; pageSize?: number; datasetId?: number; enabled?: boolean }) =>
        [...ns, 'reportSchedule', 'list', params] as const,
      detail: (id: number) => [...ns, 'reportSchedule', 'detail', id] as const,
      executions: (scheduleId: number, params?: Record<string, unknown>) =>
        [...ns, 'reportSchedule', 'executions', scheduleId, params ?? {}] as const,
    },
    deliveryConfig: {
      all: () => [...ns, 'deliveryConfig'] as const,
      list: (params: { pageNum?: number; pageSize?: number }) =>
        [...ns, 'deliveryConfig', 'list', params] as const,
    },
    dataSet: {
      all: () => [...ns, 'dataSet'] as const,
      valid: () => [...ns, 'dataSet', 'valid'] as const,
      schema: (dataSetId: number) => [...ns, 'dataSet', 'schema', dataSetId] as const,
    },
    taskCenter: {
      all: () => [...ns, 'taskCenter'] as const,
      alertRules: (params: Record<string, unknown>) => [...ns, 'taskCenter', 'alertRules', params] as const,
      alertEvents: (params: Record<string, unknown>) => [...ns, 'taskCenter', 'alertEvents', params] as const,
      exportTasks: (params: Record<string, unknown>) => [...ns, 'taskCenter', 'exportTasks', params] as const,
    },
  } as const;
}

/** Default singleton — resolves tenant lazily at call time. */
export const queryKeys = {
  reportSchedule: {
    all: () => createQueryKeys().reportSchedule.all(),
    list: (p: Parameters<ReturnType<typeof createQueryKeys>['reportSchedule']['list']>[0]) =>
      createQueryKeys().reportSchedule.list(p),
    detail: (id: number) => createQueryKeys().reportSchedule.detail(id),
    executions: (scheduleId: number, p?: Record<string, unknown>) =>
      createQueryKeys().reportSchedule.executions(scheduleId, p),
  },
  deliveryConfig: {
    all: () => createQueryKeys().deliveryConfig.all(),
    list: (p: Parameters<ReturnType<typeof createQueryKeys>['deliveryConfig']['list']>[0]) =>
      createQueryKeys().deliveryConfig.list(p),
  },
  dataSet: {
    all: () => createQueryKeys().dataSet.all(),
    valid: () => createQueryKeys().dataSet.valid(),
    schema: (id: number) => createQueryKeys().dataSet.schema(id),
  },
  taskCenter: {
    all: () => createQueryKeys().taskCenter.all(),
    alertRules: (p: Record<string, unknown>) => createQueryKeys().taskCenter.alertRules(p),
    alertEvents: (p: Record<string, unknown>) => createQueryKeys().taskCenter.alertEvents(p),
    exportTasks: (p: Record<string, unknown>) => createQueryKeys().taskCenter.exportTasks(p),
  },
} as const;

/** Type helper: extract key tuple type for typing custom hooks. */
export type QueryKeyOf<Fn extends (...args: any[]) => readonly unknown[]> = ReturnType<Fn>;
