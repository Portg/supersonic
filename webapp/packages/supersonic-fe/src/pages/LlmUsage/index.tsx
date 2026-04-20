import React, { useEffect, useState } from 'react';
import { Card, DatePicker, InputNumber, Space, Statistic, Table, Row, Col, message } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import {
  queryLlmUsage,
  queryDailyAggregates,
  queryTotalTokens,
  LlmUsageRow,
  DailyAgg,
} from '@/services/llmUsage';
import styles from './style.less';

const { RangePicker } = DatePicker;

const LlmUsage: React.FC = () => {
  const [tenantId, setTenantId] = useState<number>(1);
  const [range, setRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(30, 'day'),
    dayjs(),
  ]);
  const [rows, setRows] = useState<LlmUsageRow[]>([]);
  const [daily, setDaily] = useState<DailyAgg[]>([]);
  const [totalTokens, setTotalTokens] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!range[0] || !range[1] || !tenantId) return;
    const from = range[0].format('YYYY-MM-DD');
    const to = range[1].format('YYYY-MM-DD');
    setLoading(true);
    Promise.all([
      queryLlmUsage({ tenantId, from, to, page: 1, size: 50 }),
      queryDailyAggregates({ tenantId, from, to }),
      queryTotalTokens({ tenantId, from, to }),
    ])
      .then(([pg, agg, total]) => {
        setRows((pg as any)?.records || []);
        setDaily((agg as DailyAgg[]) || []);
        setTotalTokens((total as number) || 0);
      })
      .catch(() => {
        message.error('加载 LLM 用量数据失败');
        setRows([]);
        setDaily([]);
        setTotalTokens(0);
      })
      .finally(() => setLoading(false));
  }, [tenantId, range]);

  const totalCostUsd = daily.reduce((sum, d) => sum + d.cost, 0) / 1_000_000;

  const columns = [
    {
      title: 'Time',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm:ss'),
    },
    { title: 'Model', dataIndex: 'model', key: 'model' },
    { title: 'Call Type', dataIndex: 'callType', key: 'callType' },
    { title: 'In Tokens', dataIndex: 'inputTokens', key: 'inputTokens' },
    { title: 'Out Tokens', dataIndex: 'outputTokens', key: 'outputTokens' },
    { title: 'Total Tokens', dataIndex: 'totalTokens', key: 'totalTokens' },
    {
      title: 'Cost (USD)',
      dataIndex: 'estimatedCostMicros',
      key: 'estimatedCostMicros',
      render: (v: number) => (v / 1_000_000).toFixed(6),
    },
    {
      title: 'Success',
      dataIndex: 'success',
      key: 'success',
      render: (v: boolean) => (v ? 'yes' : 'no'),
    },
  ];

  return (
    <div className={styles.llmUsage}>
      <Space style={{ marginBottom: 16 }}>
        <span>Tenant ID:</span>
        <InputNumber
          value={tenantId}
          min={1}
          onChange={(v) => v && setTenantId(Number(v))}
        />
        <RangePicker
          value={range}
          onChange={(v) => v && v[0] && v[1] && setRange([v[0], v[1]])}
        />
      </Space>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Card>
            <Statistic title="Total Tokens" value={totalTokens} />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic title="Total Cost (USD)" value={totalCostUsd} precision={4} />
          </Card>
        </Col>
      </Row>

      <Card title="Recent Calls">
        <Table<LlmUsageRow>
          rowKey="id"
          dataSource={rows}
          columns={columns}
          loading={loading}
          pagination={{ pageSize: 20 }}
        />
      </Card>
    </div>
  );
};

export default LlmUsage;
