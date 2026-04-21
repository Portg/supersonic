import React from 'react';
import { Skeleton, Result, Button } from 'antd';
import type { UseQueryResult } from '@tanstack/react-query';

interface Props<TData> {
  query: Pick<UseQueryResult<TData>, 'isLoading' | 'isError' | 'data' | 'error' | 'refetch'>
       & Partial<Pick<UseQueryResult<TData>, 'isFetching'>>;
  skeletonRows?: number;
  keepStaleOnError?: boolean;
  errorTitle?: string;
  children: React.ReactNode;
}

function QueryBoundary<TData>({
  query,
  skeletonRows = 4,
  keepStaleOnError = true,
  errorTitle = '加载失败',
  children,
}: Props<TData>) {
  if (query.isLoading && !query.data) {
    return <Skeleton active paragraph={{ rows: skeletonRows }} />;
  }
  if (query.isError && !(keepStaleOnError && query.data)) {
    const msg = (query.error as { message?: string } | null)?.message;
    return (
      <Result
        status="error"
        title={errorTitle}
        subTitle={msg}
        extra={
          query.refetch ? (
            <Button type="primary" onClick={() => query.refetch!()}>重试</Button>
          ) : null
        }
      />
    );
  }
  return <>{children}</>;
}

export default QueryBoundary;
