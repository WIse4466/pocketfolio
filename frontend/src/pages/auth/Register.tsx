import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Form, Input, Button, Card, Typography, message, Divider } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons';
import { authApi } from '@/api/auth.api';
import { useAuthStore } from '@/store/authStore';
import type { RegisterRequest } from '@/types/auth.types';

const { Title, Text } = Typography;

const Register = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuthStore();

  const onFinish = async (values: RegisterRequest) => {
    setLoading(true);
    try {
      const response = await authApi.register(values);
      login(response);
      message.success('註冊成功！');
      navigate('/');
    } catch (error: any) {
      console.error('註冊失敗:', error);
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
            建立帳號
          </Title>
          <Text type="secondary">開始管理你的財務</Text>
        </div>

        {/* 註冊表單 */}
        <Form
          name="register"
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
            name="displayName"
            rules={[
              { required: true, message: '請輸入使用者名稱' },
              { min: 2, message: '使用者名稱至少 2 個字元' },
            ]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="使用者名稱"
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[
              { required: true, message: '請輸入密碼' },
              { min: 6, message: '密碼至少 6 個字元' },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密碼"
            />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: '請確認密碼' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('兩次輸入的密碼不一致'));
                },
              }),
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="確認密碼"
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
              註冊
            </Button>
          </Form.Item>
        </Form>

        <Divider plain>或</Divider>

        {/* 登入連結 */}
        <div style={{ textAlign: 'center' }}>
          <Text type="secondary">
            已經有帳號？{' '}
            <Link to="/login">
              <Text style={{ color: '#1890ff' }}>立即登入</Text>
            </Link>
          </Text>
        </div>
      </Card>
    </div>
  );
};

export default Register;