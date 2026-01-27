import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  message,
  Space,
  Popconfirm,
  Tag,
  Tree,
  Card,
  Row,
  Col,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SettingOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  getRoleList,
  createRole,
  updateRole,
  deleteRole,
  updateRolePermissions,
  getAllPermissions,
  getRoleById,
  Role,
  Permission,
} from '@/services/role';

const RoleManagement: React.FC = () => {
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [permissionModalVisible, setPermissionModalVisible] = useState(false);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [selectedPermissionKeys, setSelectedPermissionKeys] = useState<number[]>([]);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchRoles();
    fetchPermissions();
  }, []);

  const fetchRoles = async () => {
    setLoading(true);
    try {
      const res = await getRoleList();
      if (res.code === 200) {
        setRoles(res.data || []);
      }
    } catch (error) {
      message.error('获取角色列表失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchPermissions = async () => {
    try {
      const res = await getAllPermissions();
      if (res.code === 200) {
        setPermissions(res.data || []);
      }
    } catch (error) {
      message.error('获取权限列表失败');
    }
  };

  const handleAdd = () => {
    setEditingRole(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record: Role) => {
    setEditingRole(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      const res = await deleteRole(id);
      if (res.code === 200) {
        message.success('删除成功');
        fetchRoles();
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
      const roleData: Role = {
        ...values,
        id: editingRole?.id,
        tenantId: 1,
        status: true,
      };

      const res = editingRole ? await updateRole(roleData) : await createRole(roleData);

      if (res.code === 200) {
        message.success(editingRole ? '更新成功' : '创建成功');
        setModalVisible(false);
        fetchRoles();
      } else {
        message.error(res.msg || '操作失败');
      }
    } catch (error) {
      console.error('Validation failed:', error);
    }
  };

  const handlePermissionConfig = async (record: Role) => {
    setEditingRole(record);
    // 获取角色详情（包含权限）
    try {
      const res = await getRoleById(record.id!);
      if (res.code === 200 && res.data) {
        setSelectedPermissionKeys(res.data.permissionIds || []);
      }
    } catch (error) {
      setSelectedPermissionKeys([]);
    }
    setPermissionModalVisible(true);
  };

  const handlePermissionSubmit = async () => {
    if (!editingRole?.id) return;

    try {
      const res = await updateRolePermissions(editingRole.id, selectedPermissionKeys);
      if (res.code === 200) {
        message.success('权限配置成功');
        setPermissionModalVisible(false);
        fetchRoles();
      } else {
        message.error(res.msg || '权限配置失败');
      }
    } catch (error) {
      message.error('权限配置失败');
    }
  };

  const columns: ColumnsType<Role> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 80,
    },
    {
      title: '角色名称',
      dataIndex: 'name',
    },
    {
      title: '角色编码',
      dataIndex: 'code',
    },
    {
      title: '描述',
      dataIndex: 'description',
      ellipsis: true,
    },
    {
      title: '类型',
      dataIndex: 'isSystem',
      width: 100,
      render: (isSystem: boolean) =>
        isSystem ? <Tag color="blue">系统</Tag> : <Tag color="green">自定义</Tag>,
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
      width: 200,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<SettingOutlined />}
            onClick={() => handlePermissionConfig(record)}
          >
            权限
          </Button>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
            disabled={record.isSystem}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除该角色吗？"
            onConfirm={() => handleDelete(record.id!)}
            disabled={record.isSystem}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />} disabled={record.isSystem}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  // 构建权限树数据
  const buildTreeData = (perms: Permission[]): any[] => {
    const menuPerms = perms.filter((p) => p.type === 'MENU');
    const apiPerms = perms.filter((p) => p.type === 'API');

    return [
      {
        key: 'menu-root',
        title: '菜单权限',
        children: menuPerms.map((p) => ({
          key: p.id,
          title: `${p.name} (${p.code})`,
        })),
      },
      {
        key: 'api-root',
        title: 'API权限',
        children: apiPerms.map((p) => ({
          key: p.id,
          title: `${p.name} (${p.code})`,
        })),
      },
    ];
  };

  return (
    <Card title="角色管理" extra={
      <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
        新增角色
      </Button>
    }>
      <Table
        columns={columns}
        dataSource={roles}
        rowKey="id"
        loading={loading}
        pagination={false}
      />

      {/* 角色编辑弹窗 */}
      <Modal
        title={editingRole ? '编辑角色' : '新增角色'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="角色名称"
            rules={[{ required: true, message: '请输入角色名称' }]}
          >
            <Input placeholder="请输入角色名称" />
          </Form.Item>
          <Form.Item
            name="code"
            label="角色编码"
            rules={[{ required: true, message: '请输入角色编码' }]}
          >
            <Input placeholder="请输入角色编码，如 ADMIN" disabled={!!editingRole} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="请输入描述" rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 权限配置弹窗 */}
      <Modal
        title={`配置权限 - ${editingRole?.name}`}
        open={permissionModalVisible}
        onOk={handlePermissionSubmit}
        onCancel={() => setPermissionModalVisible(false)}
        width={600}
        destroyOnClose
      >
        <Tree
          checkable
          defaultExpandAll
          checkedKeys={selectedPermissionKeys}
          onCheck={(checked: any) => {
            // 过滤掉非数字 key（如 'menu-root'）
            const numericKeys = (Array.isArray(checked) ? checked : checked.checked).filter(
              (k: any) => typeof k === 'number',
            );
            setSelectedPermissionKeys(numericKeys);
          }}
          treeData={buildTreeData(permissions)}
        />
      </Modal>
    </Card>
  );
};

export default RoleManagement;
