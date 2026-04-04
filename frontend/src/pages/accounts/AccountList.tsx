import { useState, useEffect, type ReactNode } from 'react';
import {
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  InputNumber,
  Select,
  message,
  Popconfirm,
  Tag,
  Typography,
  Card,
  Row,
  Col,
  Statistic,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  BankOutlined,
  WalletOutlined,
  CreditCardOutlined,
  LineChartOutlined,
} from '@ant-design/icons';
import { accountApi } from '@/api/account.api';
import type { Account, AccountRequest, AccountType } from '@/types/account.types';
import type { ColumnsType } from 'antd/es/table';
import { formatCurrency } from '@/utils/format';

const { Title } = Typography;

// 帳戶類型對應
const accountTypeConfig: Record<AccountType, { label: string; color: string; icon: ReactNode }> = {
  CASH: { label: '現金', color: 'green', icon: <WalletOutlined /> },
  BANK: { label: '銀行', color: 'blue', icon: <BankOutlined /> },
  CREDIT_CARD: { label: '信用卡', color: 'orange', icon: <CreditCardOutlined /> },
  INVESTMENT: { label: '投資', color: 'purple', icon: <LineChartOutlined /> },
};

const AccountList = () => {
  const [loading, setLoading] = useState(false);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingAccount, setEditingAccount] = useState<Account | null>(null);
  const [form] = Form.useForm();

  // 載入資料
  useEffect(() => {
    loadAccounts();
  }, []);

  const loadAccounts = async () => {
    setLoading(true);
    try {
      const data = await accountApi.getAccounts();
      setAccounts(data);
    } catch {
      message.error('載入帳戶失敗');
    } finally {
      setLoading(false);
    }
  };

  // 新增帳戶
  const handleCreate = () => {
    setEditingAccount(null);
    form.resetFields();
    form.setFieldsValue({ currency: 'TWD' }); // 預設幣別
    setModalVisible(true);
  };

  // 編輯帳戶
  const handleEdit = (record: Account) => {
    setEditingAccount(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  // 刪除帳戶
  const handleDelete = async (id: string) => {
    try {
      await accountApi.deleteAccount(id);
      message.success('刪除成功');
      loadAccounts();
    } catch {
      message.error('刪除失敗，可能有交易記錄或資產使用此帳戶');
    }
  };

  // 提交表單
  const handleSubmit = async (values: AccountRequest) => {
    try {
      if (editingAccount) {
        await accountApi.updateAccount(editingAccount.id, values);
        message.success('更新成功');
      } else {
        await accountApi.createAccount(values);
        message.success('新增成功');
      }

      setModalVisible(false);
      loadAccounts();
    } catch {
      message.error(editingAccount ? '更新失敗' : '新增失敗');
    }
  };

  // 計算總資產
  const totalBalance = accounts.reduce((sum, acc) => sum + acc.currentBalance, 0);

  // 表格欄位
  const columns: ColumnsType<Account> = [
    {
      title: '帳戶名稱',
      dataIndex: 'name',
      key: 'name',
      width: 200,
      render: (name: string, record: Account) => (
        <Space>
          {accountTypeConfig[record.type].icon}
          <span>{name}</span>
        </Space>
      ),
    },
    {
      title: '類型',
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type: AccountType) => (
        <Tag color={accountTypeConfig[type].color}>
          {accountTypeConfig[type].label}
        </Tag>
      ),
    },
    {
      title: '初始餘額',
      dataIndex: 'initialBalance',
      key: 'initialBalance',
      width: 150,
      align: 'right',
      render: (balance: number) => formatCurrency(balance),
    },
    {
      title: '當前餘額',
      dataIndex: 'currentBalance',
      key: 'currentBalance',
      width: 150,
      align: 'right',
      render: (balance: number) => (
        <span style={{ color: balance >= 0 ? '#52c41a' : '#ff4d4f', fontWeight: 'bold' }}>
          {formatCurrency(balance)}
        </span>
      ),
      sorter: (a, b) => a.currentBalance - b.currentBalance,
    },
    {
      title: '幣別',
      dataIndex: 'currency',
      key: 'currency',
      width: 80,
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
        <Title level={2}>帳戶管理</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新增帳戶
        </Button>
      </div>

      {/* 統計卡片 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="總資產"
              value={totalBalance}
              precision={0}
              valueStyle={{ color: totalBalance >= 0 ? '#3f8600' : '#cf1322' }}
              prefix="$"
              suffix="TWD"
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="帳戶數量"
              value={accounts.length}
              prefix={<BankOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="現金帳戶"
              value={accounts.filter(a => a.type === 'CASH').length}
              prefix={<WalletOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="投資帳戶"
              value={accounts.filter(a => a.type === 'INVESTMENT').length}
              prefix={<LineChartOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* 表格 */}
      <Table
        columns={columns}
        dataSource={accounts}
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
        title={editingAccount ? '編輯帳戶' : '新增帳戶'}
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
            label="帳戶名稱"
            name="name"
            rules={[
              { required: true, message: '請輸入帳戶名稱' },
              { max: 100, message: '帳戶名稱不能超過 100 個字元' },
            ]}
          >
            <Input placeholder="例如：中國信託支票帳戶" />
          </Form.Item>

          <Form.Item
            label="帳戶類型"
            name="type"
            rules={[{ required: true, message: '請選擇帳戶類型' }]}
          >
            <Select placeholder="請選擇帳戶類型">
              {Object.entries(accountTypeConfig).map(([key, config]) => (
                <Select.Option key={key} value={key}>
                  <Space>
                    {config.icon}
                    <span>{config.label}</span>
                  </Space>
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            label="初始餘額"
            name="initialBalance"
            rules={[
              { required: true, message: '請輸入初始餘額' },
              { type: 'number', message: '請輸入有效數字' },
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              placeholder="請輸入初始餘額"
              precision={2}
              formatter={(value) => `$ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={(value) => value!.replace(/\$\s?|(,*)/g, '') as string & number}
            />
          </Form.Item>

          <Form.Item
            label="幣別"
            name="currency"
            rules={[{ required: true, message: '請選擇幣別' }]}
          >
            <Select placeholder="請選擇幣別">
              <Select.Option value="TWD">TWD (新台幣)</Select.Option>
              <Select.Option value="USD">USD (美元)</Select.Option>
              <Select.Option value="JPY">JPY (日圓)</Select.Option>
              <Select.Option value="EUR">EUR (歐元)</Select.Option>
              <Select.Option value="CNY">CNY (人民幣)</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            label="描述"
            name="description"
          >
            <Input.TextArea
              placeholder="選填，例如：日常開銷使用"
              rows={3}
              maxLength={200}
              showCount
            />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                {editingAccount ? '更新' : '新增'}
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

export default AccountList;