package com.pocketfolio.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AssetHistoryResponse {

    private String symbol;
    private String assetName;
    private List<DataPoint> history;  // 歷史資料點

    @Data
    @Builder
    public static class DataPoint {
        private String date;  // YYYY-MM-DD
        private Double price;
        private Double marketValue;
        private Double profitLoss;
        private Double profitLossPercent;
    }
}