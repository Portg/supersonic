import { QueryCache, QueryClient, MutationCache } from '@tanstack/react-query';
import { notification } from 'antd';
import { shouldRetry, extractHttpStatus } from './queryUtils';

const FOUR_OH_ONE = 401;
const FOUR_OH_THREE = 403;

function describeError(err: unknown): { message: string; description?: string } {
  if (err && typeof err === 'object') {
    const anyErr = err as { message?: string; data?: { msg?: string; message?: string } };
    const description = anyErr.data?.msg || anyErr.data?.message;
    return {
      message: anyErr.message || '请求失败',
      description,
    };
  }
  return { message: '请求失败' };
}

function handleGlobalError(err: unknown, meta?: Record<string, unknown>) {
  const status = extractHttpStatus(err);
  // 401/403 are redirected by services/request.ts responseInterceptor — do not notify twice.
  if (status === FOUR_OH_ONE || status === FOUR_OH_THREE) return;
  // Opt-out: a hook can pass meta.silent = true to suppress this notification.
  if (meta && meta.silent === true) return;
  const { message, description } = describeError(err);
  notification.error({ message, description, duration: 4 });
}

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 5 * 60_000,
      refetchOnWindowFocus: false,
      refetchOnReconnect: true,
      retry: shouldRetry,
    },
    mutations: {
      retry: false,
    },
  },
  queryCache: new QueryCache({
    onError: (err, query) => handleGlobalError(err, query.meta as Record<string, unknown>),
  }),
  mutationCache: new MutationCache({
    onError: (err, _vars, _ctx, mutation) =>
      handleGlobalError(err, mutation.meta as Record<string, unknown>),
  }),
});
