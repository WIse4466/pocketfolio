import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Avatar, Dropdown, Space, Typography } from 'antd';
import {
  DashboardOutlined,
  TransactionOutlined,
  TagsOutlined,
  BankOutlined,
  LineChartOutlined,
  HistoryOutlined,
  UserOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '@/store/authStore';
import type { MenuProps } from 'antd';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

const MainLayout = () => {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuthStore();

  // 側邊欄選單項目
  const menuItems: MenuProps['items'] = [
    {
      key: '/',
      icon: <DashboardOutlined />,
      label: '控制台',
      onClick: () => navigate('/'),
    },
    {
      key: '/transactions',
      icon: <TransactionOutlined />,
      label: '交易記錄',
      onClick: () => navigate('/transactions'),
    },
    {
      key: '/categories',
      icon: <TagsOutlined />,
      label: '類別管理',
      onClick: () => navigate('/categories'),
    },
    {
      key: '/accounts',
      icon: <BankOutlined />,
      label: '帳戶管理',
      onClick: () => navigate('/accounts'),
    },
    {
      key: '/assets',
      icon: <LineChartOutlined />,
      label: '資產管理',
      onClick: () => navigate('/assets'),
    },
    {
      key: '/history',
      icon: <HistoryOutlined />,
      label: '資產走勢',
      onClick: () => navigate('/history'),
    },
    {
      key: '/statistics',
      icon: <LineChartOutlined />,
      label: '統計分析',
      onClick: () => navigate('/statistics'),
      disabled: true, // Phase 6 實作
    },
  ];

  // 用戶下拉選單
  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '個人資料',
      onClick: () => navigate('/profile'),
      disabled: true, // 未來實作
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '登出',
      onClick: () => {
        logout();
        navigate('/login');
      },
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {/* 側邊欄 */}
      <Sider 
        trigger={null} 
        collapsible 
        collapsed={collapsed}
        style={{
          overflow: 'auto',
          height: '100vh',
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
        }}
      >
        {/* Logo */}
        <div
          style={{
            height: '64px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontSize: collapsed ? '20px' : '24px',
            fontWeight: 'bold',
            transition: 'all 0.2s',
          }}
        >
          {collapsed ? '💰' : '💰 PocketFolio'}
        </div>

        {/* 選單 */}
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
        />
      </Sider>

      {/* 主要內容區 */}
      <Layout style={{ marginLeft: collapsed ? 80 : 200, transition: 'all 0.2s' }}>
        {/* Header */}
        <Header
          style={{
            padding: '0 24px',
            background: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 1px 4px rgba(0,21,41,.08)',
          }}
        >
          {/* 折疊按鈕 */}
          <div
            style={{ fontSize: '18px', cursor: 'pointer' }}
            onClick={() => setCollapsed(!collapsed)}
          >
            {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          </div>

          {/* 用戶資訊 */}
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <Space style={{ cursor: 'pointer' }}>
              <Avatar icon={<UserOutlined />} />
              <Text strong>{user?.displayName || '用戶'}</Text>
            </Space>
          </Dropdown>
        </Header>

        {/* 內容區 */}
        <Content
          style={{
            margin: '24px 16px',
            padding: 24,
            minHeight: 280,
            background: '#fff',
            borderRadius: '8px',
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;