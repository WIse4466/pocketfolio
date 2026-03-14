package com.pocketfolio.backend.controller;

import com.pocketfolio.backend.dto.PriceData;
import com.pocketfolio.backend.dto.PriceUpdateResponse;
import com.pocketfolio.backend.entity.AssetType;
import com.pocketfolio.backend.security.SecurityUtil;
import com.pocketfolio.backend.service.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;

    /**
     * 取得即時價格（從快取或 API）
     */
    @GetMapping("/{symbol}")
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
    public ResponseEntity<PriceUpdateResponse> updateAssetPrice(@PathVariable UUID assetId) {
        PriceUpdateResponse result = priceService.updateAssetPrice(assetId);
        return ResponseEntity.ok(result);
    }

    /**
     * 手動更新當前用戶的所有資產價格
     */
    @PostMapping("/update/my-assets")
    public ResponseEntity<List<PriceUpdateResponse>> updateMyAssetPrices() {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<PriceUpdateResponse> results = priceService.updateUserAssetPrices(userId);
        return ResponseEntity.ok(results);
    }

    /**
     * 清除所有價格快取
     */
    @DeleteMapping("/cache")
    public ResponseEntity<String> clearAllCache() {
        priceService.clearAllPriceCache();
        return ResponseEntity.ok("所有價格快取已清除");
    }

    /**
     * 清除特定資產的價格快取
     */
    @DeleteMapping("/cache/{symbol}")
    public ResponseEntity<String> clearCache(
            @PathVariable String symbol,
            @RequestParam AssetType type) {
        priceService.clearPriceCache(symbol, type);
        return ResponseEntity.ok("快取已清除: " + symbol);
    }
}