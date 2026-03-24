import axios from './axios';
import type { AssetType } from '@/types/asset.types';

export type AlertCondition = 'ABOVE' | 'BELOW';

export interface PriceAlert {
  id: string;
  assetId?: string;
  symbol: string;
  assetType: AssetType;
  condition: AlertCondition;
  targetPrice: number;
  active: boolean;
  triggered: boolean;
  triggeredAt?: string;
  createdAt: string;
  note?: string;
  currentPrice?: number;
  conditionText: string;
}

export interface PriceAlertRequest {
  assetId?: string;
  symbol: string;
  assetType: AssetType;
  condition: AlertCondition;
  targetPrice: number;
  note?: string;
}

export const priceAlertApi = {
  // 查詢我的所有警報
  getMyAlerts: async (activeOnly = false): Promise<PriceAlert[]> => {
    const response = await axios.get<PriceAlert[]>('/price-alerts', {
      params: { activeOnly },
    });
    return response.data;
  },

  // 查詢單個警報
  getAlert: async (id: string): Promise<PriceAlert> => {
    const response = await axios.get<PriceAlert>(`/price-alerts/${id}`);
    return response.data;
  },

  // 建立警報
  createAlert: async (data: PriceAlertRequest): Promise<PriceAlert> => {
    const response = await axios.post<PriceAlert>('/price-alerts', data);
    return response.data;
  },

  // 更新警報
  updateAlert: async (id: string, data: PriceAlertRequest): Promise<PriceAlert> => {
    const response = await axios.put<PriceAlert>(`/price-alerts/${id}`, data);
    return response.data;
  },

  // 切換啟用狀態
  toggleAlert: async (id: string, active: boolean): Promise<PriceAlert> => {
    const response = await axios.patch<PriceAlert>(`/price-alerts/${id}/toggle`, null, {
      params: { active },
    });
    return response.data;
  },

  // 刪除警報
  deleteAlert: async (id: string): Promise<void> => {
    await axios.delete(`/price-alerts/${id}`);
  },
};