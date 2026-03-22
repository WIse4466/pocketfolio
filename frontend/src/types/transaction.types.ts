// 交易記錄
export interface Transaction {
  id: string;
  amount: number;
  note: string;
  date: string;
  categoryId?: string;
  categoryName?: string;
  categoryType?: 'INCOME' | 'EXPENSE';
  accountId?: string;
  accountName?: string;
  accountType?: 'CASH' | 'BANK' | 'CREDIT_CARD' | 'INVESTMENT';
}

// 交易請求
export interface TransactionRequest {
  amount: number;
  note: string;
  date: string;
  categoryId?: string;
  accountId?: string;
}