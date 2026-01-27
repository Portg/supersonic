import React, { useEffect, useState } from 'react';
import { Card, Tag, Descriptions, Spin, Alert, Empty } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, LockOutlined } from '@ant-design/icons';
import {
  checkDataSetPermission,
  getDataSetRowFilters,
  PermissionCheckResult,
} from '@/services/datasetAuth';

type Props = {
  datasetId: number;
};

const PermissionStatus: React.FC<Props> = ({ datasetId }) => {
  const [loading, setLoading] = useState(false);
  const [permissionStatus, setPermissionStatus] = useState<PermissionCheckResult | null>(null);
  const [rowFilters, setRowFilters] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);

  const fetchPermissionStatus = async () => {
    if (!datasetId) {
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [permResult, filterResult] = await Promise.all([
        checkDataSetPermission(datasetId),
        getDataSetRowFilters(datasetId),
      ]);

      if (permResult.code === 200) {
        setPermissionStatus(permResult.data);
      } else {
        setError(permResult.msg || '获取权限状态失败');
      }

      if (filterResult.code === 200) {
        setRowFilters(filterResult.data || []);
      }
    } catch (err: any) {
      setError(err.message || '获取权限状态失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPermissionStatus();
  }, [datasetId]);

  if (loading) {
    return (
      <Card title="当前用户权限状态" style={{ marginBottom: 16 }}>
        <Spin tip="加载中..." />
      </Card>
    );
  }

  if (error) {
    return (
      <Card title="当前用户权限状态" style={{ marginBottom: 16 }}>
        <Alert message={error} type="error" />
      </Card>
    );
  }

  if (!permissionStatus) {
    return (
      <Card title="当前用户权限状态" style={{ marginBottom: 16 }}>
        <Empty description="无权限信息" />
      </Card>
    );
  }

  return (
    <Card title="当前用户权限状态" style={{ marginBottom: 16 }}>
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label="查看权限">
          {permissionStatus.hasViewPermission ? (
            <Tag icon={<CheckCircleOutlined />} color="success">
              已授权
            </Tag>
          ) : (
            <Tag icon={<CloseCircleOutlined />} color="error">
              未授权
            </Tag>
          )}
        </Descriptions.Item>
        <Descriptions.Item label="管理权限">
          {permissionStatus.hasAdminPermission ? (
            <Tag icon={<CheckCircleOutlined />} color="success">
              已授权
            </Tag>
          ) : (
            <Tag icon={<CloseCircleOutlined />} color="default">
              未授权
            </Tag>
          )}
        </Descriptions.Item>
        <Descriptions.Item label="行权限过滤">
          {rowFilters.length > 0 ? (
            <div>
              <Tag icon={<LockOutlined />} color="warning">
                数据已过滤
              </Tag>
              <div style={{ marginTop: 8 }}>
                <Alert
                  type="info"
                  message="当前数据集应用了以下行权限过滤条件："
                  description={
                    <ul style={{ margin: '8px 0', paddingLeft: 20 }}>
                      {rowFilters.map((filter, index) => (
                        <li key={index}>
                          <code style={{ background: '#f5f5f5', padding: '2px 6px' }}>
                            {filter}
                          </code>
                        </li>
                      ))}
                    </ul>
                  }
                />
              </div>
            </div>
          ) : (
            <Tag color="default">无过滤</Tag>
          )}
        </Descriptions.Item>
      </Descriptions>
    </Card>
  );
};

export default PermissionStatus;
