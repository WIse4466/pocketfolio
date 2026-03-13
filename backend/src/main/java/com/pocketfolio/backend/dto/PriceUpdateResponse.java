package com.pocketfolio.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PriceUpdateResponse {
    private String symbol;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private BigDecimal changePercent;
    private LocalDateTime updateTime;
    private boolean success;
    private String errorMessage;
}
