import React, { useState } from 'react';
import dayjs from 'dayjs';
import { Button, Table, Tag, Switch, Space, Popconfirm, message, Tooltip } from 'antd';
import { PlusOutlined, SendOutlined, SettingOutlined } from '@ant-design/icons';
import { history } from 'umi';
import ScheduleForm from './components/ScheduleForm';
import ExecutionList from './components/ExecutionList';
import styles from './index.less';
import taskStyles from '../TaskCenter/style.less';
import type { ReportSchedule } from '@/services/reportSchedule';
import { DELIVERY_TYPE_MAP } from '@/services/deliveryConfig';
import { MSG } from '@/common/messages';
import PageEmpty from '@/components/PageEmpty';
import {
  useScheduleListQuery,
  useValidDataSetMapQuery,
  useDeliveryConfigMapQuery,
  useScheduleSaveMutation,
  useScheduleDeleteMutation,
  useScheduleToggleMutation,
  useScheduleTriggerMutation,
} from '@/hooks/queries/reportSchedule';

const ReportSchedulePage: React.FC = () => {
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20 });
  const [formVisible, setFormVisible] = useState(false);
  const [editRecord, setEditRecord] = useState<ReportSchedule | undefined>();
  const [executionDrawer, setExecutionDrawer] = useState<{ visible: boolean; scheduleId?: number; name?: string }>({ visible: false });
  const [triggeringScheduleIds, setTriggeringScheduleIds] = useState<Record<number, boolean>>({});

  const listQuery = useScheduleListQuery(pagination);
  const deliveryConfigMapQuery = useDeliveryConfigMapQuery();
  const datasetNameMapQuery = useValidDataSetMapQuery();

  const data = (listQuery.data?.records ?? []) as ReportSchedule[];
  const total = listQuery.data?.total ?? 0;
  const loading = listQuery.isLoading || listQuery.isFetching;
  const deliveryConfigMap = deliveryConfigMapQuery.data ?? {};
  const datasetNameMap = datasetNameMapQuery.data ?? {};

  const saveMutation = useScheduleSaveMutation();
  const deleteMutation = useScheduleDeleteMutation();
  const toggleMutation = useScheduleToggleMutation();
  const triggerMutation = useScheduleTriggerMutation();

  const handleCreate = () => {
    setEditRecord(undefined);
    setFormVisible(true);
  };

  const handleEdit = (record: ReportSchedule) => {
    setEditRecord(record);
    setFormVisible(true);
  };

  const handleFormSubmit = async (values: Partial<ReportSchedule>) => {
    await saveMutation.mutateAsync({ id: editRecord?.id, values });
    message.success(editRecord?.id ? MSG.UPDATE_SUCCESS : MSG.CREATE_SUCCESS);
    setFormVisible(false);
  };

  const handleDelete = async (id: number) => {
    await deleteMutation.mutateAsync(id);
    message.success(MSG.DELETE_SUCCESS);
  };

  const handleToggle = async (record: ReportSchedule, checked: boolean) => {
    await toggleMutation.mutateAsync({ id: record.id, enabled: checked });
  };

  const handleTrigger = async (id: number) => {
    if (triggeringScheduleIds[id]) return;
    setTriggeringScheduleIds((p) => ({ ...p, [id]: true }));
    try {
      await triggerMutation.mutateAsync(id);
      message.success('已触发执行');
    } finally {
      setTriggeringScheduleIds((p) => { const n = { ...p }; delete n[id]; return n; });
    }
  };

  const columns = [
    {
      title: '任务名称',
      dataIndex: 'name',
      width: 200,
      ellipsis: true,
    },
    {
      title: '关联数据集',
      dataIndex: 'datasetId',
      width: 180,
      render: (id: number) => (datasetNameMap[id] != null ? `${datasetNameMap[id]} (${id})` : id ?? '-'),
    },
    {
      title: 'Cron 表达式',
      dataIndex: 'cronExpression',
      width: 150,
      render: (cron: string) => (
        <Tooltip title={cron}>
          <code>{cron}</code>
        </Tooltip>
      ),
    },
    {
      title: '输出格式',
      dataIndex: 'outputFormat',
      width: 100,
      render: (fmt: string) => <Tag>{fmt}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 80,
      render: (enabled: boolean, record: ReportSchedule) => (
        <Switch checked={enabled} size="small" onChange={(checked) => handleToggle(record, checked)} />
      ),
    },
    {
      title: '上次执行',
      dataIndex: 'lastExecutionTime',
      width: 180,
      render: (val: string) => val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: '推送渠道',
      dataIndex: 'deliveryConfigIds',
      width: 150,
      render: (ids: string) => {
        if (!ids) return <span style={{ color: '#999' }}>-</span>;
        const idList = ids.split(',').map((id) => parseInt(id.trim(), 10)).filter((id) => !isNaN(id));
        if (idList.length === 0) return <span style={{ color: '#999' }}>-</span>;
        return (
          <Space size={2} wrap>
            {idList.map((id) => {
              const config = deliveryConfigMap[id];
              if (!config) return <Tag key={id}>{id}</Tag>;
              const typeInfo = DELIVERY_TYPE_MAP[config.deliveryType];
              return (
                <Tooltip key={id} title={config.name}>
                  <Tag color={typeInfo?.color} icon={<SendOutlined />}>
                    {typeInfo?.text || config.deliveryType}
                  </Tag>
                </Tooltip>
              );
            })}
          </Space>
        );
      },
    },
    {
      title: '操作',
      width: 200,
      fixed: 'right' as const,
      render: (_: any, record: ReportSchedule) => (
        <Space size={4} wrap>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            loading={!!triggeringScheduleIds[record.id]}
            disabled={!!triggeringScheduleIds[record.id]}
            onClick={() => handleTrigger(record.id)}
          >
            立即执行
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => setExecutionDrawer({ visible: true, scheduleId: record.id, name: record.name })}
          >
            执行记录
          </Button>
          <Popconfirm title="确认删除?" onConfirm={() => handleDelete(record.id)} okText="确认" cancelText="取消">
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.reportSchedulePage}>
      <div className={taskStyles.sectionHeader}>
        <div>
          <div className={taskStyles.sectionTitle}>报表调度</div>
        </div>
        <Space>
          <Button icon={<SettingOutlined />} onClick={() => history.push('/delivery-config')}>
            推送配置
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            创建调度
          </Button>
        </Space>
      </div>
      <div className={taskStyles.tableShell}>
        <Table
          rowKey="id"
          size="middle"
          bordered={false}
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: 'max-content' }}
          locale={{
            emptyText: (
              <PageEmpty
                description="暂无调度任务，创建后可定时导出报表"
                action={
                  <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
                    创建调度
                  </Button>
                }
              />
            ),
          }}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (page, size) => setPagination({ current: page, pageSize: size }),
          }}
        />
      </div>
      <ScheduleForm
        visible={formVisible}
        record={editRecord}
        onCancel={() => setFormVisible(false)}
        onSubmit={handleFormSubmit}
      />
      <ExecutionList
        visible={executionDrawer.visible}
        scheduleId={executionDrawer.scheduleId}
        scheduleName={executionDrawer.name}
        onClose={() => setExecutionDrawer({ visible: false })}
      />
    </div>
  );
};

export default ReportSchedulePage;
