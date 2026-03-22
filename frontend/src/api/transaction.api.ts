import axios from './axios';
import type { Transaction, TransactionRequest } from '@/types/transaction.types';
import type { PageResponse } from '@/types/api.types';

export const transactionApi = {
  // 查詢交易列表
  getTransactions: async (params?: {
    page?: number;
    size?: number;
    categoryId?: string;
    accountId?: string;
    startDate?: string;
    endDate?: string;
  }): Promise<PageResponse<Transaction>> => {
    const response = await axios.get<PageResponse<Transaction>>('/transactions', { params });
    return response.data;
  },

  // 查詢單筆交易
  getTransaction: async (id: string): Promise<Transaction> => {
    const response = await axios.get<Transaction>(`/transactions/${id}`);
    return response.data;
  },

  // 建立交易
  createTransaction: async (data: TransactionRequest): Promise<Transaction> => {
    const response = await axios.post<Transaction>('/transactions', data);
    return response.data;
  },

  // 更新交易
  updateTransaction: async (id: string, data: TransactionRequest): Promise<Transaction> => {
    const response = await axios.put<Transaction>(`/transactions/${id}`, data);
    return response.data;
  },

  // 刪除交易
  deleteTransaction: async (id: string): Promise<void> => {
    await axios.delete(`/transactions/${id}`);
  },
};