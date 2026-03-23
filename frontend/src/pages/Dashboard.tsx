import { Card, Row, Col, Statistic, Typography } from 'antd';
import {
  DollarOutlined,
  RiseOutlined,
  FallOutlined,
  WalletOutlined,
} from '@ant-design/icons';

const { Title } = Typography;

const Dashboard = () => {
  return (
    <div>
      <Title level={2}>控制台</Title>

      {/* 統計卡片 */}
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="總資產"
              value={150000}
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
              value={50000}
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
              value={30000}
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
              value={5}
              prefix={<WalletOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* 提示訊息 */}
      <Card style={{ marginTop: '24px' }}>
        <Title level={4}>歡迎使用 PocketFolio！</Title>
        <p>這是一個個人財務管理系統，幫助你追蹤收支、管理資產。</p>
        <p>請從左側選單開始使用各項功能。</p>
        <p style={{ color: '#999', fontSize: '12px' }}>
          註：統計數據將在 Phase 6 整合真實 API
        </p>
      </Card>
    </div>
  );
};

export default Dashboard;