/**
 * @jest-environment jsdom
 */
const React = require('react');
const { QueryClient, QueryClientProvider } = require('@tanstack/react-query');
const { renderHook, waitFor } = require('@testing-library/react');

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
jest.mock('../../.tmp-unit/services/deliveryConfig.js', () => ({
  getConfigList: jest.fn(),
}), { virtual: true });

const reportScheduleSvc = require('../../.tmp-unit/services/reportSchedule.js');
const { useScheduleListQuery } = require('../../.tmp-unit/hooks/queries/reportSchedule.js');

function wrapper(client) {
  return ({ children }) => React.createElement(QueryClientProvider, { client }, children);
}

describe('useScheduleListQuery', () => {
  it('calls getScheduleList with params and returns records', async () => {
    reportScheduleSvc.getScheduleList.mockResolvedValueOnce({
      records: [{ id: 1, name: 'Daily KPI' }],
      total: 1,
    });
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false, staleTime: 0 } },
    });
    const { result } = renderHook(
      () => useScheduleListQuery({ current: 1, pageSize: 20 }),
      { wrapper: wrapper(client) },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data.records).toHaveLength(1);
    expect(reportScheduleSvc.getScheduleList).toHaveBeenCalledWith({ current: 1, pageSize: 20 });
  });

  it('surfaces errors to the query result', async () => {
    reportScheduleSvc.getScheduleList.mockRejectedValueOnce(new Error('boom'));
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    const { result } = renderHook(
      () => useScheduleListQuery({ current: 1, pageSize: 20 }),
      { wrapper: wrapper(client) },
    );

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error.message).toBe('boom');
  });
});
