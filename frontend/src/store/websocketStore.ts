import { create } from 'zustand';

interface WebSocketStore {
  isConnected: boolean;
  lastPriceUpdateAt: number | null; // timestamp，AssetList 監聽這個來觸發 reload

  setConnected: (connected: boolean) => void;
  notifyPriceUpdate: () => void;
}

export const useWebSocketStore = create<WebSocketStore>((set) => ({
  isConnected: false,
  lastPriceUpdateAt: null,

  setConnected: (connected) => set({ isConnected: connected }),
  notifyPriceUpdate: () => set({ lastPriceUpdateAt: Date.now() }),
}));
