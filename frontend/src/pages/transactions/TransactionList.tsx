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
  SwapOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { transactionApi } from '@/api/transaction.api';
import { categoryApi } from '@/api/category.api';
import { accountApi } from '@/api/account.api';
import type { Transaction, TransactionRequest, TransactionType } from '@/types/transaction.types';
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
  const [selectedType, setSelectedType] = useState<TransactionType>('EXPENSE');
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

  useEffect(() => {
    loadTransactions();
    loadCategories();
    loadAccounts();
  }, [filters]);

  const loadTransactions = async () => {
    setLoading(true);
    try {
      const response = await transactionApi.getTransactions({ ...filters });
      setTransactions(response.content || (response as any));
    } catch {
      message.error('載入交易記錄失敗');
    } finally {
      setLoading(false);
    }
  };

  const loadCategories = async () => {
    try {
      setCategories(await categoryApi.getCategories());
    } catch {
      console.error('載入類別失敗');
    }
  };

  const loadAccounts = async () => {
    try {
      setAccounts(await accountApi.getAccounts());
    } catch {
      console.error('載入帳戶失敗');
    }
  };

  const handleCreate = () => {
    setEditingTransaction(null);
    setSelectedType('EXPENSE');
    form.resetFields();
    form.setFieldsValue({ type: 'EXPENSE', date: dayjs() });
    setModalVisible(true);
  };

  const handleEdit = (record: Transaction) => {
    if (record.type === 'TRANSFER_OUT' || record.type === 'TRANSFER_IN') {
      message.warning('轉帳記錄不支援編輯，請刪除後重新建立');
      return;
    }
    setEditingTransaction(record);
    setSelectedType(record.type);
    form.setFieldsValue({ ...record, date: dayjs(record.date) });
    setModalVisible(true);
  };

  const handleDelete = async (id: string) => {
    try {
      await transactionApi.deleteTransaction(id);
      message.success('刪除成功');
      loadTransactions();
    } catch {
      message.error('刪除失敗');
    }
  };

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
    } catch {
      message.error(editingTransaction ? '更新失敗' : '新增失敗');
    }
  };

  // 欄位顏色與符號
  const amountColor = (type: TransactionType) => {
    if (type === 'INCOME') return '#52c41a';
    if (type === 'TRANSFER_OUT' || type === 'TRANSFER_IN') return '#1677ff';
    return '#ff4d4f';
  };

  const amountPrefix = (type: TransactionType) => {
    if (type === 'INCOME') return '+';
    if (type === 'TRANSFER_IN') return '+';
    return '-';
  };

  const columns: ColumnsType<Transaction> = [
    {
      title: '日期',
      dataIndex: 'date',
      key: 'date',
      width: 110,
      render: (date: string) => formatDate(date),
      sorter: (a, b) => dayjs(a.date).unix() - dayjs(b.date).unix(),
    },
    {
      title: '類型 / 類別',
      key: 'typeCategory',
      width: 160,
      render: (_, record) => {
        if (record.type === 'TRANSFER_OUT') {
          return (
            <Tag color="blue" icon={<SwapOutlined />}>
              {record.accountName} → {record.toAccountName ?? '?'}
            </Tag>
          );
        }
        if (record.type === 'TRANSFER_IN') {
          return (
            <Tag color="blue" icon={<SwapOutlined />}>
              轉入
            </Tag>
          );
        }
        return (
          <Tag color={record.type === 'INCOME' ? 'green' : 'red'}>
            {record.categoryName ?? '未分類'}
          </Tag>
        );
      },
    },
    {
      title: '帳戶',
      dataIndex: 'accountName',
      key: 'accountName',
      width: 110,
    },
    {
      title: '金額',
      dataIndex: 'amount',
      key: 'amount',
      width: 120,
      align: 'right',
      render: (amount: number, record) => (
        <span style={{ color: amountColor(record.type) }}>
          {amountPrefix(record.type)}{formatCurrency(amount)}
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
            disabled={record.type === 'TRANSFER_IN'}
          >
            編輯
          </Button>
          <Popconfirm
            title={
              record.type === 'TRANSFER_OUT'
                ? '刪除轉帳將同時刪除配對的轉入記錄，確定嗎？'
                : '確定要刪除嗎？'
            }
            onConfirm={() => handleDelete(record.id)}
            okText="確定"
            cancelText="取消"
          >
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
              disabled={record.type === 'TRANSFER_IN'}
            >
              刪除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const isTransfer = selectedType === 'TRANSFER_OUT';

  return (
    <div>
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
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item
            label="日期"
            name="date"
            rules={[{ required: true, message: '請選擇日期' }]}
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            label="交易類型"
            name="type"
            rules={[{ required: true, message: '請選擇交易類型' }]}
          >
            <Select
              onChange={(v: TransactionType) => {
                setSelectedType(v);
                form.setFieldValue('categoryId', undefined);
                form.setFieldValue('toAccountId', undefined);
              }}
              disabled={!!editingTransaction}
            >
              <Select.Option value="INCOME">
                <Tag color="green">收入</Tag>
              </Select.Option>
              <Select.Option value="EXPENSE">
                <Tag color="red">支出</Tag>
              </Select.Option>
              <Select.Option value="TRANSFER_OUT">
                <Tag color="blue">轉帳</Tag>
              </Select.Option>
            </Select>
          </Form.Item>

          {/* 類別（收入/支出顯示） */}
          {!isTransfer && (
            <Form.Item
              label="類別"
              name="categoryId"
              rules={[{ required: true, message: '請選擇類別' }]}
            >
              <Select placeholder="請選擇類別">
                {categories
                  .filter((cat) =>
                    selectedType === 'INCOME' ? cat.type === 'INCOME' : cat.type === 'EXPENSE'
                  )
                  .map((cat) => (
                    <Select.Option key={cat.id} value={cat.id}>
                      {cat.name}
                    </Select.Option>
                  ))}
              </Select>
            </Form.Item>
          )}

          {/* 來源帳戶 */}
          <Form.Item
            label={isTransfer ? '來源帳戶' : '帳戶'}
            name="accountId"
            rules={isTransfer ? [{ required: true, message: '請選擇來源帳戶' }] : []}
          >
            <Select placeholder="請選擇帳戶" allowClear={!isTransfer}>
              {accounts.map((acc) => (
                <Select.Option key={acc.id} value={acc.id}>
                  {acc.name}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          {/* 目標帳戶（轉帳顯示） */}
          {isTransfer && (
            <Form.Item
              label="目標帳戶"
              name="toAccountId"
              rules={[{ required: true, message: '請選擇目標帳戶' }]}
            >
              <Select placeholder="請選擇目標帳戶">
                {accounts.map((acc) => (
                  <Select.Option key={acc.id} value={acc.id}>
                    {acc.name}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          )}

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

          <Form.Item label="備註" name="note">
            <Input.TextArea placeholder="請輸入備註" rows={3} />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                {editingTransaction ? '更新' : '新增'}
              </Button>
              <Button onClick={() => setModalVisible(false)}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default TransactionList;
