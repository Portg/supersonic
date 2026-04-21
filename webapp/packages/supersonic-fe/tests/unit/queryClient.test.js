const {
  extractHttpStatus,
  shouldRetry,
} = require('../../.tmp-unit/utils/queryUtils.js');

describe('extractHttpStatus', () => {
  it('reads response.status from umi-request error', () => {
    expect(extractHttpStatus({ response: { status: 404 } })).toBe(404);
  });

  it('reads data.status (some server errors wrap in data)', () => {
    expect(extractHttpStatus({ data: { status: 500 } })).toBe(500);
  });

  it('returns undefined when no status', () => {
    expect(extractHttpStatus(new Error('network'))).toBeUndefined();
    expect(extractHttpStatus(null)).toBeUndefined();
    expect(extractHttpStatus(undefined)).toBeUndefined();
  });
});

describe('shouldRetry', () => {
  it('does not retry on 4xx', () => {
    expect(shouldRetry(0, { response: { status: 400 } })).toBe(false);
    expect(shouldRetry(0, { response: { status: 403 } })).toBe(false);
    expect(shouldRetry(0, { response: { status: 499 } })).toBe(false);
  });

  it('retries once on 5xx', () => {
    expect(shouldRetry(0, { response: { status: 500 } })).toBe(true);
    expect(shouldRetry(1, { response: { status: 500 } })).toBe(false);
  });

  it('retries once on network errors (no status)', () => {
    expect(shouldRetry(0, new Error('network'))).toBe(true);
    expect(shouldRetry(1, new Error('network'))).toBe(false);
  });
});
