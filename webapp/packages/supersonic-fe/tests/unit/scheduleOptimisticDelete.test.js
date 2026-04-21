/**
 * @jest-environment jsdom
 */
const React = require('react');
const { QueryClient, QueryClientProvider } = require('@tanstack/react-query');
const { renderHook, waitFor, act } = require('@testing-library/react');

jest.mock('../../.tmp-unit/services/reportSchedule.js', () => ({
  getScheduleList: jest.fn(),
  getValidDataSetList: jest.fn(),
  createSchedule: jest.fn(),
  updateSchedule: jest.fn(),
  deleteSchedule: jest.fn(),
  pauseSchedule: jest.fn(),
  resumeSchedule: jest.fn(),
  triggerSchedule: jest.fn(),
}), { virtual: true });
jest.mock('../../.tmp-unit/services/deliveryConfig.js', () => ({ getConfigList: jest.fn() }), { virtual: true });

const svc = require('../../.tmp-unit/services/reportSchedule.js');
const {
  useScheduleDeleteMutation,
  useScheduleListQuery,
  useScheduleSaveMutation,
  useScheduleToggleMutation,
} = require('../../.tmp-unit/hooks/queries/reportSchedule.js');
const { queryKeys } = require('../../.tmp-unit/utils/queryKeys.js');

function wrap(client) {
  return ({ children }) => React.createElement(QueryClientProvider, { client }, children);
}

