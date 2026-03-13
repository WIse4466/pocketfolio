package com.pocketfolio.backend.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinGeckoResponse {

    @JsonProperty("market_data")
    private MarketData marketData;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MarketData {

        @JsonProperty("current_price")
        private Map<String, BigDecimal> currentPrice;

        public BigDecimal getUsdPrice() {
            return currentPrice != null ? currentPrice.get("usd") : null;
        }
    }
}
