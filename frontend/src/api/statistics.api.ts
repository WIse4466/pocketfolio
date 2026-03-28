import axios from './axios';

export interface CategoryStat {
  categoryName: string;
  amount: number;
  percentage: number;
}

export interface MonthlyStatistics {
  year: number;
  month: number;
  totalIncome: number;
  totalExpense: number;
  netAmount: number;
  incomeByCategory: CategoryStat[];
  expenseByCategory: CategoryStat[];
}

export interface AccountBalance {
  accountId: string;
  accountName: string;
  accountType: string;
  currentBalance: number;
  initialBalance: number;
  change: number;
  changePercent: number;
}

export const statisticsApi = {
  // 查詢月度統計
  getMonthlyStatistics: async (year: number, month: number): Promise<MonthlyStatistics> => {
    const response = await axios.get<MonthlyStatistics>(`/statistics/monthly/${year}/${month}`);
    return response.data;
  },

  // 查詢帳戶餘額統計
  getAccountBalances: async (): Promise<AccountBalance[]> => {
    const response = await axios.get<AccountBalance[]>('/statistics/account-balances');
    return response.data;
  },
};