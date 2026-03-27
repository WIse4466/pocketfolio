import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { notification } from 'antd';
import { useWebSocketStore } from '@/store/websocketStore';
import { useAuthStore } from '@/store/authStore';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

export const useWebSocket = () => {
  const clientRef = useRef<Client | null>(null);
  const { setConnected, notifyPriceUpdate } = useWebSocketStore();
  const { token } = useAuthStore();

  useEffect(() => {
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),

      // JWT 認證透過 STOMP header 傳入
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },

      reconnectDelay: 5000, // 斷線後 5 秒自動重連

      onConnect: () => {
        setConnected(true);

        // 訂閱廣播價格更新（所有用戶）
        client.subscribe('/topic/price-updates', () => {
          notifyPriceUpdate();
        });

        // 訂閱個人警報通知
        client.subscribe('/user/queue/alerts', (message) => {
          const alert = JSON.parse(message.body);
          notification.warning({
            message: '價格警報觸發',
            description: `${alert.assetName}（${alert.symbol}）已達到您設定的警報條件`,
            placement: 'topRight',
            duration: 8,
          });
        });
      },

      onDisconnect: () => {
        setConnected(false);
      },

      onStompError: () => {
        setConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [token]);
};
