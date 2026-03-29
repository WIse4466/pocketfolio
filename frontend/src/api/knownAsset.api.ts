import axios from './axios';

export type KnownAssetType = 'STOCK_TW' | 'STOCK_TWO' | 'CRYPTO';

export interface KnownAssetResult {
  symbol: string;      // Yahoo/CoinGecko 相容代號：2330.TW、bitcoin
  name: string;        // 顯示名稱：台積電、Bitcoin
  displayCode: string; // 短代碼：2330、BTC
  assetType: KnownAssetType;
}

export const knownAssetApi = {
  search: (assetType: KnownAssetType, keyword: string) =>
    axios
      .get<KnownAssetResult[]>('/known-assets/search', { params: { assetType, keyword } })
      .then((r) => r.data),
};
