import { useEffect, useState } from 'react';
import { Button, Card, Space, Typography, message } from 'antd';
import { authApi } from '@/api/auth.api';
import { useAuthStore } from './store/authStore';

const { Title, Text } = Typography;

function App() {
  const { isAuthenticated, user, login, logout } = useAuthStore();
  const [loading, setLoading] = useState(false);

  // 測試登入
  const handleTestLogin = async () => {
    setLoading(true);
    try {
      const response = await authApi.login({
        email: 'test@example.com',
        password: 'password123',
      })
      
      login(response);
      message.success('登入成功！');
    } catch (error) {
      message.error('登入失敗，請確認後端是否運行');
    } finally {
      setLoading(false);
    }
  };

  // 測試登出
  const handleLogout = () => {
    logout();
    message.success('已登出');
  };

  return (
    <div style={{ padding: '50px', maxWidth: '600px', margin: '0 auto' }}>
      <Card>
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <Title level={2}>🎨 PocketFolio 前端測試</Title>
          
          <div>
            <Text strong>認證狀態：</Text>
            <Text>{isAuthenticated ? '✅ 已登入' : '❌ 未登入'}</Text>
          </div>

          {user && (
            <div>
              <Text strong>用戶資訊：</Text>
              <div>
                <Text>Email: {user.email}</Text><br />
                <Text>Username: {user.displayName}</Text>
              </div>
            </div>
          )}

          <Space>
            {!isAuthenticated ? (
              <Button type="primary" onClick={handleTestLogin} loading={loading}>
                測試登入
              </Button>
            ) : (
              <Button onClick={handleLogout}>
                登出
              </Button>
            )}
          </Space>

          <Text type="secondary">
            提示：請先確認後端已運行，並已註冊測試帳號 test@example.com
          </Text>
        </Space>
      </Card>
    </div>
  );
}

export default App;