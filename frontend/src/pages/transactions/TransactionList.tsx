import { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  InputNumber,
  DatePicker,
  Select,
  message,
  Popconfirm,
  Tag,
  Typography,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { transactionApi } from '@/api/transaction.api';
import { categoryApi } from '@/api/category.api';
import { accountApi } from '@/api/account.api';
import type { Transaction, TransactionRequest } from '@/types/transaction.types';
import type { Category } from '@/types/category.types';
import type { Account } from '@/types/account.types';
import type { ColumnsType } from 'antd/es/table';
import { formatCurrency, formatDate } from '@/utils/format';

const { Title } = Typography;
const { RangePicker } = DatePicker;

const TransactionList = () => {
  const [loading, setLoading] = useState(false);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState<Transaction | null>(null);
  const [form] = Form.useForm();

  // 篩選條件
  const [filters, setFilters] = useState<{
  categoryId?: string;
  accountId?: string;
  startDate?: string;
  endDate?: string;
}>({
  categoryId: undefined,
  accountId: undefined,
  startDate: undefined,
  endDate: undefined,
});

  // 載入資料
  useEffect(() => {
    loadTransactions();
    loadCategories();
    loadAccounts();
  }, [filters]);

  const loadTransactions = async () => {
    setLoading(true);
    try {
      const response = await transactionApi.getTransactions({
        ...filters,
      });
      setTransactions(response.content || response as any); // 處理分頁或陣列回應
    } catch (error) {
      message.error('載入交易記錄失敗');
    } finally {
      setLoading(false);
    }
  };

  const loadCategories = async () => {
    try {
      const data = await categoryApi.getCategories();
      setCategories(data);
    } catch (error) {
      console.error('載入類別失敗', error);
    }
  };

  const loadAccounts = async () => {
    try {
      const data = await accountApi.getAccounts();
      setAccounts(data);
    } catch (error) {
      console.error('載入帳戶失敗', error);
    }
  };

  // 新增交易
  const handleCreate = () => {
    setEditingTransaction(null);
    form.resetFields();
    setModalVisible(true);
  };

  // 編輯交易
  const handleEdit = (record: Transaction) => {
    setEditingTransaction(record);
    form.setFieldsValue({
      ...record,
      date: dayjs(record.date),
    });
    setModalVisible(true);
  };

  // 刪除交易
  const handleDelete = async (id: string) => {
    try {
      await transactionApi.deleteTransaction(id);
      message.success('刪除成功');
      loadTransactions();
    } catch (error) {
      message.error('刪除失敗');
    }
  };

  // 提交表單
  const handleSubmit = async (values: any) => {
    try {
      const requestData: TransactionRequest = {
        ...values,
        date: values.date.format('YYYY-MM-DD'),
      };

      if (editingTransaction) {
        await transactionApi.updateTransaction(editingTransaction.id, requestData);
        message.success('更新成功');
      } else {
        await transactionApi.createTransaction(requestData);
        message.success('新增成功');
      }

      setModalVisible(false);
      loadTransactions();
    } catch (error) {
      message.error(editingTransaction ? '更新失敗' : '新增失敗');
    }
  };

  // 表格欄位
  const columns: ColumnsType<Transaction> = [
    {
      title: '日期',
      dataIndex: 'date',
      key: 'date',
      width: 120,
      render: (date: string) => formatDate(date),
      sorter: (a, b) => dayjs(a.date).unix() - dayjs(b.date).unix(),
    },
    {
      title: '類別',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 120,
      render: (name: string, record: Transaction) => (
        <Tag color={record.categoryType === 'INCOME' ? 'green' : 'red'}>
          {name || '未分類'}
        </Tag>
      ),
    },
    {
      title: '帳戶',
      dataIndex: 'accountName',
      key: 'accountName',
      width: 120,
    },
    {
      title: '金額',
      dataIndex: 'amount',
      key: 'amount',
      width: 120,
      align: 'right',
      render: (amount: number, record: Transaction) => (
        <span style={{ color: record.categoryType === 'INCOME' ? '#52c41a' : '#ff4d4f' }}>
          {record.categoryType === 'INCOME' ? '+' : '-'}
          {formatCurrency(amount)}
        </span>
      ),
      sorter: (a, b) => a.amount - b.amount,
    },
    {
      title: '備註',
      dataIndex: 'note',
      key: 'note',
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
        <Title level={2}>交易記錄</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新增交易
        </Button>
      </div>

      {/* 篩選器 */}
      <Space style={{ marginBottom: 16 }} wrap>
        <Select
          placeholder="選擇類別"
          style={{ width: 150 }}
          allowClear
          onChange={(value) => setFilters({ ...filters, categoryId: value })}
        >
          {categories.map((cat) => (
            <Select.Option key={cat.id} value={cat.id}>
              {cat.name}
            </Select.Option>
          ))}
        </Select>

        <Select
          placeholder="選擇帳戶"
          style={{ width: 150 }}
          allowClear
          onChange={(value) => setFilters({ ...filters, accountId: value })}
        >
          {accounts.map((acc) => (
            <Select.Option key={acc.id} value={acc.id}>
              {acc.name}
            </Select.Option>
          ))}
        </Select>

        <RangePicker
          onChange={(dates) => {
            setFilters({
              ...filters,
              startDate: dates?.[0]?.format('YYYY-MM-DD'),
              endDate: dates?.[1]?.format('YYYY-MM-DD'),
            });
          }}
        />

        <Button icon={<SearchOutlined />} onClick={loadTransactions}>
          查詢
        </Button>
      </Space>

      {/* 表格 */}
      <Table
        columns={columns}
        dataSource={transactions}
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
        title={editingTransaction ? '編輯交易' : '新增交易'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={null}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
        >
          <Form.Item
            label="日期"
            name="date"
            rules={[{ required: true, message: '請選擇日期' }]}
            initialValue={dayjs()}
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            label="類別"
            name="categoryId"
            rules={[{ required: true, message: '請選擇類別' }]}
          >
            <Select placeholder="請選擇類別">
              {categories.map((cat) => (
                <Select.Option key={cat.id} value={cat.id}>
                  <Tag color={cat.type === 'INCOME' ? 'green' : 'red'}>
                    {cat.type === 'INCOME' ? '收入' : '支出'}
                  </Tag>
                  {cat.name}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            label="帳戶"
            name="accountId"
          >
            <Select placeholder="請選擇帳戶" allowClear>
              {accounts.map((acc) => (
                <Select.Option key={acc.id} value={acc.id}>
                  {acc.name}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            label="金額"
            name="amount"
            rules={[
              { required: true, message: '請輸入金額' },
              { type: 'number', min: 0.01, message: '金額必須大於 0' },
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              placeholder="請輸入金額"
              precision={2}
              min={0}
            />
          </Form.Item>

          <Form.Item
            label="備註"
            name="note"
            rules={[{ required: true, message: '請輸入備註' }]}
          >
            <Input.TextArea
              placeholder="請輸入備註"
              rows={3}
            />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                {editingTransaction ? '更新' : '新增'}
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

export default TransactionList;