import { describe, it, expect, vi, beforeEach } from 'vitest';
import { accountApi } from './account.api';
import type { Account, AccountRequest } from '@/types/account.types';

vi.mock('./axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

import axiosInstance from './axios';

const mockAxios = axiosInstance as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  put: ReturnType<typeof vi.fn>;
  delete: ReturnType<typeof vi.fn>;
};

const mockAccount: Account = {
  id: 'acc-1',
  name: '現金',
  type: 'CASH',
  initialBalance: 1000,
  currentBalance: 1500,
  currency: 'TWD',
};

const accountRequest: AccountRequest = {
  name: '現金',
  type: 'CASH',
  initialBalance: 1000,
  currency: 'TWD',
};

describe('accountApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getAccounts', () => {
    it('呼叫 GET /accounts，並回傳帳戶列表', async () => {
      mockAxios.get.mockResolvedValue({ data: [mockAccount] });

      const result = await accountApi.getAccounts();

      expect(mockAxios.get).toHaveBeenCalledWith('/accounts', { params: undefined });
      expect(result).toEqual([mockAccount]);
    });

    it('帶入查詢參數', async () => {
      mockAxios.get.mockResolvedValue({ data: [] });

      await accountApi.getAccounts({ type: 'CASH', search: '現' });

      expect(mockAxios.get).toHaveBeenCalledWith('/accounts', {
        params: { type: 'CASH', search: '現' },
      });
    });
  });

  describe('getAccount', () => {
    it('呼叫 GET /accounts/:id，並回傳單筆帳戶', async () => {
      mockAxios.get.mockResolvedValue({ data: mockAccount });

      const result = await accountApi.getAccount('acc-1');

      expect(mockAxios.get).toHaveBeenCalledWith('/accounts/acc-1');
      expect(result).toEqual(mockAccount);
    });
  });

  describe('createAccount', () => {
    it('呼叫 POST /accounts，並回傳建立的帳戶', async () => {
      mockAxios.post.mockResolvedValue({ data: mockAccount });

      const result = await accountApi.createAccount(accountRequest);

      expect(mockAxios.post).toHaveBeenCalledWith('/accounts', accountRequest);
      expect(result).toEqual(mockAccount);
    });
  });

  describe('updateAccount', () => {
    it('呼叫 PUT /accounts/:id，並回傳更新後的帳戶', async () => {
      const updated: Account = { ...mockAccount, name: '零用錢' };
      mockAxios.put.mockResolvedValue({ data: updated });

      const result = await accountApi.updateAccount('acc-1', {
        ...accountRequest,
        name: '零用錢',
      });

      expect(mockAxios.put).toHaveBeenCalledWith('/accounts/acc-1', {
        ...accountRequest,
        name: '零用錢',
      });
      expect(result.name).toBe('零用錢');
    });
  });

  describe('deleteAccount', () => {
    it('呼叫 DELETE /accounts/:id', async () => {
      mockAxios.delete.mockResolvedValue({});

      await accountApi.deleteAccount('acc-1');

      expect(mockAxios.delete).toHaveBeenCalledWith('/accounts/acc-1');
    });

    it('API 失敗時將錯誤向上拋出', async () => {
      mockAxios.delete.mockRejectedValue(new Error('Not Found'));

      await expect(accountApi.deleteAccount('unknown-id')).rejects.toThrow('Not Found');
    });
  });
});
