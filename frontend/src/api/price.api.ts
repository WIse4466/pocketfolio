import axios from './axios';
import type { AssetType } from '@/types/asset.types';

export interface PriceData {
  symbol: string;
  price: number;
  updateTime: string;
  source: string;
}

export interface PriceUpdateResponse {
  symbol: string;
  oldPrice: number;
  newPrice: number;
  changePercent: number;
  updateTime: string;
  success: boolean;
  errorMessage?: string;
}

export const priceApi = {
  // 查詢即時價格
  getPrice: async (symbol: string, type: AssetType): Promise<PriceData> => {
    const response = await axios.get<PriceData>(`/prices/${symbol}`, {
      params: { type },
    });
    return response.data;
  },

  // 更新資產價格
  updateAssetPrice: async (assetId: string): Promise<PriceUpdateResponse> => {
    const response = await axios.post<PriceUpdateResponse>(`/prices/update/asset/${assetId}`);
    return response.data;
  },

  // 批次更新我的資產價格
  updateMyAssetPrices: async (): Promise<PriceUpdateResponse[]> => {
    const response = await axios.post<PriceUpdateResponse[]>('/prices/update/my-assets');
    return response.data;
  },

  // 清除快取
  clearCache: async (): Promise<void> => {
    await axios.delete('/prices/cache');
  },
};