describe('optimistic delete', () => {
  afterEach(() => {
    // Prevent tenant leaking between tests: tests 3-5 write X-Tenant-Id to localStorage.
    localStorage.removeItem('X-Tenant-Id');
    jest.clearAllMocks();
  });
  it('removes row immediately and keeps it removed on success', async () => {
    // First call: initial load; second call: refetch after onSettled invalidation (delete confirmed).
    svc.getScheduleList
      .mockResolvedValueOnce({ records: [{ id: 1, name: 'a' }, { id: 2, name: 'b' }], total: 2 })
      .mockResolvedValueOnce({ records: [{ id: 2, name: 'b' }], total: 1 });
    svc.deleteSchedule.mockResolvedValue(undefined);
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const { result } = renderHook(
      () => ({ list: useScheduleListQuery({ current: 1, pageSize: 20 }), del: useScheduleDeleteMutation() }),
      { wrapper: wrap(client) },
    );
    await waitFor(() => expect(result.current.list.isSuccess).toBe(true));

    await act(async () => { await result.current.del.mutateAsync(1); });
    await waitFor(() =>
      expect(result.current.list.data.records.find((r) => r.id === 1)).toBeUndefined(),
    );
  });

  it('rolls back on failure', async () => {
    svc.getScheduleList.mockResolvedValue({ records: [{ id: 1, name: 'a' }, { id: 2, name: 'b' }], total: 2 });
    svc.deleteSchedule.mockRejectedValue(new Error('nope'));
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const { result } = renderHook(
      () => ({ list: useScheduleListQuery({ current: 1, pageSize: 20 }), del: useScheduleDeleteMutation() }),
      { wrapper: wrap(client) },
    );
    await waitFor(() => expect(result.current.list.isSuccess).toBe(true));

    await act(async () => {
      try { await result.current.del.mutateAsync(1); } catch (_) {}
    });
    await waitFor(() => {
      expect(result.current.list.data.records.find((r) => r.id === 1)).toBeDefined();
    });
  });

  // Tests 3-5 use mutate() (fire-and-forget) rather than mutateAsync() so that
  // the deleteSchedule / createSchedule / pauseSchedule promise can be resolved
  // manually, letting us assert intermediate (optimistic) cache state before the
  // server responds. mutateAsync() would block the await and prevent mid-flight checks.
  it('only updates caches that actually contain the deleted row', async () => {
    localStorage.setItem('X-Tenant-Id', '1');
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const page1Key = queryKeys.reportSchedule.list({ current: 1, pageSize: 20 });
    const page2Key = queryKeys.reportSchedule.list({ current: 2, pageSize: 20 });
    client.setQueryData(page1Key, { records: [{ id: 1, name: 'a' }, { id: 2, name: 'b' }], total: 3 });
    client.setQueryData(page2Key, { records: [{ id: 3, name: 'c' }], total: 3 });

    let resolveDelete;
    svc.deleteSchedule.mockReturnValue(new Promise((resolve) => { resolveDelete = resolve; }));
    const { result } = renderHook(() => ({ del: useScheduleDeleteMutation() }), {
      wrapper: wrap(client),
    });

    act(() => {
      result.current.del.mutate(1);
    });

    await waitFor(() => {
      expect(client.getQueryData(page1Key)).toEqual({
        records: [{ id: 2, name: 'b' }],
        total: 2,
      });
    });
    expect(client.getQueryData(page2Key)).toEqual({
      records: [{ id: 3, name: 'c' }],
      total: 3,
    });

    await act(async () => { resolveDelete(); });
  });

  it('keeps invalidation scoped to the tenant where the mutation started', async () => {
    localStorage.setItem('X-Tenant-Id', '7');
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const tenant7Key = queryKeys.reportSchedule.list({ current: 1, pageSize: 20 });
    client.setQueryData(tenant7Key, { records: [{ id: 1, name: 'a' }], total: 1 });

    localStorage.setItem('X-Tenant-Id', '8');
    const tenant8Key = queryKeys.reportSchedule.list({ current: 1, pageSize: 20 });
    client.setQueryData(tenant8Key, { records: [{ id: 9, name: 'z' }], total: 1 });

    localStorage.setItem('X-Tenant-Id', '7');
    const cancelSpy = jest.spyOn(client, 'cancelQueries');
    const invalidateSpy = jest.spyOn(client, 'invalidateQueries');
    let resolveDelete;
    svc.deleteSchedule.mockReturnValue(new Promise((resolve) => { resolveDelete = resolve; }));
    const { result } = renderHook(() => ({ del: useScheduleDeleteMutation() }), {
      wrapper: wrap(client),
    });

    act(() => {
      result.current.del.mutate(1);
    });

    await waitFor(() => {
      expect(svc.deleteSchedule).toHaveBeenCalledWith(1);
    });
    localStorage.setItem('X-Tenant-Id', '8');
    await act(async () => { resolveDelete(); });

    await waitFor(() => {
      expect(cancelSpy).toHaveBeenCalledWith({ queryKey: ['t:7', 'reportSchedule'] });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['t:7', 'reportSchedule'] });
    });
    expect(cancelSpy).not.toHaveBeenCalledWith({ queryKey: ['t:8', 'reportSchedule'] });
    expect(invalidateSpy).not.toHaveBeenCalledWith({ queryKey: ['t:8', 'reportSchedule'] });
  });

  it('save mutation invalidates the tenant that initiated the request', async () => {
    localStorage.setItem('X-Tenant-Id', '11');
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const tenant11ListKey = queryKeys.reportSchedule.list({ current: 1, pageSize: 20 });
    const tenant11DeliveryKey = queryKeys.deliveryConfig.list({ pageNum: 1, pageSize: 100 });
    client.setQueryData(tenant11ListKey, { records: [{ id: 1, name: 'a' }], total: 1 });
    client.setQueryData(tenant11DeliveryKey, { 3: { id: 3, name: 'x' } });

    localStorage.setItem('X-Tenant-Id', '12');
    const tenant12ListKey = queryKeys.reportSchedule.list({ current: 1, pageSize: 20 });
    const tenant12DeliveryKey = queryKeys.deliveryConfig.list({ pageNum: 1, pageSize: 100 });
    client.setQueryData(tenant12ListKey, { records: [{ id: 2, name: 'b' }], total: 1 });
    client.setQueryData(tenant12DeliveryKey, { 4: { id: 4, name: 'y' } });

    localStorage.setItem('X-Tenant-Id', '11');
    const invalidateSpy = jest.spyOn(client, 'invalidateQueries');
    let resolveSave;
    svc.createSchedule.mockReturnValue(new Promise((resolve) => { resolveSave = resolve; }));
    const { result } = renderHook(() => ({ save: useScheduleSaveMutation() }), {
      wrapper: wrap(client),
    });

    act(() => {
      result.current.save.mutate({ values: { name: 'new schedule' } });
    });

    await waitFor(() => {
      expect(svc.createSchedule).toHaveBeenCalledWith({ name: 'new schedule' });
    });
    localStorage.setItem('X-Tenant-Id', '12');
    await act(async () => { resolveSave({ id: 100 }); });

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['t:11', 'reportSchedule'] });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['t:11', 'deliveryConfig'] });
    });
    expect(invalidateSpy).not.toHaveBeenCalledWith({ queryKey: ['t:12', 'reportSchedule'] });
    expect(invalidateSpy).not.toHaveBeenCalledWith({ queryKey: ['t:12', 'deliveryConfig'] });
  });

  it('toggle mutation invalidates the tenant that initiated the request', async () => {
    localStorage.setItem('X-Tenant-Id', '5');
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const tenant5Key = queryKeys.reportSchedule.list({ current: 1, pageSize: 20 });
    client.setQueryData(tenant5Key, { records: [{ id: 1, name: 'a', enabled: true }], total: 1 });

    localStorage.setItem('X-Tenant-Id', '6');
    const tenant6Key = queryKeys.reportSchedule.list({ current: 1, pageSize: 20 });
    client.setQueryData(tenant6Key, { records: [{ id: 9, name: 'z', enabled: true }], total: 1 });

    localStorage.setItem('X-Tenant-Id', '5');
    const invalidateSpy = jest.spyOn(client, 'invalidateQueries');
    let resolveToggle;
    svc.pauseSchedule.mockReturnValue(new Promise((resolve) => { resolveToggle = resolve; }));
    const { result } = renderHook(() => ({ toggle: useScheduleToggleMutation() }), {
      wrapper: wrap(client),
    });

    act(() => {
      result.current.toggle.mutate({ id: 1, enabled: false }); // false → pause
    });

    await waitFor(() => {
      expect(svc.pauseSchedule).toHaveBeenCalledWith(1);
    });
    localStorage.setItem('X-Tenant-Id', '6'); // simulate tenant switch while in-flight
    await act(async () => { resolveToggle(); });

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['t:5', 'reportSchedule'] });
    });
    expect(invalidateSpy).not.toHaveBeenCalledWith({ queryKey: ['t:6', 'reportSchedule'] });
  });
});
