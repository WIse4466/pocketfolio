import axios from './axios';
import type { Account, AccountRequest, AccountType } from '@/types/account.types';

export const accountApi = {
  // 查詢帳戶列表
  getAccounts: async (params?: { type?: AccountType; search?: string }): Promise<Account[]> => {
    const response = await axios.get<Account[]>('/accounts', { params });
    return response.data;
  },

  // 查詢單個帳戶
  getAccount: async (id: string): Promise<Account> => {
    const response = await axios.get<Account>(`/accounts/${id}`);
    return response.data;
  },

  // 建立帳戶
  createAccount: async (data: AccountRequest): Promise<Account> => {
    const response = await axios.post<Account>('/accounts', data);
    return response.data;
  },

  // 更新帳戶
  updateAccount: async (id: string, data: AccountRequest): Promise<Account> => {
    const response = await axios.put<Account>(`/accounts/${id}`, data);
    return response.data;
  },

  // 刪除帳戶
  deleteAccount: async (id: string): Promise<void> => {
    await axios.delete(`/accounts/${id}`);
  },
};