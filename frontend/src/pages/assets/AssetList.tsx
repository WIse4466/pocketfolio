import { useState, useEffect, useCallback, useRef } from 'react';
import {
  Table,
  Button,
  Space,
  Modal,
  Form,
  InputNumber,
  Select,
  AutoComplete,
  message,
  Popconfirm,
  Tag,
  Typography,
  Card,
  Row,
  Col,
  Statistic,
  Tooltip,
  Progress,
  Radio,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  RiseOutlined,
  FallOutlined,
  DollarOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { assetApi } from '@/api/asset.api';
import { accountApi } from '@/api/account.api';
import { priceApi } from '@/api/price.api';
import { knownAssetApi, type KnownAssetResult, type KnownAssetType } from '@/api/knownAsset.api';
import { useWebSocketStore } from '@/store/websocketStore';
import type { Asset, AssetRequest, AssetType } from '@/types/asset.types';
import type { Account } from '@/types/account.types';
import type { ColumnsType } from 'antd/es/table';
import { formatCurrency, formatPercent } from '@/utils/format';

const { Title, Text } = Typography;

const AssetList = () => {
  const [loading, setLoading] = useState(false);
  const [assets, setAssets] = useState<Asset[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selectedAccount, setSelectedAccount] = useState<string>();
  const [modalVisible, setModalVisible] = useState(false);
  const [editingAsset, setEditingAsset] = useState<Asset | null>(null);
  const [updating, setUpdating] = useState(false);
  const [form] = Form.useForm();
  const { lastPriceUpdateAt } = useWebSocketStore();

  // AutoComplete 狀態
  // searchMarket 控制搜哪個清單（STOCK_TW / STOCK_TWO / CRYPTO），與送出的 type 欄位（STOCK / CRYPTO）分開
  const [searchMarket, setSearchMarket] = useState<KnownAssetType>('STOCK_TW');
  const [searchOptions, setSearchOptions] = useState<{ value: string; label: string; data: KnownAssetResult }[]>([]);
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleAssetSearch = useCallback((keyword: string) => {
    if (keyword.length < 1) {
      setSearchOptions([]);
      return;
    }
    if (debounceTimer.current) clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(async () => {
      try {
        const results = await knownAssetApi.search(searchMarket, keyword);
        setSearchOptions(
          results.map((r) => ({
            value: r.symbol,
            label: `${r.displayCode}　${r.name}`,
            data: r,
          }))
        );
      } catch {
        // 搜尋失敗靜默忽略
      }
    }, 300);
  }, [searchMarket]);

  const handleAssetSelect = useCallback((_value: string, option: { value: string; label: string; data: KnownAssetResult }) => {
    form.setFieldsValue({
      symbol: option.data.symbol,
      name: option.data.name,
    });
    setSearchOptions([]);
  }, [form]);

  // 載入資料
  useEffect(() => {
    loadAccounts();
  }, []);

  // 後端推播價格更新時自動 reload
  useEffect(() => {
    if (lastPriceUpdateAt && selectedAccount) {
      loadAssets();
    }
  }, [lastPriceUpdateAt]);

  useEffect(() => {
    if (selectedAccount) {
      loadAssets();
    }
  }, [selectedAccount]);

  const loadAccounts = async () => {
    try {
      const data = await accountApi.getAccounts({ type: 'INVESTMENT' });
      setAccounts(data);
      if (data.length > 0) {
        setSelectedAccount(data[0].id);
      }
    } catch (error) {
      message.error('載入帳戶失敗');
    }
  };

  const loadAssets = async () => {
    if (!selectedAccount) return;
    
    setLoading(true);
    try {
      const data = await assetApi.getAccountAssets(selectedAccount);
      setAssets(data);
    } catch (error) {
      message.error('載入資產失敗');
    } finally {
      setLoading(false);
    }
  };

  // 新增資產
  const handleCreate = () => {
    setEditingAsset(null);
    form.resetFields();
    form.setFieldsValue({ accountId: selectedAccount, type: 'STOCK', _searchMarket: 'STOCK_TW' });

    setSearchMarket('STOCK_TW');
    setSearchOptions([]);
    setModalVisible(true);
  };

  // 編輯資產
  const handleEdit = (record: Asset) => {
    setEditingAsset(record);
    const market: KnownAssetType = record.type === 'CRYPTO' ? 'CRYPTO' : 'STOCK_TW';
    form.setFieldsValue({ ...record, _searchMarket: market });
    setSearchMarket(market);
    setSearchOptions([]);
    setModalVisible(true);
  };

  // 刪除資產
  const handleDelete = async (id: string) => {
    try {
      await assetApi.deleteAsset(id);
      message.success('刪除成功');
      loadAssets();
    } catch (error) {
      message.error('刪除失敗');
    }
  };

  // 更新單個資產價格
  const handleUpdatePrice = async (id: string) => {
    try {
      await priceApi.updateAssetPrice(id);
      message.success('價格更新成功');
      loadAssets();
    } catch (error) {
      message.error('價格更新失敗');
    }
  };

  // 批次更新所有資產價格
  const handleUpdateAllPrices = async () => {
    setUpdating(true);
    try {
      const result = await priceApi.updateMyAssetPrices();
      message.success(`成功更新 ${result.successCount} 個資產價格`);
      loadAssets();
    } catch (error) {
      message.error('批次更新失敗');
    } finally {
      setUpdating(false);
    }
  };

  // 提交表單
  const handleSubmit = async (values: AssetRequest) => {
    try {
      if (editingAsset) {
        await assetApi.updateAsset(editingAsset.id, values);
        message.success('更新成功');
      } else {
        await assetApi.createAsset(values);
        message.success('新增成功');
      }

      setModalVisible(false);
      loadAssets();
    } catch (error) {
      message.error(editingAsset ? '更新失敗' : '新增失敗');
    }
  };

  // 計算統計資料
  const totalMarketValue = assets.reduce((sum, asset) => sum + asset.marketValue, 0);
  const totalCost = assets.reduce((sum, asset) => sum + asset.costPrice * asset.quantity, 0);
  const totalProfitLoss = totalMarketValue - totalCost;
  const totalProfitLossPercent = totalCost > 0 ? (totalProfitLoss / totalCost) * 100 : 0;

  // 表格欄位
  const columns: ColumnsType<Asset> = [
    {
      title: '資產名稱',
      dataIndex: 'name',
      key: 'name',
      width: 150,
      render: (name: string, record: Asset) => (
        <div>
          <div>{name}</div>
          <Text type="secondary" style={{ fontSize: '12px' }}>
            {record.symbol}
          </Text>
        </div>
      ),
    },
    {
      title: '類型',
      dataIndex: 'type',
      key: 'type',
      width: 80,
      render: (type: AssetType) => (
        <Tag color={type === 'CRYPTO' ? 'gold' : 'blue'}>
          {type === 'CRYPTO' ? '加密貨幣' : '股票'}
        </Tag>
      ),
    },
    {
      title: '持有數量',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 100,
      align: 'right',
    },
    {
      title: '成本價',
      dataIndex: 'costPrice',
      key: 'costPrice',
      width: 100,
      align: 'right',
      render: (price: number) => formatCurrency(price),
    },
    {
      title: '當前價格',
      dataIndex: 'currentPrice',
      key: 'currentPrice',
      width: 120,
      align: 'right',
      render: (price: number, record: Asset) => (
        <Tooltip title={`更新時間: ${dayjs(record.lastPriceUpdate).format('MM/DD HH:mm')}`}>
          <span>{formatCurrency(price)}</span>
        </Tooltip>
      ),
    },
    {
      title: '市值',
      dataIndex: 'marketValue',
      key: 'marketValue',
      width: 120,
      align: 'right',
      render: (value: number) => (
        <span style={{ fontWeight: 'bold' }}>{formatCurrency(value)}</span>
      ),
      sorter: (a, b) => a.marketValue - b.marketValue,
    },
    {
      title: '損益',
      key: 'profitLoss',
      width: 150,
      align: 'right',
      render: (_, record: Asset) => (
        <div>
          <div
            style={{
              color: record.profitLoss >= 0 ? '#52c41a' : '#ff4d4f',
              fontWeight: 'bold',
            }}
          >
            {record.profitLoss >= 0 ? '+' : ''}
            {formatCurrency(record.profitLoss)}
          </div>
          <div
            style={{
              fontSize: '12px',
              color: record.profitLoss >= 0 ? '#52c41a' : '#ff4d4f',
            }}
          >
            {record.profitLoss >= 0 ? '▲' : '▼'} {formatPercent(Math.abs(record.profitLossPercent))}
          </div>
        </div>
      ),
      sorter: (a, b) => a.profitLoss - b.profitLoss,
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<ReloadOutlined />}
            onClick={() => handleUpdatePrice(record.id)}
          >
            更新價格
          </Button>
          <Button
            type="link"
            size="small"
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
            <Button type="link" danger size="small" icon={<DeleteOutlined />}>
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
        <Title level={2}>資產管理</Title>
        <Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={handleUpdateAllPrices}
            loading={updating}
          >
            更新所有價格
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新增資產
          </Button>
        </Space>
      </div>

      {/* 帳戶選擇 */}
      {accounts.length > 0 ? (
        <>
          <Select
            style={{ width: 200, marginBottom: 16 }}
            value={selectedAccount}
            onChange={setSelectedAccount}
            placeholder="選擇投資帳戶"
          >
            {accounts.map((acc) => (
              <Select.Option key={acc.id} value={acc.id}>
                {acc.name}
              </Select.Option>
            ))}
          </Select>

          {/* 統計卡片 */}
          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="總市值"
                  value={totalMarketValue}
                  precision={0}
                  prefix={<DollarOutlined />}
                  suffix="TWD"
                />
              </Card>
            </Col>

            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="總成本"
                  value={totalCost}
                  precision={0}
                  prefix="$"
                  suffix="TWD"
                />
              </Card>
            </Col>

            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="總損益"
                  value={totalProfitLoss}
                  precision={0}
                  valueStyle={{ color: totalProfitLoss >= 0 ? '#3f8600' : '#cf1322' }}
                  prefix={totalProfitLoss >= 0 ? <RiseOutlined /> : <FallOutlined />}
                  suffix="TWD"
                />
              </Card>
            </Col>

            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="報酬率"
                  value={totalProfitLossPercent}
                  precision={2}
                  valueStyle={{ color: totalProfitLoss >= 0 ? '#3f8600' : '#cf1322' }}
                  suffix="%"
                />
                <Progress
                  percent={Math.abs(totalProfitLossPercent)}
                  showInfo={false}
                  strokeColor={totalProfitLoss >= 0 ? '#52c41a' : '#ff4d4f'}
                  size="small"
                />
              </Card>
            </Col>
          </Row>

          {/* 表格 */}
          <Table
            columns={columns}
            dataSource={assets}
            rowKey="id"
            loading={loading}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 筆`,
            }}
          />
        </>
      ) : (
        <Card>
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Text type="secondary">尚未建立投資帳戶</Text>
            <br />
            <Button
              type="link"
              onClick={() => window.location.href = '/accounts'}
            >
              前往建立投資帳戶
            </Button>
          </div>
        </Card>
      )}

      {/* 新增/編輯 Modal */}
      <Modal
        title={editingAsset ? '編輯資產' : '新增資產'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={null}
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="accountId" hidden>
            <AutoComplete />
          </Form.Item>
          {/* symbol / name 由 AutoComplete 選取後自動填入，用 hidden field 傳值 */}
          <Form.Item name="symbol" hidden><AutoComplete /></Form.Item>
          <Form.Item name="name" hidden><AutoComplete /></Form.Item>

          {/* type 送後端（STOCK / CRYPTO），由 _searchMarket 選擇決定 */}
          <Form.Item name="type" hidden><AutoComplete /></Form.Item>

          <Form.Item
            label="資產類型"
            name="_searchMarket"
            rules={[{ required: true, message: '請選擇資產類型' }]}
          >
            <Radio.Group onChange={(e) => {
              const market: KnownAssetType = e.target.value;
              form.setFieldsValue({
                type: market === 'CRYPTO' ? 'CRYPTO' : 'STOCK',
                symbol: undefined,
                name: undefined,
              });
              setSearchMarket(market);
              setSearchOptions([]);
            }}>
              <Radio value="STOCK_TW">台股（上市）</Radio>
              <Radio value="STOCK_TWO">台股（上櫃）</Radio>
              <Radio value="CRYPTO">加密貨幣</Radio>
            </Radio.Group>
          </Form.Item>

          <Form.Item
            label="搜尋資產"
            name="_assetSearch"
            rules={[{ validator: async () => {
              if (!form.getFieldValue('symbol')) throw new Error('請選擇一個資產');
            }}]}
          >
            <AutoComplete
              options={searchOptions}
              onSearch={handleAssetSearch}
              onSelect={handleAssetSelect}
              placeholder={searchMarket === 'CRYPTO' ? '輸入名稱或代碼，例如：BTC、bitcoin' : '輸入股票代號或名稱，例如：2330、台積電'}
              allowClear
              onClear={() => {
                form.setFieldsValue({ symbol: undefined, name: undefined });
              }}
            />
          </Form.Item>

          <Form.Item
            label="持有數量"
            name="quantity"
            rules={[
              { required: true, message: '請輸入持有數量' },
              { type: 'number', min: 0.00000001, message: '數量必須大於 0' },
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              placeholder="請輸入數量"
              precision={8}
              min={0}
            />
          </Form.Item>

          <Form.Item
            label="成本價格"
            name="costPrice"
            rules={[
              { required: true, message: '請輸入成本價格' },
              { type: 'number', min: 0.01, message: '價格必須大於 0' },
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              placeholder="請輸入購買時的價格"
              precision={2}
              min={0}
            />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                {editingAsset ? '更新' : '新增'}
              </Button>
              <Button onClick={() => setModalVisible(false)}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AssetList;