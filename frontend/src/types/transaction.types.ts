export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER_OUT' | 'TRANSFER_IN';

// 交易記錄
export interface Transaction {
  id: string;
  type: TransactionType;
  amount: number;
  note: string;
  date: string;
  transferGroupId?: string;
  categoryId?: string;
  categoryName?: string;
  categoryType?: 'INCOME' | 'EXPENSE';
  accountId?: string;
  accountName?: string;
  accountType?: 'CASH' | 'BANK' | 'CREDIT_CARD' | 'INVESTMENT';
  toAccountId?: string;
  toAccountName?: string;
}

// 交易請求
export interface TransactionRequest {
  type: TransactionType;
  amount: number;
  note?: string;
  date: string;
  categoryId?: string;
  accountId?: string;
  toAccountId?: string;
  // 資產連結（TRANSFER_OUT 轉入 INVESTMENT 帳戶時選填）
  assetId?: string;
  assetType?: 'STOCK' | 'CRYPTO' | 'FUND' | 'BOND';
  assetSymbol?: string;
  assetName?: string;
  assetQuantity?: number;
  assetCostPrice?: number;
  assetNote?: string;
}