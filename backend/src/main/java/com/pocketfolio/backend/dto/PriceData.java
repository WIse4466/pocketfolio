package com.pocketfolio.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceData implements Serializable {

    private String symbol;           // 代號
    private BigDecimal price;        // 價格
    private LocalDateTime updateTime; // 更新時間
    private String source;           // 資料來源（COINGECKO / YAHOO）

    /**
     * 檢查資料是否過期（超過 5 分鐘）
     */
    @JsonIgnore
    public boolean isExpired() {
        if (updateTime == null) {
            return true;
        }
        return updateTime.plusMinutes(5).isBefore(LocalDateTime.now());
    }
}