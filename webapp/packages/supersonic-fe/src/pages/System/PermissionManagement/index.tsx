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
  Tag,
  Card,
  InputNumber,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  getAllPermissions,
  createPermission,
  updatePermission,
  deletePermission,
  Permission,
} from '@/services/role';

const PermissionManagement: React.FC = () => {
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingPermission, setEditingPermission] = useState<Permission | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchPermissions();
  }, []);

  const fetchPermissions = async () => {
    setLoading(true);
    try {
      const res = await getAllPermissions();
      if (res.code === 200) {
        setPermissions(res.data || []);
      }
    } catch (error) {
      message.error('获取权限列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = () => {
    setEditingPermission(null);
    form.resetFields();
    form.setFieldsValue({ type: 'MENU', sortOrder: 0 });
    setModalVisible(true);
  };

  const handleEdit = (record: Permission) => {
    setEditingPermission(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      const res = await deletePermission(id);
      if (res.code === 200) {
        message.success('删除成功');
        fetchPermissions();
      } else {
        message.error(res.msg || '删除失败');
      }
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const permissionData: Permission = {
        ...values,
        id: editingPermission?.id,
        status: true,
      };

      const res = editingPermission
        ? await updatePermission(permissionData)
        : await createPermission(permissionData);

      if (res.code === 200) {
        message.success(editingPermission ? '更新成功' : '创建成功');
        setModalVisible(false);
        fetchPermissions();
      } else {
        message.error(res.msg || '操作失败');
      }
    } catch (error) {
      console.error('Validation failed:', error);
    }
  };

  const getTypeTag = (type: string) => {
    const typeMap: Record<string, { color: string; text: string }> = {
      MENU: { color: 'blue', text: '菜单' },
      BUTTON: { color: 'green', text: '按钮' },
      API: { color: 'orange', text: 'API' },
    };
    const config = typeMap[type] || { color: 'default', text: type };
    return <Tag color={config.color}>{config.text}</Tag>;
  };

  const columns: ColumnsType<Permission> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 60,
    },
    {
      title: '权限名称',
      dataIndex: 'name',
      width: 150,
    },
    {
      title: '权限编码',
      dataIndex: 'code',
      width: 180,
    },
    {
      title: '类型',
      dataIndex: 'type',
      width: 80,
      render: (type: string) => getTypeTag(type),
    },
    {
      title: '路径',
      dataIndex: 'path',
      ellipsis: true,
    },
    {
      title: '图标',
      dataIndex: 'icon',
      width: 150,
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 60,
    },
    {
      title: '描述',
      dataIndex: 'description',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (status: boolean) =>
        status ? <Tag color="success">启用</Tag> : <Tag color="default">禁用</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm title="确定删除该权限吗？" onConfirm={() => handleDelete(record.id!)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  // 获取父权限选项（只有 MENU 类型可以作为父权限）
  const parentOptions = permissions
    .filter((p) => p.type === 'MENU' && p.id !== editingPermission?.id)
    .map((p) => ({
      label: p.name,
      value: p.id,
    }));

  return (
    <Card title="权限管理" extra={
      <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
        新增权限
      </Button>
    }>
      <Table
        columns={columns}
        dataSource={permissions}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 20 }}
        scroll={{ y: 600 }}
      />

      <Modal
        title={editingPermission ? '编辑权限' : '新增权限'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        destroyOnClose
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="权限名称"
            rules={[{ required: true, message: '请输入权限名称' }]}
          >
            <Input placeholder="请输入权限名称，如：对话" />
          </Form.Item>
          <Form.Item
            name="code"
            label="权限编码"
            rules={[{ required: true, message: '请输入权限编码' }]}
          >
            <Input placeholder="请输入权限编码，如 MENU_CHAT" disabled={!!editingPermission} />
          </Form.Item>
          <Form.Item
            name="type"
            label="权限类型"
            rules={[{ required: true, message: '请选择权限类型' }]}
          >
            <Select
              options={[
                { label: '菜单', value: 'MENU' },
                { label: '按钮', value: 'BUTTON' },
                { label: 'API', value: 'API' },
              ]}
            />
          </Form.Item>
          <Form.Item name="parentId" label="父权限">
            <Select
              options={parentOptions}
              allowClear
              placeholder="选择父权限（可选）"
            />
          </Form.Item>
          <Form.Item name="path" label="路径">
            <Input placeholder="菜单路径或API路径，如 /chat" />
          </Form.Item>
          <Form.Item name="icon" label="图标">
            <Input placeholder="Ant Design 图标名称，如 MessageOutlined" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="请输入描述" rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default PermissionManagement;
