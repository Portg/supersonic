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
} = require('../../.tmp-unit/hooks/queries/reportSchedule.js');

function wrap(client) {
  return ({ children }) => React.createElement(QueryClientProvider, { client }, children);
}

describe('optimistic delete', () => {
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
});
