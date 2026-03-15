package com.pocketfolio.backend.dto.websocket;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SystemMessage {

    private String message;
    private String level;  // INFO, WARNING, ERROR
    private LocalDateTime timestamp;
    private String messageType;  // SYSTEM

    public static SystemMessage info(String message) {
        return SystemMessage.builder()
                .message(message)
                .level("INFO")
                .timestamp(LocalDateTime.now())
                .messageType("SYSTEM")
                .build();
    }

    public static SystemMessage warning(String message) {
        return SystemMessage.builder()
                .message(message)
                .level("WARNING")
                .timestamp(LocalDateTime.now())
                .messageType("SYSTEM")
                .build();
    }
}