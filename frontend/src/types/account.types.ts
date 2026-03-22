// 帳戶類型
export type AccountType = 'CASH' | 'BANK' | 'CREDIT_CARD' | 'INVESTMENT';

// 帳戶
export interface Account {
  id: string;
  name: string;
  type: AccountType;
  initialBalance: number;
  currentBalance: number;
  description?: string;
  currency: string;
}

// 帳戶請求
export interface AccountRequest {
  name: string;
  type: AccountType;
  initialBalance: number;
  description?: string;
  currency?: string;
}