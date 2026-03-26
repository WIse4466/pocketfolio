import axios from './axios';

export interface PortfolioHistoryPoint {
  date: string;
  totalMarketValue: number;
  totalCost: number;
  totalProfitLoss: number;
  totalProfitLossPercent: number;
  assetCount: number;
}

export const snapshotApi = {
  // 查詢投資組合歷史趨勢
  getPortfolioHistory: async (days: number = 30): Promise<PortfolioHistoryPoint[]> => {
    const response = await axios.get<PortfolioHistoryPoint[]>('/snapshots/portfolio/history', {
      params: { days },
    });
    return response.data;
  },
};
