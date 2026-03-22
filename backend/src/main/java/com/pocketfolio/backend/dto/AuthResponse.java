package com.pocketfolio.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String type = "Bearer";  // Token 類型
    private UUID userId;
    private String email;
    private String displayName;
}
