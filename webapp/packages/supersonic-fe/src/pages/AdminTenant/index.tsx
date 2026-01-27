import React, { useState, useEffect, useRef } from 'react';
import {
  Button,
  message,
  Modal,
  Form,
  Input,
  InputNumber,
  Space,
  Tag,
  Popconfirm,
  Drawer,
  Descriptions,
  Tabs,
  Row,
  Col,
  Statistic,
  Select,
  Radio,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  StopOutlined,
  CheckCircleOutlined,
  EyeOutlined,
  TeamOutlined,
  DatabaseOutlined,
  RobotOutlined,
  ApiOutlined,
  CloudOutlined,
  GiftOutlined,
} from '@ant-design/icons';
import { ProTable, ProCard } from '@ant-design/pro-components';
import type { ProColumns, ActionType } from '@ant-design/pro-components';
import {
  getAllTenants,
  createTenant,
  updateTenant,
  deleteTenant,
  suspendTenant,
  activateTenant,
  Tenant,
} from '@/services/tenant';
import {
  getSubscriptionPlans,
  getTenantSubscription,
  updateTenantSubscription,
} from '@/services/subscription';
import type { SubscriptionPlan, TenantSubscription } from '@/services/tenant';
import dayjs from 'dayjs';
import styles from './style.less';

const { TextArea } = Input;
const { TabPane } = Tabs;

