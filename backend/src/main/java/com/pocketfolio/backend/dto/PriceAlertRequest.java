package com.pocketfolio.backend.dto;

import com.pocketfolio.backend.entity.AssetType;
import com.pocketfolio.backend.entity.PriceAlert;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PriceAlertRequest {

    private UUID assetId;  // 可選，綁定特定資產

    @NotBlank(message = "資產代號不能為空")
    private String symbol;

    @NotNull(message = "資產類型不能為空")
    private AssetType assetType;

    @NotNull(message = "警報條件不能為空")
    private PriceAlert.AlertCondition condition;

    @NotNull(message = "目標價格不能為空")
    @Positive(message = "目標價格必須大於 0")
    private BigDecimal targetPrice;

    private String note;
}