import { describe, it, expect, vi, beforeEach } from 'vitest';
import { authApi } from './auth.api';
import type { AuthResponse } from '@/types/auth.types';

// Mock 整個 axios 實例
vi.mock('./axios', () => ({
  default: {
    post: vi.fn(),
  },
}));

import axiosInstance from './axios';

const mockAxios = axiosInstance as {
  post: ReturnType<typeof vi.fn>;
};

const mockAuthResponse: AuthResponse = {
  token: 'jwt-token',
  type: 'Bearer',
  userId: 'user-1',
  email: 'test@example.com',
  displayName: 'TestUser',
};

describe('authApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('login', () => {
    it('呼叫 POST /auth/login，並回傳 response.data', async () => {
      mockAxios.post.mockResolvedValue({ data: mockAuthResponse });

      const result = await authApi.login({
        email: 'test@example.com',
        password: 'password123',
      });

      expect(mockAxios.post).toHaveBeenCalledWith('/auth/login', {
        email: 'test@example.com',
        password: 'password123',
      });
      expect(result).toEqual(mockAuthResponse);
    });

    it('API 失敗時將錯誤向上拋出', async () => {
      mockAxios.post.mockRejectedValue(new Error('Network Error'));

      await expect(
        authApi.login({ email: 'x@x.com', password: 'pw' })
      ).rejects.toThrow('Network Error');
    });
  });

  describe('register', () => {
    it('呼叫 POST /auth/register，並回傳 response.data', async () => {
      mockAxios.post.mockResolvedValue({ data: mockAuthResponse });

      const result = await authApi.register({
        email: 'new@example.com',
        displayName: 'NewUser',
        password: 'password123',
      });

      expect(mockAxios.post).toHaveBeenCalledWith('/auth/register', {
        email: 'new@example.com',
        displayName: 'NewUser',
        password: 'password123',
      });
      expect(result).toEqual(mockAuthResponse);
    });

    it('API 失敗時將錯誤向上拋出', async () => {
      mockAxios.post.mockRejectedValue(new Error('Email already exists'));

      await expect(
        authApi.register({
          email: 'dup@example.com',
          displayName: 'User',
          password: 'pw',
        })
      ).rejects.toThrow('Email already exists');
    });
  });
});
