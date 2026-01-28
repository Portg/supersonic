import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  Select,
  message,
  Space,
  Popconfirm,
  Card,
  Tag,
  Switch,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { getAllTenants, createTenant, updateTenant, deleteTenant } from '@/services/platform';
import styles from './style.less';

interface Tenant {
  id: number;
  name: string;
  displayName: string;
  description?: string;
  status: number;
  subscriptionPlanId?: number;
  subscriptionPlanName?: string;
  maxUsers?: number;
  maxQueries?: number;
  createdAt: string;
  createdBy: string;
}

const TenantManagement: React.FC = () => {
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingTenant, setEditingTenant] = useState<Tenant | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    loadTenants();
  }, []);

  const loadTenants = async () => {
    setLoading(true);
    try {
      const { code, data } = await getAllTenants();
      if (code === 200 && data) {
        setTenants(data);
      }
    } catch (error) {
      message.error('加载租户列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = () => {
    setEditingTenant(null);
    form.resetFields();
    form.setFieldsValue({ status: 1 });
    setModalVisible(true);
  };

  const handleEdit = (tenant: Tenant) => {
    setEditingTenant(tenant);
    form.setFieldsValue(tenant);
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      const { code } = await deleteTenant(id);
      if (code === 200) {
        message.success('删除成功');
        loadTenants();
      }
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      let result;
      if (editingTenant) {
        result = await updateTenant(editingTenant.id, values);
      } else {
        result = await createTenant(values);
      }

      if (result.code === 200) {
        message.success(editingTenant ? '更新成功' : '创建成功');
        setModalVisible(false);
        loadTenants();
      } else {
        message.error(result.msg || '操作失败');
      }
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '租户标识',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '租户名称',
      dataIndex: 'displayName',
      key: 'displayName',
    },
    {
      title: '订阅计划',
      dataIndex: 'subscriptionPlanName',
      key: 'subscriptionPlanName',
      render: (text: string) => text ? <Tag color="blue">{text}</Tag> : '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: Tenant) => (
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
            title="确认删除该租户?"
            onConfirm={() => handleDelete(record.id)}
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
    <div className={styles.container}>
      <Card
        title="租户管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新建租户
          </Button>
        }
      >
        <Table
          dataSource={tenants}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <Modal
        title={editingTenant ? '编辑租户' : '新建租户'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        confirmLoading={loading}
        okText="确定"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" name="platformTenantForm">
          <Form.Item
            name="name"
            label="租户标识"
            rules={[
              { required: true, message: '请输入租户标识' },
              { pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/, message: '只能包含字母、数字、下划线和中划线，且以字母开头' },
            ]}
          >
            <Input placeholder="请输入租户标识" disabled={!!editingTenant} />
          </Form.Item>
          <Form.Item
            name="displayName"
            label="租户名称"
            rules={[{ required: true, message: '请输入租户名称' }]}
          >
            <Input placeholder="请输入租户名称" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="请输入描述" rows={3} />
          </Form.Item>
          <Form.Item name="status" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" defaultChecked />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default TenantManagement;
