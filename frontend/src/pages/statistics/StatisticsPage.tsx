import { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Typography, Spin, Alert, DatePicker, Table, Tag } from 'antd';
import { RiseOutlined, FallOutlined, SwapOutlined } from '@ant-design/icons';
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import dayjs, { type Dayjs } from 'dayjs';
import { statisticsApi, type MonthlyStatistics } from '@/api/statistics.api';

const { Title } = Typography;

const COLORS = ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16'];

const StatisticsPage = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [stats, setStats] = useState<MonthlyStatistics | null>(null);
  const [selectedDate, setSelectedDate] = useState<Dayjs>(dayjs());

  const fetchStats = async (date: Dayjs) => {
    setLoading(true);
    setError(null);
    try {
      const data = await statisticsApi.getMonthlyStatistics(date.year(), date.month() + 1);
      setStats(data);
    } catch {
      setError('載入統計資料失敗');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStats(selectedDate);
  }, [selectedDate]);

  const expenseBreakdown = stats?.categoryBreakdown?.filter(c => c.categoryType === 'EXPENSE') ?? [];
  const incomeBreakdown = stats?.categoryBreakdown?.filter(c => c.categoryType === 'INCOME') ?? [];

  const columns = [
    { title: '類別', dataIndex: 'categoryName', key: 'categoryName' },
    {
      title: '金額',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      render: (v: number) => `$${v.toLocaleString()}`,
      sorter: (a: any, b: any) => b.totalAmount - a.totalAmount,
    },
    {
      title: '佔比',
      dataIndex: 'percentage',
      key: 'percentage',
      render: (v: number) => `${v.toFixed(1)}%`,
    },
    { title: '筆數', dataIndex: 'transactionCount', key: 'transactionCount' },
  ];

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 24 }}>
        <Title level={2} style={{ margin: 0 }}>統計分析</Title>
        <DatePicker
          picker="month"
          value={selectedDate}
          onChange={(date) => date && setSelectedDate(date)}
          allowClear={false}
        />
      </div>

      {error && <Alert type="error" message={error} style={{ marginBottom: 16 }} />}

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0' }}>
          <Spin size="large" />
        </div>
      ) : (
        <>
          {/* 月度摘要 */}
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={8}>
              <Card>
                <Statistic
                  title="本月收入"
                  value={stats?.totalIncome ?? 0}
                  precision={0}
                  valueStyle={{ color: '#3f8600' }}
                  prefix={<RiseOutlined />}
                  suffix="TWD"
                />
              </Card>
            </Col>
            <Col xs={24} sm={8}>
              <Card>
                <Statistic
                  title="本月支出"
                  value={stats?.totalExpense ?? 0}
                  precision={0}
                  valueStyle={{ color: '#cf1322' }}
                  prefix={<FallOutlined />}
                  suffix="TWD"
                />
              </Card>
            </Col>
            <Col xs={24} sm={8}>
              <Card>
                <Statistic
                  title="本月結餘"
                  value={stats?.netAmount ?? 0}
                  precision={0}
                  valueStyle={{ color: (stats?.netAmount ?? 0) >= 0 ? '#3f8600' : '#cf1322' }}
                  prefix={<SwapOutlined />}
                  suffix="TWD"
                />
              </Card>
            </Col>
          </Row>

          {/* 支出分析 */}
          {expenseBreakdown.length > 0 && (
            <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
              <Col xs={24} lg={10}>
                <Card title={<><Tag color="red">支出</Tag>類別圓餅圖</>}>
                  <ResponsiveContainer width="100%" height={260}>
                    <PieChart>
                      <Pie
                        data={expenseBreakdown}
                        dataKey="totalAmount"
                        nameKey="categoryName"
                        cx="50%"
                        cy="50%"
                        outerRadius={90}
                        label={({ categoryName, percentage }: any) => `${categoryName} ${Number(percentage).toFixed(0)}%`}
                        labelLine={false}
                      >
                        {expenseBreakdown.map((_, i) => (
                          <Cell key={i} fill={COLORS[i % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip formatter={(v) => [`$${Number(v).toLocaleString()}`]} />
                    </PieChart>
                  </ResponsiveContainer>
                </Card>
              </Col>
              <Col xs={24} lg={14}>
                <Card title={<><Tag color="red">支出</Tag>明細</>}>
                  <Table
                    dataSource={expenseBreakdown}
                    columns={columns}
                    rowKey="categoryId"
                    pagination={false}
                    size="small"
                  />
                </Card>
              </Col>
            </Row>
          )}

          {/* 收入分析 */}
          {incomeBreakdown.length > 0 && (
            <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
              <Col xs={24} lg={10}>
                <Card title={<><Tag color="green">收入</Tag>類別圓餅圖</>}>
                  <ResponsiveContainer width="100%" height={260}>
                    <PieChart>
                      <Pie
                        data={incomeBreakdown}
                        dataKey="totalAmount"
                        nameKey="categoryName"
                        cx="50%"
                        cy="50%"
                        outerRadius={90}
                        label={({ categoryName, percentage }: any) => `${categoryName} ${Number(percentage).toFixed(0)}%`}
                        labelLine={false}
                      >
                        {incomeBreakdown.map((_, i) => (
                          <Cell key={i} fill={COLORS[i % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip formatter={(v) => [`$${Number(v).toLocaleString()}`]} />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </Card>
              </Col>
              <Col xs={24} lg={14}>
                <Card title={<><Tag color="green">收入</Tag>明細</>}>
                  <Table
                    dataSource={incomeBreakdown}
                    columns={columns}
                    rowKey="categoryId"
                    pagination={false}
                    size="small"
                  />
                </Card>
              </Col>
            </Row>
          )}

          {expenseBreakdown.length === 0 && incomeBreakdown.length === 0 && (
            <Card style={{ marginTop: 16, textAlign: 'center', color: '#999' }}>
              本月尚無交易記錄
            </Card>
          )}
        </>
      )}
    </div>
  );
};

export default StatisticsPage;
