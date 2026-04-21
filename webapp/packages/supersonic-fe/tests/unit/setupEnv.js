process.env.SHOW_TAG = 'true';

jest.mock('@umijs/max', () => ({ history: { location: { search: '' } } }), { virtual: true });
jest.mock('antd', () => ({
  notification: { error: jest.fn(), success: jest.fn() },
  message: { error: jest.fn(), success: jest.fn() },
  Spin: () => null,
}), { virtual: true });
