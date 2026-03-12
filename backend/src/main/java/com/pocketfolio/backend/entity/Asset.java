package com.pocketfolio.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assets")
@Getter
@Setter
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetType type;  // STOCK / CRYPTO / FUND / BOND

    @Column(nullable = false)
    private String symbol;  // 2330, NVDA, BTC

    @Column(nullable = false)
    private String name;  // 名稱：台積電、比特幣

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;  // 持有數量

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal costPrice;  // 成本價

    @Column(precision = 19, scale = 8)
    private BigDecimal currentPrice;  // 當前市價

    private LocalDateTime lastPriceUpdate;  // 最後價格更新時間

    private String note;  // 備註

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ────────────── 計算屬性（不存資料庫）──────────────

    @Transient
    public BigDecimal getMarketValue() {
        // 市值 = 數量 × 當前價格
        BigDecimal price = (currentPrice != null) ? currentPrice : costPrice;
        return quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);
    }

    @Transient
    public BigDecimal getProfitLoss() {
        // 損益 = (當前價格 - 成本價) × 數量
        if (currentPrice == null) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(costPrice)
                .multiply(quantity)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transient
    public BigDecimal getProfitLossPercent() {
        // 損益百分比 = (當前價格 - 成本價) / 成本價 × 100
        if (currentPrice == null || costPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(costPrice)
                .divide(costPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
