package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.websocket.SystemMessage;
import com.pocketfolio.backend.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ws-test")
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final WebSocketService webSocketService;

    /**
     * 測試廣播消息
     */
    @GetMapping("/broadcast-test")
    public String broadcastTest() {
        webSocketService.broadcastSystemMessage(
                SystemMessage.info("這是一則測試廣播消息")
        );
        return "廣播已發送";
    }

    /**
     * 接收客戶端消息並回應
     *
     * 客戶端發送到：/app/hello
     * 廣播到：/topic/greetings
     */
    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public SystemMessage greeting(String message) {
        log.info("收到客戶端消息: {}", message);
        return SystemMessage.info("Hello, " + message + "!");
    }
}