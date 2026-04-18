import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useWebSocketStore } from './websocketStore';

describe('useWebSocketStore', () => {
  beforeEach(() => {
    useWebSocketStore.setState({ isConnected: false, lastPriceUpdateAt: null });
  });

  describe('初始狀態', () => {
    it('isConnected 為 false', () => {
      expect(useWebSocketStore.getState().isConnected).toBe(false);
    });

    it('lastPriceUpdateAt 為 null', () => {
      expect(useWebSocketStore.getState().lastPriceUpdateAt).toBeNull();
    });
  });

  describe('setConnected', () => {
    it('設定 isConnected 為 true', () => {
      useWebSocketStore.getState().setConnected(true);
      expect(useWebSocketStore.getState().isConnected).toBe(true);
    });

    it('設定 isConnected 為 false', () => {
      useWebSocketStore.getState().setConnected(true);
      useWebSocketStore.getState().setConnected(false);
      expect(useWebSocketStore.getState().isConnected).toBe(false);
    });
  });

  describe('notifyPriceUpdate', () => {
    it('呼叫後 lastPriceUpdateAt 為非 null 數字', () => {
      useWebSocketStore.getState().notifyPriceUpdate();
      expect(useWebSocketStore.getState().lastPriceUpdateAt).not.toBeNull();
      expect(typeof useWebSocketStore.getState().lastPriceUpdateAt).toBe('number');
    });

    it('每次呼叫都更新 lastPriceUpdateAt 時間戳記', () => {
      // 讓時間可以推進
      vi.useFakeTimers();

      useWebSocketStore.getState().notifyPriceUpdate();
      const first = useWebSocketStore.getState().lastPriceUpdateAt;

      vi.advanceTimersByTime(100);
      useWebSocketStore.getState().notifyPriceUpdate();
      const second = useWebSocketStore.getState().lastPriceUpdateAt;

      expect(second).toBeGreaterThan(first!);

      vi.useRealTimers();
    });
  });
});
