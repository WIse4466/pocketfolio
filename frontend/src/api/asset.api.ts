import axios from './axios';
import type { Asset, AssetRequest, AssetType } from '@/types/asset.types';

export const assetApi = {
  // 查詢帳戶的資產列表
  getAccountAssets: async (accountId: string, type?: AssetType): Promise<Asset[]> => {
    const response = await axios.get<Asset[]>(`/assets/account/${accountId}`, {
      params: { type },
    });
    return response.data;
  },

  // 查詢單個資產
  getAsset: async (id: string): Promise<Asset> => {
    const response = await axios.get<Asset>(`/assets/${id}`);
    return response.data;
  },

  // 建立資產
  createAsset: async (data: AssetRequest): Promise<Asset> => {
    const response = await axios.post<Asset>('/assets', data);
    return response.data;
  },

  // 更新資產
  updateAsset: async (id: string, data: AssetRequest): Promise<Asset> => {
    const response = await axios.put<Asset>(`/assets/${id}`, data);
    return response.data;
  },

  // 刪除資產
  deleteAsset: async (id: string): Promise<void> => {
    await axios.delete(`/assets/${id}`);
  },
};