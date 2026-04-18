import { describe, it, expect, beforeEach } from 'vitest';
import { tokenUtils } from './token';

describe('tokenUtils', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  describe('getToken', () => {
    it('localStorage 無 token 時回傳 null', () => {
      expect(tokenUtils.getToken()).toBeNull();
    });

    it('localStorage 有 token 時回傳該值', () => {
      localStorage.setItem('token', 'abc123');
      expect(tokenUtils.getToken()).toBe('abc123');
    });
  });

  describe('setToken', () => {
    it('將 token 存入 localStorage', () => {
      tokenUtils.setToken('my-jwt-token');
      expect(localStorage.getItem('token')).toBe('my-jwt-token');
    });

    it('覆蓋已存在的 token', () => {
      localStorage.setItem('token', 'old-token');
      tokenUtils.setToken('new-token');
      expect(localStorage.getItem('token')).toBe('new-token');
    });
  });

  describe('removeToken', () => {
    it('從 localStorage 移除 token', () => {
      localStorage.setItem('token', 'some-token');
      tokenUtils.removeToken();
      expect(localStorage.getItem('token')).toBeNull();
    });

    it('token 不存在時呼叫不報錯', () => {
      expect(() => tokenUtils.removeToken()).not.toThrow();
    });
  });

  describe('hasToken', () => {
    it('localStorage 無 token 時回傳 false', () => {
      expect(tokenUtils.hasToken()).toBe(false);
    });

    it('localStorage 有 token 時回傳 true', () => {
      localStorage.setItem('token', 'jwt');
      expect(tokenUtils.hasToken()).toBe(true);
    });

    it('token 被移除後回傳 false', () => {
      localStorage.setItem('token', 'jwt');
      localStorage.removeItem('token');
      expect(tokenUtils.hasToken()).toBe(false);
    });
  });
});
