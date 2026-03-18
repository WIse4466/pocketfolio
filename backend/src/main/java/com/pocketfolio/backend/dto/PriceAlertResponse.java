package com.pocketfolio.backend.dto;

import com.pocketfolio.backend.entity.AssetType;
import com.pocketfolio.backend.entity.PriceAlert;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PriceAlertResponse {

    private UUID id;
    private UUID assetId;
    private String symbol;
    private AssetType assetType;
    private PriceAlert.AlertCondition condition;
    private BigDecimal targetPrice;
    private boolean active;
    private boolean triggered;
    private LocalDateTime triggeredAt;
    private LocalDateTime createdAt;
    private String note;

    // 額外資訊
    private BigDecimal currentPrice;  // 當前價格
    private String conditionText;     // 條件描述（例如：「當價格低於 60000」）
}