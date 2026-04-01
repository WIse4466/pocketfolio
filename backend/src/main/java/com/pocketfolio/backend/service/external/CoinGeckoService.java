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

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinGeckoService {

    private final WebClient.Builder webClientBuilder;

    @Value("${api.coingecko.base-url}")
    private String baseUrl;

    /**
     * 取得加密貨幣即時價格（USD）
     *
     * @param coinGeckoId CoinGecko coin id，例如：bitcoin, ethereum（存在 Asset.symbol 欄位）
     * @return 價格（USD）
     */
    @Cacheable(
            value = "prices",
            key = "'crypto:' + #coinGeckoId.toLowerCase()",
            unless = "#result == null"
    )
    public PriceData getPrice(String coinGeckoId) {
        try {
            String id = coinGeckoId.toLowerCase();
            log.info("呼叫 CoinGecko API: {}", id);

            WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();

            CoinGeckoResponse response = webClient.get()
                    .uri("/coins/{id}", id)
                    .retrieve()
                    .bodyToMono(CoinGeckoResponse.class)
                    .block();

            if (response != null && response.getMarketData() != null) {
                BigDecimal price = response.getMarketData().getUsdPrice();
                log.info("CoinGecko - {} 價格: ${}", coinGeckoId, price);
                return PriceData.builder()
                        .symbol(id)
                        .price(price)
                        .updateTime(LocalDateTime.now())
                        .source("CoinGecko")
                        .build();
            }

            log.warn("CoinGecko - 無法取得 {} 的價格", id);
            return null;

        } catch (Exception e) {
            log.error("CoinGecko API 呼叫失敗: {}", e.getMessage());
            return null;
        }
    }
}