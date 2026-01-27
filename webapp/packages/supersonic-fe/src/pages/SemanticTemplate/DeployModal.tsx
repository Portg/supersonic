import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Select,
  Input,
  Alert,
  Divider,
  Button,
  Space,
  message,
  Spin,
  Checkbox,
} from 'antd';
import {
  SemanticTemplate,
  SemanticDeployParam,
  SemanticPreviewResult,
  SemanticDeployment,
  previewDeployment,
  executeDeployment,
} from '@/services/semanticTemplate';
import { getDatabaseList } from '@/pages/SemanticModel/service';
import { saveAgent } from '@/pages/Agent/service';
import { AgentToolTypeEnum, ChatAppConfig } from '@/pages/Agent/type';
import { getLlmModelAppList, getLlmList } from '@/services/system';

interface DeployModalProps {
  visible: boolean;
  template: SemanticTemplate | null;
  onCancel: () => void;
  onSuccess: () => void;
  onPreviewResult: (data: SemanticPreviewResult) => void;
}

const DeployModal: React.FC<DeployModalProps> = ({
  visible,
  template,
  onCancel,
  onSuccess,
  onPreviewResult,
}) => {
  const [form] = Form.useForm();
  const [databases, setDatabases] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [deployLoading, setDeployLoading] = useState(false);

  useEffect(() => {
    if (visible) {
      loadDatabases();
      // Set default values for config params
      if (template?.templateConfig?.configParams) {
        const defaultValues: Record<string, string> = {};
        template.templateConfig.configParams.forEach((param) => {
          if (param.defaultValue) {
            defaultValues[param.key] = param.defaultValue;
          }
        });
        form.setFieldsValue({ params: defaultValues });
      }
    }
  }, [visible, template]);

  const loadDatabases = async () => {
    setLoading(true);
    try {
      const res = await getDatabaseList();
      setDatabases(res.data || []);
    } catch (error) {
      message.error('加载数据库列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handlePreview = async () => {
    try {
      const values = await form.validateFields();
      setPreviewLoading(true);
      const param: SemanticDeployParam = {
        databaseId: values.databaseId,
        params: values.params || {},
      };
      const res: any = await previewDeployment(template!.id, param);
      // API response is wrapped in { code, data, msg } format
      if (res?.code === 200 && res?.data) {
        onPreviewResult(res.data);
      } else {
        message.error(res?.msg || '预览部署失败');
      }
    } catch (error: any) {
      if (error.errorFields) {
        // Form validation error
        return;
      }
      message.error('预览部署失败');
    } finally {
      setPreviewLoading(false);
    }
  };

  const handleDeploy = async () => {
    try {
      const values = await form.validateFields();
      setDeployLoading(true);
      const param: SemanticDeployParam = {
        databaseId: values.databaseId,
        params: values.params || {},
      };
      const res: any = await executeDeployment(template!.id, param);
      // API response is wrapped in { code, data, msg } format
      if (res?.code === 200) {
        const deployment: SemanticDeployment = res.data;

        // Auto-create Agent if agentConfig is returned and user wants to create it
        if (values.createAgent && deployment.resultDetail?.agentConfig) {
          try {
            await createAgentFromConfig(deployment.resultDetail.agentConfig);
            message.success('部署成功，Agent已创建');
          } catch (agentError) {
            message.warning('部署成功，但创建Agent失败，请手动创建');
          }
        } else {
          message.success('部署成功');
        }

        onSuccess();
      } else {
        message.error(res?.msg || '执行部署失败');
      }
    } catch (error: any) {
      if (error.errorFields) {
        // Form validation error
        return;
      }
      message.error('执行部署失败');
    } finally {
      setDeployLoading(false);
    }
  };

  const createAgentFromConfig = async (agentConfig: any) => {
    const toolConfig = {
      tools: [
        {
          id: `dataset_${agentConfig.dataSetId}`,
          type: AgentToolTypeEnum.DATASET,
          name: agentConfig.dataSetName || agentConfig.name,
          dataSetIds: [agentConfig.dataSetId],
        },
      ],
    };

    // 获取默认的 ChatApp 配置
    let chatAppConfig: ChatAppConfig = {};
    try {
      // 获取 LLM 模型列表，选择第一个作为默认
      const llmListRes: any = await getLlmList();
      let defaultLlmId: number | undefined;
      if (llmListRes?.code === 200 && llmListRes?.data?.length > 0) {
        defaultLlmId = llmListRes.data[0].id;
      } else {
        console.warn('没有可用的 LLM 模型配置，Agent 的 LLM 功能可能无法正常使用');
      }

      // 获取系统预定义的 ChatApp 配置
      const appListRes: any = await getLlmModelAppList();
      if (appListRes?.code === 200 && appListRes?.data) {
        chatAppConfig = Object.keys(appListRes.data).reduce(
          (config: ChatAppConfig, key: string) => {
            const appConfig = appListRes.data[key];
            return {
              ...config,
              [key]: {
                ...appConfig,
                // 设置默认的 LLM 模型 ID（如果有的话）
                chatModelId: defaultLlmId,
              },
            };
          },
          {},
        );
      }
    } catch (e) {
      console.warn('Failed to load ChatApp config, using empty config', e);
    }

    const agent = {
      name: agentConfig.name,
      description: agentConfig.description,
      examples: agentConfig.examples || [],
      enableSearch: agentConfig.enableSearch ? 1 : 0,
      status: 1,
      toolConfig: JSON.stringify(toolConfig),
      chatAppConfig,
      isOpen: 0,
    };

    const agentRes: any = await saveAgent(agent as any);
    if (agentRes?.code !== 200) {
      throw new Error(agentRes?.msg || '创建Agent失败');
    }
    return agentRes.data;
  };

  const renderConfigParams = () => {
    const params = template?.templateConfig?.configParams || [];
    if (params.length === 0) {
      return null;
    }

    return (
      <>
        <Divider>配置参数</Divider>
        {params.map((param) => (
          <Form.Item
            key={param.key}
            name={['params', param.key]}
            label={param.name}
            rules={[{ required: param.required, message: `请输入${param.name}` }]}
            tooltip={param.description}
            initialValue={param.defaultValue}
          >
            {param.type === 'DATABASE' ? (
              <Select
                placeholder={`请选择${param.name}`}
                options={databases.map((d) => ({ label: d.name, value: d.id }))}
              />
            ) : (
              <Input placeholder={`请输入${param.name}`} />
            )}
          </Form.Item>
        ))}
      </>
    );
  };

  return (
    <Modal
      title={`部署模板: ${template?.name}`}
      open={visible}
      width={700}
      onCancel={onCancel}
      footer={
        <Space>
          <Button onClick={onCancel}>取消</Button>
          <Button loading={previewLoading} onClick={handlePreview}>
            预览
          </Button>
          <Button type="primary" loading={deployLoading} onClick={handleDeploy}>
            部署
          </Button>
        </Space>
      }
    >
      <Spin spinning={loading}>
        <Alert
          message="部署说明"
          description="部署将在选定的数据库中创建完整的语义层结构，包括主题域、模型、指标、数据集。如果勾选「自动创建 Agent」，还会创建一个可在 Chat 中使用的智能助手。"
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />

        <Form form={form} layout="vertical" initialValues={{ createAgent: true }}>
          <Form.Item
            name="databaseId"
            label="目标数据库"
            rules={[{ required: true, message: '请选择数据库' }]}
          >
            <Select placeholder="请选择部署的目标数据库">
              {databases.map((db) => (
                <Select.Option key={db.id} value={db.id}>
                  {db.name} ({db.type})
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          {renderConfigParams()}

          <Divider>Agent 配置</Divider>
          <Form.Item name="createAgent" valuePropName="checked">
            <Checkbox>自动创建 Agent（可在 Chat 中直接使用）</Checkbox>
          </Form.Item>
        </Form>
      </Spin>
    </Modal>
  );
};

export default DeployModal;
