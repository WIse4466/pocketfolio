package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.websocket.PriceUpdateMessage;
import com.pocketfolio.backend.dto.websocket.SystemMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 廣播價格更新給所有連線的用戶
     *
     * 目的地：/topic/price-updates
     */
    public void broadcastPriceUpdate(PriceUpdateMessage message) {
        log.info("廣播價格更新: {} ${} → ${}",
                message.getSymbol(),
                message.getOldPrice(),
                message.getNewPrice());

        messagingTemplate.convertAndSend("/topic/price-updates", message);
    }

    /**
     * 發送價格更新給特定用戶
     *
     * 目的地：/queue/price-updates-{userId}
     */
    public void sendPriceUpdateToUser(UUID userId, PriceUpdateMessage message) {
        log.info("發送價格更新給用戶 {}: {}", userId, message.getSymbol());

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/price-updates",
                message
        );
    }

    /**
     * 廣播系統消息
     *
     * 目的地：/topic/system
     */
    public void broadcastSystemMessage(SystemMessage message) {
        log.info("廣播系統消息: {}", message.getMessage());

        messagingTemplate.convertAndSend("/topic/system", message);
    }

    /**
     * 發送系統消息給特定用戶
     */
    public void sendSystemMessageToUser(UUID userId, SystemMessage message) {
        log.info("發送系統消息給用戶 {}: {}", userId, message.getMessage());

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/system",
                message
        );
    }
}