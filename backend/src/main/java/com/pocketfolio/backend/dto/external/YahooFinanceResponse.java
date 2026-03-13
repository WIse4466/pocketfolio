package com.pocketfolio.backend.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.xml.transform.Result;
import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YahooFinanceResponse {

    private Chart chart;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Chart {
        private List<Result> result;
        private String error;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private Meta meta;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        @JsonProperty("regularMarketPrice")
        private BigDecimal regularMarketPrice;
    }

    public BigDecimal getPrice() {
        if (chart != null && chart.result != null && !chart.result.isEmpty()) {
            Result result = chart.result.get(0);
            if (result.meta != null) {
                return result.meta.regularMarketPrice;
            }
        }
        return null;
    }
}
