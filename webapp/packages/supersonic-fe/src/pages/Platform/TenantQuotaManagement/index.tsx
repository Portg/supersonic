import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  InputNumber,
  message,
  Space,
  Popconfirm,
  Card,
  Switch,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { TenantQuota } from '@/services/tenantQuota';
import { listTenantQuotas, upsertTenantQuota, deleteTenantQuota } from '@/services/tenantQuota';
import { MSG } from '@/common/messages';
import styles from './style.less';

const TenantQuotaManagement: React.FC = () => {
  const [quotas, setQuotas] = useState<TenantQuota[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingQuota, setEditingQuota] = useState<TenantQuota | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    loadQuotas();
  }, []);

  const loadQuotas = async () => {
    setLoading(true);
    try {
      const res: any = await listTenantQuotas();
      const code = res?.code ?? 200;
      const data = Array.isArray(res) ? res : res?.data;
      if (code === 200 && data) {
        setQuotas(data);
      }
    } catch (error) {
      message.error('加载租户配额失败');
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = () => {
    setEditingQuota(null);
    form.resetFields();
    form.setFieldsValue({
      jdbcConcurrent: 10,
      llmConcurrent: 5,
      monthlyQueryCount: 0,
      acquireTimeoutMs: 5000,
      enabled: true,
    });
    setModalVisible(true);
  };

  const handleEdit = (quota: TenantQuota) => {
    setEditingQuota(quota);
    form.setFieldsValue(quota);
    setModalVisible(true);
  };

  const handleDelete = async (tenantId: number) => {
    try {
      const res: any = await deleteTenantQuota(tenantId);
      const code = res?.code ?? 200;
      if (code === 200) {
        message.success(MSG.DELETE_SUCCESS);
        loadQuotas();
      }
    } catch (error) {
      message.error(MSG.DELETE_FAILED);
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const tenantId = editingQuota ? editingQuota.tenantId : values.tenantId;
      const result: any = await upsertTenantQuota(tenantId, values);
      const code = result?.code ?? 200;

      if (code === 200) {
        message.success(editingQuota ? MSG.UPDATE_SUCCESS : MSG.CREATE_SUCCESS);
        setModalVisible(false);
        loadQuotas();
      } else {
        message.error(result.msg || MSG.OPERATION_FAILED);
      }
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: '租户ID',
      dataIndex: 'tenantId',
      key: 'tenantId',
      width: 100,
    },
    {
      title: 'JDBC并发',
      dataIndex: 'jdbcConcurrent',
      key: 'jdbcConcurrent',
      width: 110,
    },
    {
      title: 'LLM并发',
      dataIndex: 'llmConcurrent',
      key: 'llmConcurrent',
      width: 110,
    },
    {
      title: '每月查询上限',
      dataIndex: 'monthlyQueryCount',
      key: 'monthlyQueryCount',
      width: 130,
      render: (val: number) => (val === 0 ? '不限' : val),
    },
    {
      title: '超时(ms)',
      dataIndex: 'acquireTimeoutMs',
      key: 'acquireTimeoutMs',
      width: 110,
    },
    {
      title: '启用',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 80,
      render: (val: boolean) => <Switch checked={val} disabled size="small" />,
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 180,
      render: (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: TenantQuota) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确认删除该租户配额?"
            onConfirm={() => handleDelete(record.tenantId)}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.root}>
      <Card
        title="租户并发配额管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增
          </Button>
        }
      >
        <Table
          dataSource={quotas}
          columns={columns}
          rowKey="tenantId"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <Modal
        title={editingQuota ? '编辑租户配额' : '新增租户配额'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        confirmLoading={loading}
        okText="确定"
        cancelText="取消"
        width={500}
      >
        <Form form={form} layout="vertical" name="tenantQuotaForm">
          <Form.Item
            name="tenantId"
            label="租户ID"
            rules={[{ required: true, message: '请输入租户ID' }]}
          >
            <InputNumber
              min={1}
              placeholder="请输入租户ID"
              style={{ width: '100%' }}
              disabled={!!editingQuota}
            />
          </Form.Item>
          <Space size="large">
            <Form.Item
              name="jdbcConcurrent"
              label="JDBC并发数"
              rules={[{ required: true, message: '请输入JDBC并发数' }]}
            >
              <InputNumber min={1} placeholder="10" />
            </Form.Item>
            <Form.Item
              name="llmConcurrent"
              label="LLM并发数"
              rules={[{ required: true, message: '请输入LLM并发数' }]}
            >
              <InputNumber min={1} placeholder="5" />
            </Form.Item>
          </Space>
          <Space size="large">
            <Form.Item
              name="monthlyQueryCount"
              label="每月查询上限"
              tooltip="0 表示不限"
              rules={[{ required: true, message: '请输入每月查询上限' }]}
            >
              <InputNumber min={0} placeholder="0" />
            </Form.Item>
            <Form.Item
              name="acquireTimeoutMs"
              label="超时(ms)"
              rules={[{ required: true, message: '请输入超时时间' }]}
            >
              <InputNumber min={100} placeholder="5000" />
            </Form.Item>
          </Space>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default TenantQuotaManagement;
