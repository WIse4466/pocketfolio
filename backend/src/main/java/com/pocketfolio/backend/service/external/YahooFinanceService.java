package com.pocketfolio.backend.service.external;

import com.pocketfolio.backend.dto.PriceData;
import com.pocketfolio.backend.dto.external.YahooFinanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class YahooFinanceService {

    private final WebClient.Builder webClientBuilder;

    @Value("${api.yahoo-finance.base-url}")
    private String baseUrl;

    /**
     * 取得股票即時價格
     *
     * @param symbol 股票代號，例如：2330.TW, AAPL, TSLA
     * @return 價格
     */
    @Cacheable(
            value = "prices",
            key = "'stock:' + #symbol.toUpperCase()",
            unless = "#result == null"
    )
    public PriceData getPrice(String symbol) {
        try {
            log.info("呼叫 Yahoo Finance API: {}", symbol);

            WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();

            YahooFinanceResponse response = webClient.get()
                    .uri("/{symbol}?interval=1d&range=1d", symbol)
                    .retrieve()
                    .bodyToMono(YahooFinanceResponse.class)
                    .block();

            if (response != null && response.getPrice() != null) {
                BigDecimal price = response.getPrice();
                log.info("Yahoo Finance - {} 價格: {}", symbol, price);

                return PriceData.builder()
                        .symbol(symbol)
                        .price(price)
                        .updateTime(LocalDateTime.now())
                        .source("YAHOO_FINANCE")
                        .build();
            }

            log.warn("Yahoo Finance - 無法取得 {} 的價格", symbol);
            return null;

        } catch (Exception e) {
            log.error("Yahoo Finance API 呼叫失敗: {} - {}", symbol, e.getMessage());
            return null;
        }
    }
}