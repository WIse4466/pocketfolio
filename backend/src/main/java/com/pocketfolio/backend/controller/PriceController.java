package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.PriceData;
import com.pocketfolio.backend.dto.PriceUpdateResponse;
import com.pocketfolio.backend.entity.AssetType;
import com.pocketfolio.backend.security.SecurityUtil;
import com.pocketfolio.backend.service.PriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
@Tag(name = "價格管理", description = "資產價格查詢與更新")
@SecurityRequirement(name = "bearerAuth")
public class PriceController {

    private final PriceService priceService;

    /**
     * 取得即時價格（從快取或 API）
     */
    @GetMapping("/{symbol}")
    @Operation(
            summary = "查詢即時價格",
            description = "取得指定資產的即時價格，優先從 Redis 快取讀取（5 分鐘 TTL）"
    )
    public ResponseEntity<PriceData> getPrice(
            @PathVariable String symbol,
            @RequestParam AssetType type) {

        PriceData priceData = priceService.getPrice(symbol, type);

        if (priceData == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(priceData);
    }

    /**
     * 手動更新單一資產價格
     */
    @PostMapping("/update/asset/{assetId}")
    @Operation(
            summary = "更新單一資產價格",
            description = "手動觸發指定資產的價格更新，並推播 WebSocket 消息"
    )
    public ResponseEntity<PriceUpdateResponse> updateAssetPrice(@PathVariable UUID assetId) {
        PriceUpdateResponse result = priceService.updateAssetPrice(assetId);
        return ResponseEntity.ok(result);
    }

    /**
     * 手動更新當前用戶的所有資產價格
     */
    @PostMapping("/update/my-assets")
    @Operation(
            summary = "更新我的所有資產價格",
            description = "批次更新當前用戶所有資產的價格"
    )
    public ResponseEntity<List<PriceUpdateResponse>> updateMyAssetPrices() {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<PriceUpdateResponse> results = priceService.updateUserAssetPrices(userId);
        return ResponseEntity.ok(results);
    }

    /**
     * 清除所有價格快取
     */
    @DeleteMapping("/cache")
    @Operation(
            summary = "清除所有價格快取",
            description = "清空 Redis 中的所有價格快取資料"
    )
    public ResponseEntity<String> clearAllCache() {
        priceService.clearAllPriceCache();
        return ResponseEntity.ok("所有價格快取已清除");
    }

    /**
     * 清除特定資產的價格快取
     */
    @DeleteMapping("/cache/{symbol}")
    @Operation(
            summary = "清除特定資產的價格快取",
            description = "清除指定資產的快取，下次查詢時會重新呼叫 API"
    )
    public ResponseEntity<String> clearCache(
            @PathVariable String symbol,
            @RequestParam AssetType type) {
        priceService.clearPriceCache(symbol, type);
        return ResponseEntity.ok("快取已清除: " + symbol);
    }
}