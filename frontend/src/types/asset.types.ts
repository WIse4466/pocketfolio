export type AssetType = 'STOCK' | 'CRYPTO';

export interface Asset {
  id: string;
  accountId: string;
  name: string;
  symbol: string;
  type: AssetType;
  quantity: number;
  costPrice: number;
  currentPrice: number;
  marketValue: number;
  profitLoss: number;
  profitLossPercent: number;
  lastPriceUpdate?: string;
}

export interface AssetRequest {
  accountId: string;
  name: string;
  symbol: string;
  type: AssetType;
  quantity: number;
  costPrice: number;
}