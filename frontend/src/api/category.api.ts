import axios from './axios';
import type { Category, CategoryRequest, CategoryType } from '@/types/category.types';

export const categoryApi = {
  // 查詢類別列表
  getCategories: async (type?: CategoryType): Promise<Category[]> => {
    const response = await axios.get<Category[]>('/categories', {
      params: { type },
    });
    return response.data;
  },

  // 查詢單個類別
  getCategory: async (id: string): Promise<Category> => {
    const response = await axios.get<Category>(`/categories/${id}`);
    return response.data;
  },

  // 建立類別
  createCategory: async (data: CategoryRequest): Promise<Category> => {
    const response = await axios.post<Category>('/categories', data);
    return response.data;
  },

  // 更新類別
  updateCategory: async (id: string, data: CategoryRequest): Promise<Category> => {
    const response = await axios.put<Category>(`/categories/${id}`, data);
    return response.data;
  },

  // 刪除類別
  deleteCategory: async (id: string): Promise<void> => {
    await axios.delete(`/categories/${id}`);
  },
};