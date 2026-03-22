// 類別類型
export type CategoryType = 'INCOME' | 'EXPENSE';

// 類別
export interface Category {
  id: string;
  name: string;
  type: CategoryType;
  description?: string;
}

// 類別請求
export interface CategoryRequest {
  name: string;
  type: CategoryType;
  description?: string;
}