import { useState, useEffect, useCallback, useRef } from 'react';
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
  AutoComplete,
  Radio,
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
  CheckCircleOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { transactionApi } from '@/api/transaction.api';
import { categoryApi } from '@/api/category.api';
import { accountApi } from '@/api/account.api';
import { assetApi } from '@/api/asset.api';
import { priceApi } from '@/api/price.api';
import { knownAssetApi, type KnownAssetResult, type KnownAssetType } from '@/api/knownAsset.api';
import type { Transaction, TransactionRequest, TransactionType } from '@/types/transaction.types';
import type { Category } from '@/types/category.types';
import type { Account } from '@/types/account.types';
import type { Asset } from '@/types/asset.types';
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
  const [toAccountIsInvestment, setToAccountIsInvestment] = useState(false);
  const [investmentAssets, setInvestmentAssets] = useState<Asset[]>([]);
  const [assetLinkMode, setAssetLinkMode] = useState<'none' | 'existing' | 'new'>('none');
  const [assetSearchMarket, setAssetSearchMarket] = useState<KnownAssetType>('STOCK_TW');
  const [assetSearchOptions, setAssetSearchOptions] = useState<{ value: string; label: string; data: KnownAssetResult }[]>([]);
  const [selectedAssetDisplay, setSelectedAssetDisplay] = useState<string | null>(null);
  const [assetCurrentPrice, setAssetCurrentPrice] = useState<number | null>(null);
  const [assetCurrentPriceLoading, setAssetCurrentPriceLoading] = useState(false);
  const assetDebounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
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
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters]);

  const loadTransactions = async () => {
    setLoading(true);
    try {
      const response = await transactionApi.getTransactions({ ...filters });
      setTransactions(response.content);
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

  const resetAssetLinkState = () => {
    setToAccountIsInvestment(false);
    setInvestmentAssets([]);
    setAssetLinkMode('none');
    setAssetSearchMarket('STOCK_TW');
    setAssetSearchOptions([]);
    setSelectedAssetDisplay(null);
    setAssetCurrentPrice(null);
  };

  const clearSelectedAsset = () => {
    setSelectedAssetDisplay(null);
    setAssetCurrentPrice(null);
    setAssetSearchOptions([]);
    form.setFieldsValue({ assetSymbol: undefined, assetName: undefined, _assetSearchInput: undefined });
  };

  const recalcTransferAmount = () => {
    const qty: number | undefined = form.getFieldValue('assetQuantity');
    const price: number | undefined = form.getFieldValue('assetCostPrice');
    if (qty && price && qty > 0 && price > 0) {
      form.setFieldValue('amount', parseFloat((qty * price).toFixed(2)));
    }
  };

  const handleAssetSymbolSearch = useCallback((keyword: string) => {
    if (keyword.length < 1) { setAssetSearchOptions([]); return; }
    if (assetDebounceTimer.current) clearTimeout(assetDebounceTimer.current);
    assetDebounceTimer.current = setTimeout(async () => {
      try {
        const results = await knownAssetApi.search(assetSearchMarket, keyword);
        setAssetSearchOptions(results.map((r) => ({
          value: r.symbol,
          label: r.marketCapRank
            ? `${r.displayCode}  ${r.name}  #${r.marketCapRank}`
            : `${r.displayCode}  ${r.name}`,
          data: r,
        })));
      } catch { /* 靜默忽略 */ }
    }, 300);
  }, [assetSearchMarket]);

  const handleAssetSymbolSelect = useCallback(async (_value: string, option: { value: string; label: string; data: KnownAssetResult }) => {
    const assetType = form.getFieldValue('assetType') ?? 'STOCK';
    form.setFieldsValue({ assetSymbol: option.data.symbol, assetName: option.data.name });
    setSelectedAssetDisplay(`${option.data.name}（${option.data.symbol}）`);
    setAssetSearchOptions([]);
    setAssetCurrentPrice(null);
    setAssetCurrentPriceLoading(true);
    try {
      const data = await priceApi.getPrice(option.data.symbol, assetType);
      setAssetCurrentPrice(data.price);
    } catch {
      // 查不到價格不影響主流程，靜默忽略
    } finally {
      setAssetCurrentPriceLoading(false);
    }
  }, [form]);

  const handleToAccountChange = async (accountId: string) => {
    const account = accounts.find((a) => a.id === accountId);
    if (account?.type === 'INVESTMENT') {
      setToAccountIsInvestment(true);
      try {
        const assets = await assetApi.getAccountAssets(accountId);
        setInvestmentAssets(assets);
      } catch {
        setInvestmentAssets([]);
      }
    } else {
      setToAccountIsInvestment(false);
      setInvestmentAssets([]);
    }
    setAssetLinkMode('none');
    form.setFieldValue('assetId', undefined);
    form.setFieldValue('assetSymbol', undefined);
    form.setFieldValue('assetName', undefined);
    form.setFieldValue('assetQuantity', undefined);
    form.setFieldValue('_assetSearchInput', undefined);
    form.setFieldValue('_assetSearchMarket', 'STOCK_TW');
  };

  const handleCreate = () => {
    setEditingTransaction(null);
    setSelectedType('EXPENSE');
    resetAssetLinkState();
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

  const handleSubmit = async (values: TransactionRequest & { date: { format: (s: string) => string } }) => {
    try {
      const requestData: TransactionRequest = {
        ...values,
        date: values.date.format('YYYY-MM-DD'),
        // 不連結資產時清除相關欄位
        ...(assetLinkMode === 'none' && {
          assetId: undefined,
          assetSymbol: undefined,
          assetName: undefined,
          assetType: undefined,
          assetQuantity: undefined,
          assetCostPrice: undefined,
          assetNote: undefined,
        }),
        _assetSearchMarket: undefined,
        _assetSearchInput: undefined,
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
                resetAssetLinkState();
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
              <Select placeholder="請選擇目標帳戶" onChange={handleToAccountChange}>
                {accounts.map((acc) => (
                  <Select.Option key={acc.id} value={acc.id}>
                    {acc.name}
                    {acc.type === 'INVESTMENT' && (
                      <span style={{ color: '#1677ff', marginLeft: 6, fontSize: 12 }}>投資</span>
                    )}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          )}

          {/* 資產連結（目標為投資帳戶時顯示） */}
          {isTransfer && toAccountIsInvestment && (
            <>
              <Form.Item label="連結資產">
                <Select
                  value={assetLinkMode}
                  onChange={(v) => setAssetLinkMode(v)}
                >
                  <Select.Option value="none">不連結資產</Select.Option>
                  <Select.Option value="existing">加倉已有資產</Select.Option>
                  <Select.Option value="new">建立新資產</Select.Option>
                </Select>
              </Form.Item>

              {assetLinkMode === 'existing' && (
                <>
                  <Form.Item
                    label="選擇資產"
                    name="assetId"
                    rules={[{ required: true, message: '請選擇資產' }]}
                  >
                    <Select
                      placeholder="請選擇要加倉的資產"
                      onChange={(id: string) => {
                        const asset = investmentAssets.find((a) => a.id === id);
                        setAssetCurrentPrice(asset?.currentPrice ?? null);
                      }}
                    >
                      {investmentAssets.map((a) => (
                        <Select.Option key={a.id} value={a.id}>
                          {a.name}（{a.symbol}）持有 {a.quantity} 股
                        </Select.Option>
                      ))}
                    </Select>
                  </Form.Item>
                  {assetCurrentPrice !== null && (
                    <div style={{ marginTop: -16, marginBottom: 16, color: '#1677ff', fontSize: 13 }}>
                      當前市價：{formatCurrency(assetCurrentPrice)}
                    </div>
                  )}
                  <Form.Item
                    label="加倉數量"
                    name="assetQuantity"
                    rules={[
                      { required: true, message: '請輸入加倉數量' },
                      { type: 'number', min: 0.00000001, message: '數量必須大於 0' },
                    ]}
                  >
                    <InputNumber style={{ width: '100%' }} placeholder="新增持有數量" min={0} onChange={recalcTransferAmount} />
                  </Form.Item>
                  <Form.Item
                    label="單價"
                    name="assetCostPrice"
                    rules={[
                      { required: true, message: '請輸入單價' },
                      { type: 'number', min: 0.00000001, message: '單價必須大於 0' },
                    ]}
                  >
                    <InputNumber style={{ width: '100%' }} placeholder="這批買入的每單位價格" min={0} precision={2} onChange={recalcTransferAmount} />
                  </Form.Item>
                </>
              )}

              {assetLinkMode === 'new' && (
                <>
                  {/* hidden fields 儲存實際送出的值 */}
                  <Form.Item name="assetSymbol" hidden><AutoComplete /></Form.Item>
                  <Form.Item name="assetName" hidden><AutoComplete /></Form.Item>
                  <Form.Item name="assetType" initialValue="STOCK" hidden><AutoComplete /></Form.Item>

                  <Form.Item
                    label="資產市場"
                    name="_assetSearchMarket"
                    initialValue="STOCK_TW"
                  >
                    <Radio.Group onChange={(e) => {
                      const market: KnownAssetType = e.target.value;
                      form.setFieldsValue({
                        assetType: market === 'CRYPTO' ? 'CRYPTO' : 'STOCK',
                        assetSymbol: undefined,
                        assetName: undefined,
                        _assetSearchInput: undefined,
                      });
                      setAssetSearchMarket(market);
                      setAssetSearchOptions([]);
                      setSelectedAssetDisplay(null);
                      setAssetCurrentPrice(null);
                    }}>
                      <Radio value="STOCK_TW">台股（上市）</Radio>
                      <Radio value="STOCK_TWO">台股（上櫃）</Radio>
                      <Radio value="CRYPTO">加密貨幣</Radio>
                    </Radio.Group>
                  </Form.Item>

                  <Form.Item
                    label="搜尋資產"
                    name="_assetSearchInput"
                    rules={[{ validator: async () => {
                      if (!form.getFieldValue('assetSymbol')) throw new Error('請選擇一個資產');
                    }}]}
                  >
                    {selectedAssetDisplay ? (
                      <Space direction="vertical" size={4} style={{ width: '100%' }}>
                        <Space>
                          <Tag icon={<CheckCircleOutlined />} color="success" style={{ fontSize: 14, padding: '4px 10px' }}>
                            {selectedAssetDisplay}
                          </Tag>
                          <Button type="link" size="small" onClick={clearSelectedAsset}>
                            重新選擇
                          </Button>
                        </Space>
                        {assetCurrentPriceLoading && (
                          <span style={{ color: '#8c8c8c', fontSize: 13 }}>查詢當前市價中...</span>
                        )}
                        {!assetCurrentPriceLoading && assetCurrentPrice !== null && (
                          <span style={{ color: '#1677ff', fontSize: 13 }}>
                            當前市價：{formatCurrency(assetCurrentPrice)}
                          </span>
                        )}
                      </Space>
                    ) : (
                      <AutoComplete
                        options={assetSearchOptions}
                        onSearch={handleAssetSymbolSearch}
                        onSelect={handleAssetSymbolSelect}
                        placeholder={assetSearchMarket === 'CRYPTO' ? '輸入名稱或代碼，例如：BTC、bitcoin' : '輸入股票代號或名稱，例如：2330、台積電'}
                        allowClear
                        onClear={() => form.setFieldsValue({ assetSymbol: undefined, assetName: undefined })}
                      />
                    )}
                  </Form.Item>

                  <Form.Item
                    label="數量"
                    name="assetQuantity"
                    rules={[
                      { required: true, message: '請輸入數量' },
                      { type: 'number', min: 0.00000001, message: '數量必須大於 0' },
                    ]}
                  >
                    <InputNumber style={{ width: '100%' }} placeholder="持有數量" min={0} onChange={recalcTransferAmount} />
                  </Form.Item>
                  <Form.Item
                    label="單價"
                    name="assetCostPrice"
                    rules={[
                      { required: true, message: '請輸入單價' },
                      { type: 'number', min: 0.00000001, message: '單價必須大於 0' },
                    ]}
                  >
                    <InputNumber style={{ width: '100%' }} placeholder="買入的每單位價格" min={0} precision={2} onChange={recalcTransferAmount} />
                  </Form.Item>
                </>
              )}
            </>
          )}

          <Form.Item
            label="金額"
            name="amount"
            extra={assetLinkMode !== 'none' ? '由數量 × 單價自動計算' : undefined}
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
              disabled={assetLinkMode !== 'none'}
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
