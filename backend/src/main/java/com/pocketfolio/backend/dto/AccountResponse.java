package com.pocketfolio.backend.dto;

import com.pocketfolio.backend.entity.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AccountResponse {
    private UUID id;
    private String name;
    private AccountType type;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private String description;
    private String currency;
    private List<AssetSummary> assets;

    @Data
    @Builder
    public static class AssetSummary {
        private UUID id;
        private String symbol;
        private String name;
        private BigDecimal marketValue;
        private BigDecimal profitLossPercent;
    }
}