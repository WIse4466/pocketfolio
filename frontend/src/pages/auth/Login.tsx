import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Form, Input, Button, Card, Typography, message, Divider } from 'antd';
import { LockOutlined, MailOutlined } from '@ant-design/icons';
import axios from 'axios';
import { authApi } from '@/api/auth.api';
import { useAuthStore } from '@/store/authStore';
import type { LoginRequest } from '@/types/auth.types';

const { Title, Text } = Typography;

const Login = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuthStore();

  const onFinish = async (values: LoginRequest) => {
    setLoading(true);
    try {
      const response = await authApi.login(values);
      login(response);
      message.success('登入成功！');
      navigate('/');
    } catch (error: unknown) {
      let msg = '帳號或密碼錯誤';
      if (axios.isAxiosError(error)) {
        msg = error.response?.data?.message || msg;
      }
      message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        padding: '20px',
      }}
    >
      <Card
        style={{
          width: '100%',
          maxWidth: '400px',
          boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
        }}
      >
        {/* Logo 與標題 */}
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>💰</div>
          <Title level={2} style={{ margin: 0 }}>
            PocketFolio
          </Title>
          <Text type="secondary">個人財務管理系統</Text>
        </div>

        {/* 登入表單 */}
        <Form
          name="login"
          onFinish={onFinish}
          autoComplete="off"
          size="large"
        >
          <Form.Item
            name="email"
            rules={[
              { required: true, message: '請輸入 Email' },
              { type: 'email', message: '請輸入有效的 Email' },
            ]}
          >
            <Input
              prefix={<MailOutlined />}
              placeholder="Email"
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: '請輸入密碼' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密碼"
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              size="large"
            >
              登入
            </Button>
          </Form.Item>
        </Form>

        <Divider plain>或</Divider>

        {/* 註冊連結 */}
        <div style={{ textAlign: 'center' }}>
          <Text type="secondary">
            還沒有帳號？{' '}
            <Link to="/register">
              <Text style={{ color: '#1890ff' }}>立即註冊</Text>
            </Link>
          </Text>
        </div>

        {/* 測試帳號提示 */}
        <div
          style={{
            marginTop: '24px',
            padding: '12px',
            background: '#f0f2f5',
            borderRadius: '4px',
            textAlign: 'center',
          }}
        >
          <Text type="secondary" style={{ fontSize: '12px' }}>
            測試帳號：test@example.com / password123
          </Text>
        </div>
      </Card>
    </div>
  );
};

export default Login;