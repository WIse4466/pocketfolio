// 用戶資訊
export interface User {
  id: string;
  email: string;
  displayName: string;
}

// 登入請求
export interface LoginRequest {
  email: string;
  password: string;
}

// 註冊請求
export interface RegisterRequest {
  email: string;
  displayName: string;
  password: string;
}

// 認證回應
export interface AuthResponse {
  token: string;
  type: string;
  userId: string;
  email: string;
  displayName: string;
}