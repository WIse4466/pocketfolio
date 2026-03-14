package com.pocketfolio.backend.service.external;

import com.pocketfolio.backend.dto.PriceData;
import com.pocketfolio.backend.dto.external.CoinGeckoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinGeckoService {

    private final WebClient.Builder webClientBuilder;

    @Value("${api.coingecko.base-url}")
    private String baseUrl;

    // Symbol 映射表（CoinGecko 使用的 ID）
    private static final Map<String, String> SYMBOL_TO_ID = new HashMap<>() {{
        put("BTC", "bitcoin");
        put("ETH", "ethereum");
        put("BNB", "binancecoin");
        put("XRP", "ripple");
        put("ADA", "cardano");
        put("SOL", "solana");
        put("DOGE", "dogecoin");
        put("MATIC", "matic-network");
        // 可以繼續新增更多
    }};

    /**
     * 取得加密貨幣即時價格（USD）
     *
     * @param symbol 代號，例如：BTC, ETH
     * @return 價格（USD）
     */
    @Cacheable(
            value = "prices",
            key = "'crypto:' + #symbol.toUpperCase()",
            unless = "#result == null"
    )
    public PriceData getPrice(String symbol) {
        String coinId = SYMBOL_TO_ID.get(symbol.toUpperCase());

        if (coinId == null) {
            log.warn("不支援的加密貨幣代號: {}", symbol);
            return null;
        }

        try {
            log.info("呼叫 CoinGecko API: {}", symbol);

            WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();

            CoinGeckoResponse response = webClient.get()
                    .uri("/coins/{id}", coinId)
                    .retrieve()
                    .bodyToMono(CoinGeckoResponse.class)
                    .block();

            if (response != null && response.getMarketData() != null) {
                Thread.sleep(500);
                BigDecimal price = response.getMarketData().getUsdPrice();
                log.info("CoinGecko - {} 價格: ${}", symbol, price);
                return PriceData.builder()
                        .symbol(symbol)
                        .price(price)
                        .updateTime(LocalDateTime.now())
                        .source("CoinGecko")
                        .build();
            }

            log.warn("CoinGecko - 無法取得 {} 的價格", symbol);
            return null;

        } catch (Exception e) {
            log.error("CoinGecko API 呼叫失敗: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 檢查是否支援該加密貨幣
     */
    public boolean isSupported(String symbol) {
        return SYMBOL_TO_ID.containsKey(symbol.toUpperCase());
    }
}