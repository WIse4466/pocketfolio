package com.pocketfolio.backend.dto;

import com.pocketfolio.backend.entity.AssetType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class AssetSnapshotResponse {

    private UUID id;
    private UUID assetId;
    private String symbol;
    private String assetName;
    private AssetType assetType;
    private BigDecimal quantity;
    private BigDecimal costPrice;
    private BigDecimal currentPrice;
    private BigDecimal marketValue;
    private BigDecimal profitLoss;
    private BigDecimal profitLossPercent;
    private LocalDate snapshotDate;
}