package com.pocketfolio.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "asset_snapshots", indexes = {
        @Index(name = "idx_asset_date", columnList = "asset_id, snapshot_date"),
        @Index(name = "idx_user_date", columnList = "user_id, snapshot_date")
})
@Getter
@Setter
public class AssetSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false)
    private String symbol;  // 冗餘欄位，方便查詢

    @Column(nullable = false)
    private String assetName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetType assetType;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;  // 持有數量

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal costPrice;  // 成本價

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentPrice;  // 快照時的市價

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal marketValue;  // 市值（數量 × 市價）

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal profitLoss;  // 損益金額

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal profitLossPercent;  // 損益百分比

    @Column(nullable = false)
    private LocalDate snapshotDate;  // 快照日期

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 從 Asset 建立快照
     */
    public static AssetSnapshot fromAsset(Asset asset) {
        AssetSnapshot snapshot = new AssetSnapshot();
        snapshot.setUser(asset.getUser());
        snapshot.setAsset(asset);
        snapshot.setSymbol(asset.getSymbol());
        snapshot.setAssetName(asset.getName());
        snapshot.setAssetType(asset.getType());
        snapshot.setQuantity(asset.getQuantity());
        snapshot.setCostPrice(asset.getCostPrice());
        snapshot.setCurrentPrice(asset.getCurrentPrice());
        snapshot.setMarketValue(asset.getMarketValue());
        snapshot.setProfitLoss(asset.getProfitLoss());
        snapshot.setProfitLossPercent(asset.getProfitLossPercent());
        snapshot.setSnapshotDate(LocalDate.now());
        return snapshot;
    }
}