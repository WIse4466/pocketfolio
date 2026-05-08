import axios from './axios';

export interface ExchangeRateData {
  symbol: string;
  price: number;       // 匯率，例如 32.5 代表 1 USD = 32.5 TWD
  currency: string;
  updateTime: string;
  source: string;
}

export const exchangeRateApi = {
  getUsdToTwd: async (): Promise<ExchangeRateData> => {
    const response = await axios.get<ExchangeRateData>('/exchange-rate/usd-twd');
    return response.data;
  },
};
