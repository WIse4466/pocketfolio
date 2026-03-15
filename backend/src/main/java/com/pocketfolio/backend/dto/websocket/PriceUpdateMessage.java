package com.pocketfolio.backend.dto.websocket;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PriceUpdateMessage {

    private String symbol;              // 資產代號
    private String assetName;           // 資產名稱
    private BigDecimal oldPrice;        // 舊價格
    private BigDecimal newPrice;        // 新價格
    private BigDecimal changeAmount;    // 變動金額
    private BigDecimal changePercent;   // 變動百分比
    private LocalDateTime updateTime;   // 更新時間
    private String messageType;         // 消息類型（PRICE_UPDATE）

    /**
     * 快速建立價格更新消息
     */
    public static PriceUpdateMessage fromUpdate(
            String symbol,
            String assetName,
            BigDecimal oldPrice,
            BigDecimal newPrice) {

        BigDecimal changeAmount = newPrice.subtract(oldPrice);
        BigDecimal changePercent = BigDecimal.ZERO;

        if (oldPrice.compareTo(BigDecimal.ZERO) > 0) {
            changePercent = changeAmount
                    .divide(oldPrice, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }

        return PriceUpdateMessage.builder()
                .symbol(symbol)
                .assetName(assetName)
                .oldPrice(oldPrice)
                .newPrice(newPrice)
                .changeAmount(changeAmount)
                .changePercent(changePercent)
                .updateTime(LocalDateTime.now())
                .messageType("PRICE_UPDATE")
                .build();
    }
}