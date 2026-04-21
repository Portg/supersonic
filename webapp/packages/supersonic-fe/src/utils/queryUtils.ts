/**
 * Extract HTTP status from an error thrown by umi-request.
 * umi-request attaches the Response on `err.response`, and sometimes
 * wraps backend payloads on `err.data`.
 */
export function extractHttpStatus(err: unknown): number | undefined {
  if (!err || typeof err !== 'object') return undefined;
  const anyErr = err as { response?: { status?: number }; data?: { status?: number } };
  if (anyErr.response && typeof anyErr.response.status === 'number') {
    return anyErr.response.status;
  }
  if (anyErr.data && typeof anyErr.data.status === 'number') {
    return anyErr.data.status;
  }
  return undefined;
}

/**
 * React-Query `retry` function: retry at most once, and skip 4xx.
 * Signature matches TanStack's `(failureCount: number, error: unknown) => boolean`.
 */
export function shouldRetry(failureCount: number, err: unknown): boolean {
  if (failureCount >= 1) return false;
  const status = extractHttpStatus(err);
  if (status !== undefined && status >= 400 && status < 500) return false;
  return true;
}
