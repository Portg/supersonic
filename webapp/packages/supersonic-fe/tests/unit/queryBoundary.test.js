/**
 * @jest-environment jsdom
 */
require('@testing-library/jest-dom');
const React = require('react');
const { render, screen } = require('@testing-library/react');

// Unmock antd so real Skeleton/Result components render with their CSS classes.
jest.unmock('antd');

const { default: QueryBoundary } = require('../../.tmp-unit/components/QueryBoundary/index.js');

describe('<QueryBoundary>', () => {
  it('renders Skeleton when loading', () => {
    const query = { isLoading: true, isError: false, data: undefined, error: null };
    render(React.createElement(QueryBoundary, { query }, 'content'));
    expect(document.querySelector('.ant-skeleton')).toBeTruthy();
  });

  it('renders Result when error (and no cached data)', () => {
    const query = { isLoading: false, isError: true, data: undefined, error: new Error('x') };
    render(React.createElement(QueryBoundary, { query }, 'content'));
    expect(screen.getByText(/加载失败/)).toBeInTheDocument();
  });

  it('renders children when data is present', () => {
    const query = { isLoading: false, isError: false, data: { ok: true }, error: null };
    render(React.createElement(QueryBoundary, { query }, React.createElement('div', null, 'hello')));
    expect(screen.getByText('hello')).toBeInTheDocument();
  });
});
