import { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  message,
  Popconfirm,
  Tag,
  Typography,
  Radio,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import { categoryApi } from '@/api/category.api';
import type { Category, CategoryRequest, CategoryType } from '@/types/category.types';
import type { ColumnsType } from 'antd/es/table';

const { Title } = Typography;

const CategoryList = () => {
  const [loading, setLoading] = useState(false);
  const [categories, setCategories] = useState<Category[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);
  const [filterType, setFilterType] = useState<CategoryType | undefined>(undefined);
  const [form] = Form.useForm();

  // 載入資料
  useEffect(() => {
    loadCategories();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filterType]);

  const loadCategories = async () => {
    setLoading(true);
    try {
      const data = await categoryApi.getCategories(filterType);
      setCategories(data);
    } catch {
      message.error('載入類別失敗');
    } finally {
      setLoading(false);
    }
  };

  // 新增類別
  const handleCreate = () => {
    setEditingCategory(null);
    form.resetFields();
    setModalVisible(true);
  };

  // 編輯類別
  const handleEdit = (record: Category) => {
    setEditingCategory(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  // 刪除類別
  const handleDelete = async (id: string) => {
    try {
      await categoryApi.deleteCategory(id);
      message.success('刪除成功');
      loadCategories();
    } catch {
      message.error('刪除失敗，可能有交易記錄使用此類別');
    }
  };

  // 提交表單
  const handleSubmit = async (values: CategoryRequest) => {
    try {
      if (editingCategory) {
        await categoryApi.updateCategory(editingCategory.id, values);
        message.success('更新成功');
      } else {
        await categoryApi.createCategory(values);
        message.success('新增成功');
      }

      setModalVisible(false);
      loadCategories();
    } catch {
      message.error(editingCategory ? '更新失敗' : '新增失敗');
    }
  };

  // 表格欄位
  const columns: ColumnsType<Category> = [
    {
      title: '類別名稱',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: '類型',
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type: CategoryType) => (
        <Tag color={type === 'INCOME' ? 'green' : 'red'}>
          {type === 'INCOME' ? '收入' : '支出'}
        </Tag>
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            編輯
          </Button>
          <Popconfirm
            title="確定要刪除嗎？"
            description="刪除後將無法復原"
            onConfirm={() => handleDelete(record.id)}
            okText="確定"
            cancelText="取消"
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              刪除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* 標題與操作按鈕 */}
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={2}>類別管理</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新增類別
        </Button>
      </div>

      {/* 篩選器 */}
      <Space style={{ marginBottom: 16 }}>
        <Radio.Group
          value={filterType}
          onChange={(e) => setFilterType(e.target.value)}
        >
          <Radio.Button value={undefined}>全部</Radio.Button>
          <Radio.Button value="INCOME">收入</Radio.Button>
          <Radio.Button value="EXPENSE">支出</Radio.Button>
        </Radio.Group>
      </Space>

      {/* 表格 */}
      <Table
        columns={columns}
        dataSource={categories}
        rowKey="id"
        loading={loading}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 筆`,
        }}
      />

      {/* 新增/編輯 Modal */}
      <Modal
        title={editingCategory ? '編輯類別' : '新增類別'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={null}
        width={500}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
        >
          <Form.Item
            label="類別名稱"
            name="name"
            rules={[
              { required: true, message: '請輸入類別名稱' },
              { max: 50, message: '類別名稱不能超過 50 個字元' },
            ]}
          >
            <Input placeholder="例如：薪資、餐飲、交通" />
          </Form.Item>

          <Form.Item
            label="類型"
            name="type"
            rules={[{ required: true, message: '請選擇類型' }]}
          >
            <Radio.Group>
              <Radio value="INCOME">收入</Radio>
              <Radio value="EXPENSE">支出</Radio>
            </Radio.Group>
          </Form.Item>

          <Form.Item
            label="描述"
            name="description"
          >
            <Input.TextArea
              placeholder="選填，例如：每月固定薪資收入"
              rows={3}
              maxLength={200}
              showCount
            />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                {editingCategory ? '更新' : '新增'}
              </Button>
              <Button onClick={() => setModalVisible(false)}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default CategoryList;