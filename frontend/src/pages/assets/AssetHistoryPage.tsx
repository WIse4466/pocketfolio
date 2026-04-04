import { useState, useEffect } from 'react';
import { Typography, Card, Row, Col, Statistic, Radio, Empty, Spin } from 'antd';
import { RiseOutlined, FallOutlined } from '@ant-design/icons';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceLine,
} from 'recharts';
import { snapshotApi, type PortfolioHistoryPoint } from '@/api/snapshot.api';
import { formatCurrency, formatPercent } from '@/utils/format';

const { Title, Text } = Typography;

const AssetHistoryPage = () => {
  const [loading, setLoading] = useState(false);
  const [days, setDays] = useState(30);
  const [history, setHistory] = useState<PortfolioHistoryPoint[]>([]);

  useEffect(() => {
    loadHistory();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [days]);

  const loadHistory = async () => {
    setLoading(true);
    try {
      const data = await snapshotApi.getPortfolioHistory(days);
      setHistory(data);
    } catch {
      // axios 攔截器已處理錯誤訊息
    } finally {
      setLoading(false);
    }
  };

  // 取最新一筆作為當前狀態
  const latest = history[history.length - 1];
  const isProfit = latest ? latest.totalProfitLoss >= 0 : true;
  const profitColor = isProfit ? '#52c41a' : '#ff4d4f';

  // 圖表 tooltip 格式
  const customTooltip = ({ active, payload, label }: {
    active?: boolean;
    payload?: readonly { payload?: PortfolioHistoryPoint }[];
    label?: string | number;
  }) => {
    if (!active || !payload?.length) return null;
    const d = payload[0].payload as PortfolioHistoryPoint;
    return (
      <Card size="small" style={{ minWidth: 180 }}>
        <div style={{ marginBottom: 4 }}><Text strong>{label}</Text></div>
        <div>市值：{formatCurrency(d.totalMarketValue)}</div>
        <div>成本：{formatCurrency(d.totalCost)}</div>
        <div style={{ color: d.totalProfitLoss >= 0 ? '#52c41a' : '#ff4d4f' }}>
          損益：{d.totalProfitLoss >= 0 ? '+' : ''}{formatCurrency(d.totalProfitLoss)}
          （{d.totalProfitLoss >= 0 ? '+' : ''}{formatPercent(d.totalProfitLossPercent)}）
        </div>
      </Card>
    );
  };

  return (
    <div>
      {/* 標題 */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <Title level={2} style={{ margin: 0 }}>資產歷史走勢</Title>
        <Radio.Group
          value={days}
          onChange={(e) => setDays(e.target.value)}
          optionType="button"
          buttonStyle="solid"
        >
          <Radio.Button value={7}>7 天</Radio.Button>
          <Radio.Button value={30}>30 天</Radio.Button>
          <Radio.Button value={90}>90 天</Radio.Button>
        </Radio.Group>
      </div>

      {/* 統計卡片 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="當前總市值"
              value={latest?.totalMarketValue ?? 0}
              precision={0}
              prefix="$"
              suffix="TWD"
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="總成本"
              value={latest?.totalCost ?? 0}
              precision={0}
              prefix="$"
              suffix="TWD"
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="總損益"
              value={latest?.totalProfitLoss ?? 0}
              precision={0}
              valueStyle={{ color: profitColor }}
              prefix={isProfit ? <RiseOutlined /> : <FallOutlined />}
              suffix="TWD"
            />
            {latest && (
              <Text style={{ color: profitColor }}>
                {isProfit ? '+' : ''}{formatPercent(latest.totalProfitLossPercent)}
              </Text>
            )}
          </Card>
        </Col>
      </Row>

      {/* 走勢圖 */}
      <Card title="投資組合總市值走勢">
        <Spin spinning={loading}>
          {history.length === 0 && !loading ? (
            <Empty description="尚無歷史快照資料，系統每天凌晨 1 點自動建立快照" />
          ) : (
            <ResponsiveContainer width="100%" height={360}>
              <LineChart data={history} margin={{ top: 8, right: 24, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                <YAxis
                  tickFormatter={(v) => `$${(v / 1000).toFixed(0)}K`}
                  tick={{ fontSize: 12 }}
                  width={60}
                />
                <Tooltip content={customTooltip} />
                <ReferenceLine
                  y={latest?.totalCost}
                  stroke="#aaa"
                  strokeDasharray="4 4"
                  label={{ value: '成本線', position: 'insideTopRight', fontSize: 11 }}
                />
                <Line
                  type="monotone"
                  dataKey="totalMarketValue"
                  stroke="#1677ff"
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ r: 5 }}
                  name="總市值"
                />
              </LineChart>
            </ResponsiveContainer>
          )}
        </Spin>
      </Card>
    </div>
  );
};

export default AssetHistoryPage;
