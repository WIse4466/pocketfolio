package com.pocketfolio.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "price_alerts")
@Getter
@Setter
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;  // 可選，如果沒有綁定資產就是監控特定代號

    @Column(nullable = false)
    private String symbol;  // 資產代號（BTC, ETH, 2330.TW）

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetType assetType;  // CRYPTO, STOCK

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertCondition condition;  // ABOVE（高於）, BELOW（低於）

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal targetPrice;  // 目標價格

    @Column(nullable = false)
    private boolean active = true;  // 是否啟用

    @Column(nullable = false)
    private boolean triggered = false;  // 是否已觸發

    private LocalDateTime triggeredAt;  // 觸發時間

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private String note;  // 備註

    public enum AlertCondition {
        ABOVE,  // 價格高於目標價
        BELOW   // 價格低於目標價
    }
}