const AdminTenant: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [modalVisible, setModalVisible] = useState(false);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [editingTenant, setEditingTenant] = useState<Tenant | null>(null);
  const [viewingTenant, setViewingTenant] = useState<Tenant | null>(null);
  const [form] = Form.useForm();
  const [subscriptionModalVisible, setSubscriptionModalVisible] = useState(false);
  const [subscriptionForm] = Form.useForm();
  const [plans, setPlans] = useState<SubscriptionPlan[]>([]);
  const [assigningTenant, setAssigningTenant] = useState<Tenant | null>(null);
  const [tenantSubscriptions, setTenantSubscriptions] = useState<Record<number, TenantSubscription | null>>({});
  const [assigningSubscription, setAssigningSubscription] = useState(false);

  useEffect(() => {
    loadPlans();
  }, []);

  const loadPlans = async () => {
    const res = await getSubscriptionPlans();
    if (res.code === 200 && res.data) {
      setPlans(res.data);
    }
  };

  const loadTenants = async () => {
    const res = await getAllTenants();
    if (res.code === 200) {
      return {
        data: res.data || [],
        success: true,
      };
    }
    return {
      data: [],
      success: false,
    };
  };

  const handleCreate = () => {
    setEditingTenant(null);
    form.resetFields();
    form.setFieldsValue({
      maxUsers: 10,
      maxDatasets: 10,
      maxModels: 5,
      maxAgents: 5,
      maxApiCallsPerDay: 10000,
      maxTokensPerMonth: 1000000,
    });
    setModalVisible(true);
  };

  const handleEdit = (record: Tenant) => {
    setEditingTenant(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  const handleView = (record: Tenant) => {
    setViewingTenant(record);
    setDrawerVisible(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      let res;
      if (editingTenant) {
        res = await updateTenant(editingTenant.id, values);
      } else {
        res = await createTenant(values);
      }

      if (res.code === 200) {
        message.success(editingTenant ? '更新成功' : '创建成功');
        setModalVisible(false);
        actionRef.current?.reload();
      } else {
        message.error(res.msg || '操作失败');
      }
    } catch (error) {
      message.error('操作失败');
    }
  };

  const handleDelete = async (id: number) => {
    const res = await deleteTenant(id);
    if (res.code === 200) {
      message.success('删除成功');
      actionRef.current?.reload();
    } else {
      message.error(res.msg || '删除失败');
    }
  };

  const handleSuspend = async (id: number) => {
    const res = await suspendTenant(id);
    if (res.code === 200) {
      message.success('已暂停租户');
      actionRef.current?.reload();
    } else {
      message.error(res.msg || '操作失败');
    }
  };

  const handleActivate = async (id: number) => {
    const res = await activateTenant(id);
    if (res.code === 200) {
      message.success('已激活租户');
      actionRef.current?.reload();
    } else {
      message.error(res.msg || '操作失败');
    }
  };

  const formatNumber = (num: number) => {
    if (num >= 1000000) {
      return (num / 1000000).toFixed(1) + 'M';
    }
    if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'K';
    }
    return num.toString();
  };

  const handleAssignSubscription = async (record: Tenant) => {
    setAssigningTenant(record);
    subscriptionForm.resetFields();
    subscriptionForm.setFieldsValue({ billingCycle: 'MONTHLY' });

    // Load existing subscription for this tenant
    try {
      const res = await getTenantSubscription(record.id);
      if (res.code === 200 && res.data) {
        subscriptionForm.setFieldsValue({
          planId: res.data.planId,
          billingCycle: res.data.billingCycle || 'MONTHLY',
        });
      }
    } catch (error) {
      // No existing subscription, that's fine
    }

    setSubscriptionModalVisible(true);
  };

  const handleSubmitSubscription = async () => {
    if (!assigningTenant) return;

    try {
      const values = await subscriptionForm.validateFields();
      setAssigningSubscription(true);

      const res = await updateTenantSubscription(assigningTenant.id, {
        planId: values.planId,
        billingCycle: values.billingCycle,
      });

      if (res.code === 200) {
        message.success('订阅分配成功');
        setSubscriptionModalVisible(false);
        actionRef.current?.reload();
      } else {
        message.error(res.msg || '订阅分配失败');
      }
    } catch (error) {
      message.error('订阅分配失败');
    } finally {
      setAssigningSubscription(false);
    }
  };

  const getPlanNameById = (planId?: number): string => {
    if (!planId) return '未订阅';
    const plan = plans.find((p) => p.id === planId);
    return plan ? plan.name : '未知计划';
  };

  const columns: ProColumns<Tenant>[] = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 60,
    },
    {
      title: '租户名称',
      dataIndex: 'name',
      width: 120,
      ellipsis: true,
    },
    {
      title: '租户代码',
      dataIndex: 'code',
      width: 120,
      copyable: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (_, record) => {
        const statusMap: Record<string, { color: string; text: string }> = {
          ACTIVE: { color: 'green', text: '正常' },
          SUSPENDED: { color: 'orange', text: '已暂停' },
          DELETED: { color: 'red', text: '已删除' },
        };
        const status = statusMap[record.status] || { color: 'default', text: record.status };
        return <Tag color={status.color}>{status.text}</Tag>;
      },
    },
    {
      title: '联系人',
      dataIndex: 'contactName',
      width: 100,
      ellipsis: true,
    },
    {
      title: '联系邮箱',
      dataIndex: 'contactEmail',
      width: 160,
      ellipsis: true,
    },
    {
      title: '订阅计划',
      dataIndex: 'subscriptionPlanName',
      width: 100,
      render: (_: any, record: Tenant) => {
        const planName = (record as any).subscriptionPlanName;
        return planName ? (
          <Tag color="blue">{planName}</Tag>
        ) : (
          <Tag color="default">未订阅</Tag>
        );
      },
    },
    {
      title: '最大用户数',
      dataIndex: 'maxUsers',
      width: 100,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 170,
      valueType: 'dateTime',
    },
    {
      title: '操作',
      valueType: 'option',
      width: 300,
      fixed: 'right',
      render: (_, record) => [
        <Button
          key="view"
          type="link"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => handleView(record)}
        >
          查看
        </Button>,
        <Button
          key="edit"
          type="link"
          size="small"
          icon={<EditOutlined />}
          onClick={() => handleEdit(record)}
        >
          编辑
        </Button>,
        <Button
          key="subscription"
          type="link"
          size="small"
          icon={<GiftOutlined />}
          onClick={() => handleAssignSubscription(record)}
        >
          分配订阅
        </Button>,
        record.status === 'ACTIVE' ? (
          <Popconfirm
            key="suspend"
            title="确定要暂停该租户吗？"
            onConfirm={() => handleSuspend(record.id)}
          >
            <Button type="link" size="small" icon={<StopOutlined />} danger>
              暂停
            </Button>
          </Popconfirm>
        ) : record.status === 'SUSPENDED' ? (
          <Popconfirm
            key="activate"
            title="确定要激活该租户吗？"
            onConfirm={() => handleActivate(record.id)}
          >
            <Button type="link" size="small" icon={<CheckCircleOutlined />}>
              激活
            </Button>
          </Popconfirm>
        ) : null,
        <Popconfirm
          key="delete"
          title="确定要删除该租户吗？此操作不可恢复！"
          onConfirm={() => handleDelete(record.id)}
        >
          <Button type="link" size="small" icon={<DeleteOutlined />} danger>
            删除
          </Button>
        </Popconfirm>,
      ],
    },
  ];

  return (
    <div className={styles.container}>
      <ProTable<Tenant>
        headerTitle="租户管理"
        actionRef={actionRef}
        rowKey="id"
        search={false}
        toolBarRender={() => [
          <Button key="create" type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新建租户
          </Button>,
        ]}
        request={loadTenants}
        columns={columns}
        scroll={{ x: 1400 }}
        pagination={{
          pageSize: 10,
        }}
      />

      <Modal
        title={editingTenant ? '编辑租户' : '新建租户'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={700}
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="name"
                label="租户名称"
                rules={[{ required: true, message: '请输入租户名称' }]}
              >
                <Input placeholder="请输入租户名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="code"
                label="租户代码"
                rules={[
                  { required: true, message: '请输入租户代码' },
                  { pattern: /^[a-zA-Z0-9_-]+$/, message: '只能包含字母、数字、下划线和连字符' },
                ]}
              >
                <Input placeholder="请输入租户代码" disabled={!!editingTenant} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="description" label="描述">
            <TextArea rows={2} placeholder="请输入租户描述" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="contactName" label="联系人">
                <Input placeholder="联系人姓名" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="contactEmail"
                label="联系邮箱"
                rules={[{ type: 'email', message: '请输入有效邮箱' }]}
              >
                <Input placeholder="联系邮箱" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="contactPhone" label="联系电话">
                <Input placeholder="联系电话" />
              </Form.Item>
            </Col>
          </Row>
          <ProCard title="资源配额" bordered style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item name="maxUsers" label="最大用户数">
                  <InputNumber min={1} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="maxDatasets" label="最大数据集数">
                  <InputNumber min={1} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="maxModels" label="最大模型数">
                  <InputNumber min={1} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
            </Row>
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item name="maxAgents" label="最大Agent数">
                  <InputNumber min={1} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="maxApiCallsPerDay" label="每日API调用上限">
                  <InputNumber min={1} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="maxTokensPerMonth" label="每月Token上限">
                  <InputNumber min={1} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
            </Row>
          </ProCard>
        </Form>
      </Modal>

      <Drawer
        title="租户详情"
        width={600}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
      >
        {viewingTenant && (
          <Tabs defaultActiveKey="info">
            <TabPane tab="基本信息" key="info">
              <Descriptions column={2} bordered size="small">
                <Descriptions.Item label="ID">{viewingTenant.id}</Descriptions.Item>
                <Descriptions.Item label="状态">
                  <Tag
                    color={
                      viewingTenant.status === 'ACTIVE'
                        ? 'green'
                        : viewingTenant.status === 'SUSPENDED'
                          ? 'orange'
                          : 'red'
                    }
                  >
                    {viewingTenant.status === 'ACTIVE'
                      ? '正常'
                      : viewingTenant.status === 'SUSPENDED'
                        ? '已暂停'
                        : '已删除'}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="名称">{viewingTenant.name}</Descriptions.Item>
                <Descriptions.Item label="代码">{viewingTenant.code}</Descriptions.Item>
                <Descriptions.Item label="描述" span={2}>
                  {viewingTenant.description || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="联系人">{viewingTenant.contactName || '-'}</Descriptions.Item>
                <Descriptions.Item label="联系邮箱">
                  {viewingTenant.contactEmail || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="联系电话">
                  {viewingTenant.contactPhone || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="创建时间">{viewingTenant.createdAt ? dayjs(viewingTenant.createdAt).format('YYYY-MM-DD HH:mm:ss') : '-'}</Descriptions.Item>
                <Descriptions.Item label="创建人">{viewingTenant.createdBy || '-'}</Descriptions.Item>
                <Descriptions.Item label="更新时间">{viewingTenant.updatedAt ? dayjs(viewingTenant.updatedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}</Descriptions.Item>
              </Descriptions>
            </TabPane>
            <TabPane tab="资源配额" key="quota">
              <Row gutter={[16, 16]}>
                <Col span={12}>
                  <ProCard bordered>
                    <Statistic
                      title="最大用户数"
                      value={viewingTenant.maxUsers}
                      prefix={<TeamOutlined />}
                    />
                  </ProCard>
                </Col>
                <Col span={12}>
                  <ProCard bordered>
                    <Statistic
                      title="最大数据集数"
                      value={viewingTenant.maxDatasets}
                      prefix={<DatabaseOutlined />}
                    />
                  </ProCard>
                </Col>
                <Col span={12}>
                  <ProCard bordered>
                    <Statistic
                      title="最大模型数"
                      value={viewingTenant.maxModels}
                      prefix={<RobotOutlined />}
                    />
                  </ProCard>
                </Col>
                <Col span={12}>
                  <ProCard bordered>
                    <Statistic
                      title="最大Agent数"
                      value={viewingTenant.maxAgents}
                      prefix={<RobotOutlined />}
                    />
                  </ProCard>
                </Col>
                <Col span={12}>
                  <ProCard bordered>
                    <Statistic
                      title="每日API调用上限"
                      value={formatNumber(viewingTenant.maxApiCallsPerDay)}
                      prefix={<ApiOutlined />}
                    />
                  </ProCard>
                </Col>
                <Col span={12}>
                  <ProCard bordered>
                    <Statistic
                      title="每月Token上限"
                      value={formatNumber(viewingTenant.maxTokensPerMonth)}
                      prefix={<CloudOutlined />}
                    />
                  </ProCard>
                </Col>
              </Row>
            </TabPane>
            <TabPane tab="订阅信息" key="subscription">
              <div style={{ textAlign: 'center', padding: '20px 0' }}>
                <p style={{ color: '#666', marginBottom: 16 }}>
                  订阅计划:{' '}
                  <Tag color="blue">
                    {(viewingTenant as any).subscriptionPlanName || '未订阅'}
                  </Tag>
                </p>
                <Button
                  type="primary"
                  icon={<GiftOutlined />}
                  onClick={() => {
                    setDrawerVisible(false);
                    handleAssignSubscription(viewingTenant);
                  }}
                >
                  分配订阅
                </Button>
              </div>
            </TabPane>
          </Tabs>
        )}
      </Drawer>

      <Modal
        title={`分配订阅 - ${assigningTenant?.name || ''}`}
        open={subscriptionModalVisible}
        onOk={handleSubmitSubscription}
        onCancel={() => setSubscriptionModalVisible(false)}
        confirmLoading={assigningSubscription}
        okText="确认分配"
        cancelText="取消"
      >
        <Form form={subscriptionForm} layout="vertical">
          <Form.Item
            name="planId"
            label="订阅计划"
            rules={[{ required: true, message: '请选择订阅计划' }]}
          >
            <Select placeholder="请选择订阅计划">
              {plans.map((plan) => (
                <Select.Option key={plan.id} value={plan.id}>
                  {plan.name} - ¥{plan.priceMonthly}/月
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            name="billingCycle"
            label="计费周期"
            rules={[{ required: true, message: '请选择计费周期' }]}
          >
            <Radio.Group>
              <Radio value="MONTHLY">月付</Radio>
              <Radio value="YEARLY">年付</Radio>
            </Radio.Group>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AdminTenant;
