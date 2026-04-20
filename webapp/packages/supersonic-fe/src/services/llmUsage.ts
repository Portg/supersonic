import request from './request';

const BASE = '/api/semantic/admin/llm-usage';

export type LlmUsageRow = {
  id: number;
  tenantId: number;
  userId?: string;
  provider: string;
  model: string;
  callType: string;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  estimatedCostMicros: number;
  requestId?: string;
  traceId?: string;
  latencyMs?: number;
  success: boolean;
  errorType?: string;
  createdAt: string;
};

export type PageResult<T> = {
  records: T[];
  total: number;
  current: number;
  size: number;
};

export type DailyAgg = { day: string; tokens: number; cost: number };

export function queryLlmUsage(params: {
  tenantId: number;
  from?: string;
  to?: string;
  model?: string;
  callType?: string;
  page?: number;
  size?: number;
}) {
  return request<PageResult<LlmUsageRow>>(BASE, { method: 'GET', params });
}

export function queryDailyAggregates(params: {
  tenantId: number;
  from: string;
  to: string;
}) {
  return request<DailyAgg[]>(`${BASE}/daily`, { method: 'GET', params });
}

export function queryTotalTokens(params: {
  tenantId: number;
  from: string;
  to: string;
}) {
  return request<number>(`${BASE}/total-tokens`, { method: 'GET', params });
}
