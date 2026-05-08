package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.PriceData;
import com.pocketfolio.backend.service.external.YahooFinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private static final String USD_TWD_SYMBOL = "USDTWD=X";
    private static final BigDecimal FALLBACK_RATE = new BigDecimal("32.0");

    private final YahooFinanceService yahooFinanceService;

    /**
     * 取得 USD → TWD 匯率（結果由 YahooFinanceService 的 @Cacheable 快取 5 分鐘）
     * 若 Yahoo Finance 無法取得，回傳保守預設值並記錄警告。
     */
    public PriceData getUsdToTwd() {
        PriceData data = yahooFinanceService.getPrice(USD_TWD_SYMBOL);
        if (data == null || data.getPrice() == null) {
            log.warn("無法取得 USD/TWD 匯率，使用預設值 {}", FALLBACK_RATE);
            return PriceData.builder()
                    .symbol(USD_TWD_SYMBOL)
                    .price(FALLBACK_RATE)
                    .currency("TWD")
                    .source("FALLBACK")
                    .build();
        }
        data.setCurrency("TWD");
        return data;
    }
}
