const { queryKeys, createQueryKeys } = require('../../.tmp-unit/utils/queryKeys.js');

describe('queryKeys factory (singleton reads tenant from localStorage)', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('namespaces all keys by current tenant id', () => {
    localStorage.setItem('X-Tenant-Id', '7');
    expect(queryKeys.reportSchedule.all()).toEqual(['t:7', 'reportSchedule']);
    expect(queryKeys.reportSchedule.list({ current: 1, pageSize: 20 })).toEqual([
      't:7',
      'reportSchedule',
      'list',
      { current: 1, pageSize: 20 },
    ]);
    expect(queryKeys.reportSchedule.detail(42)).toEqual(['t:7', 'reportSchedule', 'detail', 42]);
  });

  it('defaults to tenant 1 when header missing', () => {
    expect(queryKeys.reportSchedule.all()).toEqual(['t:1', 'reportSchedule']);
  });

  it('createQueryKeys allows injecting a tenant explicitly (for tests)', () => {
    const k = createQueryKeys(99);
    expect(k.reportSchedule.all()).toEqual(['t:99', 'reportSchedule']);
  });
});
