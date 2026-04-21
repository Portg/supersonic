// Browser-API shims for antd / RTL tests.
// jest-dom matchers are imported at the top of each RTL test file.

global.ResizeObserver = global.ResizeObserver || class {
  observe() {}
  unobserve() {}
  disconnect() {}
};

if (typeof global.window !== 'undefined' && !global.window.matchMedia) {
  global.window.matchMedia = (q) => ({
    matches: false, media: q, addListener() {}, removeListener() {},
    addEventListener() {}, removeEventListener() {}, dispatchEvent() { return false; },
    onchange: null,
  });
}
