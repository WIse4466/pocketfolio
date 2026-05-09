package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.PriceData;
import com.pocketfolio.backend.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchange-rate")
@RequiredArgsConstructor
@Tag(name = "匯率", description = "外幣匯率查詢")
@SecurityRequirement(name = "bearerAuth")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/usd-twd")
    @Operation(summary = "查詢 USD/TWD 匯率", description = "從 Yahoo Finance 取得即時匯率，快取 5 分鐘")
    public ResponseEntity<PriceData> getUsdToTwd() {
        return ResponseEntity.ok(exchangeRateService.getUsdToTwd());
    }
}
