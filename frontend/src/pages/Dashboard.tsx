import { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Typography, Spin, Alert } from 'antd';
import {
  DollarOutlined,
  RiseOutlined,
  FallOutlined,
  WalletOutlined,
} from '@ant-design/icons';
import { statisticsApi, type MonthlyStatistics, type AccountBalance } from '@/api/statistics.api';
import { accountApi } from '@/api/account.api';

const { Title } = Typography;

const Dashboard = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [monthlyStats, setMonthlyStats] = useState<MonthlyStatistics | null>(null);
  const [accountBalances, setAccountBalances] = useState<AccountBalance[]>([]);
  const [accountCount, setAccountCount] = useState(0);

  useEffect(() => {
    const fetchData = async () => {
      const now = new Date();
      const [statsResult, balancesResult, accountsResult] = await Promise.allSettled([
        statisticsApi.getMonthlyStatistics(now.getFullYear(), now.getMonth() + 1),
        statisticsApi.getAccountBalances(),
        accountApi.getAccounts(),
      ]);

      if (statsResult.status === 'fulfilled') setMonthlyStats(statsResult.value);
      if (balancesResult.status === 'fulfilled') setAccountBalances(balancesResult.value);
      if (accountsResult.status === 'fulfilled') setAccountCount(accountsResult.value.length);

      const allFailed = [statsResult, balancesResult, accountsResult].every(r => r.status === 'rejected');
      if (allFailed) setError('載入資料失敗，請重新整理');

      setLoading(false);
    };

    fetchData();
  }, []);

  const totalBalance = accountBalances.reduce((sum, a) => sum + a.currentBalance, 0);

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '60px 0' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div>
      <Title level={2}>控制台</Title>

      {error && (
        <Alert type="error" message={error} style={{ marginBottom: 16 }} />
      )}

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="帳戶總餘額"
              value={totalBalance}
              precision={0}
              valueStyle={{ color: '#3f8600' }}
              prefix={<DollarOutlined />}
              suffix="TWD"
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="本月收入"
              value={monthlyStats?.totalIncome ?? 0}
              precision={0}
              valueStyle={{ color: '#3f8600' }}
              prefix={<RiseOutlined />}
              suffix="TWD"
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="本月支出"
              value={monthlyStats?.totalExpense ?? 0}
              precision={0}
              valueStyle={{ color: '#cf1322' }}
              prefix={<FallOutlined />}
              suffix="TWD"
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="帳戶數量"
              value={accountCount}
              prefix={<WalletOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Card style={{ marginTop: '24px' }}>
        <Title level={4}>歡迎使用 PocketFolio！</Title>
        <p>這是一個個人財務管理系統，幫助你追蹤收支、管理資產。</p>
        <p>請從左側選單開始使用各項功能。</p>
      </Card>
    </div>
  );
};

export default Dashboard;
