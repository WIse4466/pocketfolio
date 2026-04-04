import { createBrowserRouter, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import MainLayout from '@/components/Layout/MainLayout';
import Login from '@/pages/auth/Login';
import Register from '@/pages/auth/Register';
import Dashboard from '@/pages/Dashboard';
import TransactionList from '@/pages/transactions/TransactionList';
import CategoryList from '@/pages/categories/CategoryList';
import AccountList from '@/pages/accounts/AccountList';
import NotFound from '@/pages/NotFound';
import AssetList from '@/pages/assets/AssetList';
import AssetHistoryPage from '@/pages/assets/AssetHistoryPage';
import StatisticsPage from '@/pages/statistics/StatisticsPage';

// 受保護路由組件（與 router export 同檔，react-refresh 限制不適用此模式）
// eslint-disable-next-line react-refresh/only-export-components
const PrivateRoute = ({ children }: { children: React.ReactNode }) => {
  const { isAuthenticated } = useAuthStore();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
};

// 公開路由組件（已登入則跳轉到首頁）
// eslint-disable-next-line react-refresh/only-export-components
const PublicRoute = ({ children }: { children: React.ReactNode }) => {
  const { isAuthenticated } = useAuthStore();
  return !isAuthenticated ? <>{children}</> : <Navigate to="/" replace />;
};

export const router = createBrowserRouter([
  // 公開路由
  {
    path: '/login',
    element: (
      <PublicRoute>
        <Login />
      </PublicRoute>
    ),
  },
  {
    path: '/register',
    element: (
      <PublicRoute>
        <Register />
      </PublicRoute>
    ),
  },

  // 受保護路由
  {
    path: '/',
    element: (
      <PrivateRoute>
        <MainLayout />
      </PrivateRoute>
    ),
    children: [
      {
        index: true,
        element: <Dashboard />,
      },
      {
        path: 'transactions',
        element: <TransactionList />,
      },
      {
        path: 'categories',
        element: <CategoryList />,
      },
      {
        path: 'accounts',
        element: <AccountList />,
      },
      {
        path: 'assets',
        element: <AssetList />,
      },
      {
        path: 'history',
        element: <AssetHistoryPage />,
      },
      {
        path: 'statistics',
        element: <StatisticsPage />,
      },
    ],
  },

  // 404
  {
    path: '*',
    element: <NotFound />,
  },
]);