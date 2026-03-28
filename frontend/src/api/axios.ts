import axios, { type AxiosError, type AxiosResponse } from 'axios';
import { message } from 'antd';
import { useAuthStore } from '@/store/authStore';

// 創建 Axios 實例
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 請求攔截器
axiosInstance.interceptors.request.use(
  (config) => {
    // 從 localStorage 取得 Token
    const token = localStorage.getItem('token');
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 回應攔截器
axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => {
    return response;
  },
  (error: AxiosError) => {
    // 處理錯誤
    if (error.response) {
      const status = error.response.status;
      const data: any = error.response.data;

      switch (status) {
        case 401:
          // 登入/註冊本身失敗不處理，讓各自頁面顯示錯誤
          if (!error.config?.url?.startsWith('/auth/')) {
            useAuthStore.getState().logout();
            message.error('登入已過期，請重新登入');
            window.location.href = '/login';
          }
          break;
        
        case 403:
          message.error('沒有權限存取此資源');
          break;
        
        case 404:
          message.error(data.message || '找不到資源');
          break;
        
        case 400:
          message.error(data.message || '請求參數錯誤');
          break;
        
        case 500:
          message.error('伺服器錯誤，請稍後再試');
          break;
        
        default:
          message.error(data.message || '發生錯誤，請稍後再試');
      }
    } else if (error.request) {
      // 請求已發送但沒有收到回應
      message.error('無法連接到伺服器，請檢查網路連線');
    } else {
      // 其他錯誤
      message.error('發生未知錯誤');
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;