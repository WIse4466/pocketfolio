import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from './authStore';
import type { AuthResponse } from '@/types/auth.types';

const mockAuthResponse: AuthResponse = {
  token: 'test-jwt-token',
  type: 'Bearer',
  userId: 'user-123',
  email: 'test@example.com',
  displayName: 'TestUser',
};

describe('useAuthStore', () => {
  beforeEach(() => {
    // 重置 store 狀態與 localStorage
    localStorage.clear();
    useAuthStore.setState({ token: null, user: null, isAuthenticated: false });
  });

  describe('初始狀態', () => {
    it('token 為 null', () => {
      expect(useAuthStore.getState().token).toBeNull();
    });

    it('user 為 null', () => {
      expect(useAuthStore.getState().user).toBeNull();
    });

    it('isAuthenticated 為 false', () => {
      expect(useAuthStore.getState().isAuthenticated).toBe(false);
    });
  });

  describe('login', () => {
    it('設定 token、user，並將 isAuthenticated 設為 true', () => {
      useAuthStore.getState().login(mockAuthResponse);

      const { token, user, isAuthenticated } = useAuthStore.getState();
      expect(token).toBe('test-jwt-token');
      expect(user?.id).toBe('user-123');
      expect(user?.email).toBe('test@example.com');
      expect(user?.displayName).toBe('TestUser');
      expect(isAuthenticated).toBe(true);
    });

    it('將 token 存入 localStorage', () => {
      useAuthStore.getState().login(mockAuthResponse);
      expect(localStorage.getItem('token')).toBe('test-jwt-token');
    });

    it('將 user 存入 localStorage', () => {
      useAuthStore.getState().login(mockAuthResponse);
      const stored = JSON.parse(localStorage.getItem('user')!);
      expect(stored.email).toBe('test@example.com');
    });
  });

  describe('logout', () => {
    it('清除 token、user，並將 isAuthenticated 設為 false', () => {
      useAuthStore.getState().login(mockAuthResponse);
      useAuthStore.getState().logout();

      const { token, user, isAuthenticated } = useAuthStore.getState();
      expect(token).toBeNull();
      expect(user).toBeNull();
      expect(isAuthenticated).toBe(false);
    });

    it('清除 localStorage 中的 token', () => {
      useAuthStore.getState().login(mockAuthResponse);
      useAuthStore.getState().logout();
      expect(localStorage.getItem('token')).toBeNull();
    });

    it('清除 localStorage 中的 user', () => {
      useAuthStore.getState().login(mockAuthResponse);
      useAuthStore.getState().logout();
      expect(localStorage.getItem('user')).toBeNull();
    });
  });

  describe('setUser', () => {
    it('只更新 user，不影響 token 和 isAuthenticated', () => {
      useAuthStore.getState().login(mockAuthResponse);
      const originalToken = useAuthStore.getState().token;

      useAuthStore.getState().setUser({
        id: 'user-123',
        email: 'new@example.com',
        displayName: 'NewName',
      });

      expect(useAuthStore.getState().user?.email).toBe('new@example.com');
      expect(useAuthStore.getState().token).toBe(originalToken);
      expect(useAuthStore.getState().isAuthenticated).toBe(true);
    });
  });
});
