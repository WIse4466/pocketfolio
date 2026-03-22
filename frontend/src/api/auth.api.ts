import axios from './axios.ts';
import type { LoginRequest, RegisterRequest, AuthResponse } from '@/types/auth.types';

export const authApi = {
  // 登入
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await axios.post<AuthResponse>('/auth/login', data);
    return response.data;
  },

  // 註冊
  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await axios.post<AuthResponse>('/auth/register', data);
    return response.data;
  },
};