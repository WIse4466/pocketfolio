package com.pocketfolio.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     *
     * /topic - 廣播（一對多）
     * /queue - 點對點（一對一）
     * /app - 客戶端發送消息的目的地前綴
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 啟用簡單的消息代理，處理 /topic 和 /queue 開頭的消息
        config.enableSimpleBroker("/topic", "/queue");

        // 客戶端發送消息時的目的地前綴
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 註冊 STOMP 端點
     *
     * 客戶端連接 WebSocket 的端點
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "http://localhost:5173",  // Vite
                        "http://localhost:3000"   // React
                )
                .withSockJS();  // 支援 SockJS（瀏覽器不支援 WebSocket 時的降級方案）
    }
}