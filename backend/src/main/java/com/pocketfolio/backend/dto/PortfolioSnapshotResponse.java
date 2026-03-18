package com.pocketfolio.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PortfolioSnapshotResponse {

    private LocalDate date;
    private BigDecimal totalMarketValue;  // 總市值
    private BigDecimal totalCost;         // 總成本
    private BigDecimal totalProfitLoss;   // 總損益
    private BigDecimal totalProfitLossPercent;  // 總損益百分比
    private int assetCount;               // 資產數量
    private List<AssetSnapshotResponse> assets;  // 詳細資產列表
}