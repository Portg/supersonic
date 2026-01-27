import React, { useEffect } from 'react';
import { Modal, Form, Input, Select, message } from 'antd';
import {
  SemanticTemplate,
  createTemplate,
  updateTemplate,
  saveBuiltinTemplate,
} from '@/services/semanticTemplate';

interface TemplateFormModalProps {
  visible: boolean;
  template: SemanticTemplate | null;
  onCancel: () => void;
  onSuccess: () => void;
}

const TEMPLATE_CATEGORIES = [
  { label: '访问统计', value: 'VISITS' },
  { label: '歌手/音乐', value: 'SINGER' },
  { label: '企业/商业', value: 'COMPANY' },
  { label: '电商', value: 'ECOMMERCE' },
];

const TemplateFormModal: React.FC<TemplateFormModalProps> = ({
  visible,
  template,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const isEdit = !!template;

  useEffect(() => {
    if (visible) {
      if (template) {
        form.setFieldsValue({
          name: template.name,
          bizName: template.bizName,
          category: template.category,
          description: template.description,
        });
      } else {
        form.resetFields();
      }
    }
  }, [visible, template]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const templateData: Partial<SemanticTemplate> = {
        ...values,
        templateConfig: template?.templateConfig || {
          domain: {
            name: values.name,
            bizName: values.bizName,
            description: values.description,
          },
          models: [],
          dataSet: {
            name: `${values.name}数据集`,
            bizName: `${values.bizName}_dataset`,
          },
          agent: {
            name: `${values.name}分析助手`,
            description: `${values.name}的AI分析助手`,
            enableSearch: true,
          },
          configParams: [],
        },
      };

      let res: any;
      if (isEdit) {
        if (template!.isBuiltin) {
          // 内置模板使用专用 API
          res = await saveBuiltinTemplate(templateData);
        } else {
          res = await updateTemplate(template!.id, templateData);
        }
      } else {
        res = await createTemplate(templateData);
      }
      // API response is wrapped in { code, data, msg } format
      if (res?.code === 200) {
        message.success(`模板${isEdit ? '更新' : '创建'}成功`);
        onSuccess();
      } else {
        message.error(res?.msg || `${isEdit ? '更新' : '创建'}模板失败`);
      }
    } catch (error: any) {
      if (error.errorFields) {
        return;
      }
      message.error(`${isEdit ? '更新' : '创建'}模板失败`);
    }
  };

  return (
    <Modal
      title={isEdit ? '编辑模板' : '创建模板'}
      open={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      okText={isEdit ? '更新' : '创建'}
      cancelText="取消"
      width={600}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label="模板名称"
          rules={[{ required: true, message: '请输入模板名称' }]}
        >
          <Input placeholder="例如: 产品分析模板" />
        </Form.Item>

        <Form.Item
          name="bizName"
          label="模板代码"
          rules={[
            { required: true, message: '请输入模板代码' },
            {
              pattern: /^[a-z][a-z0-9_]*$/,
              message: '代码必须以小写字母开头，只能包含小写字母、数字和下划线',
            },
          ]}
        >
          <Input placeholder="例如: product_analytics" disabled={isEdit} />
        </Form.Item>

        <Form.Item
          name="category"
          label="模板类别"
          rules={[{ required: true, message: '请选择模板类别' }]}
        >
          <Select placeholder="请选择模板类别" options={TEMPLATE_CATEGORIES} />
        </Form.Item>

        <Form.Item name="description" label="描述">
          <Input.TextArea rows={3} placeholder="描述这个模板的用途..." />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default TemplateFormModal;
