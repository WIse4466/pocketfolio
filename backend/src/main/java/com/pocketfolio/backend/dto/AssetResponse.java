package com.pocketfolio.backend.dto;

import com.pocketfolio.backend.entity.AssetType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AssetResponse {
    private UUID id;
    private UUID accountId;
    private String accountName;
    private AssetType type;
    private String symbol;
    private String name;
    private BigDecimal quantity;
    private BigDecimal costPrice;
    private BigDecimal currentPrice;
    private LocalDateTime lastPriceUpdate;
    private BigDecimal marketValue;       // 市值
    private BigDecimal profitLoss;        // 損益金額
    private BigDecimal profitLossPercent; // 損益百分比
    private String priceCurrency;         // 市價幣別：TWD / USD
    private String note;
}