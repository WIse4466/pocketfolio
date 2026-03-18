package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.PriceData;
import com.pocketfolio.backend.dto.PriceUpdateResponse;
import com.pocketfolio.backend.dto.websocket.PriceUpdateMessage;
import com.pocketfolio.backend.entity.Asset;
import com.pocketfolio.backend.entity.AssetType;
import com.pocketfolio.backend.entity.PriceAlert;
import com.pocketfolio.backend.repository.AssetRepository;
import com.pocketfolio.backend.service.external.CoinGeckoService;
import com.pocketfolio.backend.service.external.YahooFinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceService {

    private final CoinGeckoService coinGeckoService;
    private final YahooFinanceService yahooFinanceService;
    private final AssetRepository assetRepository;
    private final WebSocketService webSocketService;
    private final PriceAlertService priceAlertService;

    /**
     * 取得即時價格（外部 Service 已自動快取）
     */
    public PriceData getPrice(String symbol, AssetType assetType) {
        log.info("準備查詢資產價格: {}", symbol);
        return fetchPriceFromApi(symbol, assetType);
    }

    /**
     * 從外部 API 取得價格（會自動快取）
     */
    private PriceData fetchPriceFromApi(String symbol, AssetType assetType) {
        try {
            if (assetType == AssetType.CRYPTO) {
                return coinGeckoService.getPrice(symbol);
            } else if (assetType == AssetType.STOCK || assetType == AssetType.FUND) {
                return yahooFinanceService.getPrice(symbol);
            }
            return null;
        } catch (Exception e) {
            log.error("API 呼叫失敗: {} - {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * 更新單一資產的價格
     */
    public PriceUpdateResponse updateAssetPrice(UUID assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("找不到資產"));

        BigDecimal oldPrice = asset.getCurrentPrice();

        // 取得新價格
        PriceData priceData = getPrice(asset.getSymbol(), asset.getType());

        if (priceData == null || priceData.getPrice() == null) {
            return PriceUpdateResponse.builder()
                    .symbol(asset.getSymbol())
                    .success(false)
                    .errorMessage("無法取得價格")
                    .build();
        }

        BigDecimal newPrice = priceData.getPrice();

        // 計算變動百分比
        BigDecimal changePercent = BigDecimal.ZERO;
        if (oldPrice != null && oldPrice.compareTo(BigDecimal.ZERO) > 0) {
            changePercent = newPrice.subtract(oldPrice)
                    .divide(oldPrice, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // 更新資產價格
        asset.setCurrentPrice(newPrice);
        asset.setLastPriceUpdate(LocalDateTime.now());
        assetRepository.save(asset);

        log.info("資產價格已更新: {} ${} -> ${} ({}{}) ",
                asset.getName(), oldPrice, newPrice,
                changePercent.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "",
                changePercent);

        // WebSocket 推播
        PriceUpdateMessage wsMessage = PriceUpdateMessage.fromUpdate(
                asset.getSymbol(),
                asset.getName(),
                oldPrice,
                newPrice
        );

        // 廣播給所有用戶
        webSocketService.broadcastPriceUpdate(wsMessage);

        // 檢查價格警報
        List<PriceAlert> triggeredAlerts = priceAlertService.checkPriceAlerts(
                asset.getSymbol(),
                newPrice
        );

        // 如果有警報被觸發，推送通知
        triggeredAlerts.forEach(alert -> {
            webSocketService.sendPriceAlertToUser(
                    alert.getUser().getId(),
                    alert,
                    newPrice
            );
        });

        return PriceUpdateResponse.builder()
                .symbol(asset.getSymbol())
                .oldPrice(oldPrice)
                .newPrice(newPrice)
                .changePercent(changePercent)
                .updateTime(LocalDateTime.now())
                .success(true)
                .build();
    }

    /**
     * 更新某用戶的所有資產價格
     */
    public List<PriceUpdateResponse> updateUserAssetPrices(UUID userId) {
        List<Asset> assets = assetRepository.findByUserId(userId);

        List<PriceUpdateResponse> results = new ArrayList<>();

        for (Asset asset : assets) {
            try {
                PriceUpdateResponse result = updateAssetPrice(asset.getId());
                results.add(result);


            } catch (Exception e) {
                log.error("更新資產價格失敗: {} - {}", asset.getSymbol(), e.getMessage());
                results.add(PriceUpdateResponse.builder()
                        .symbol(asset.getSymbol())
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        return results;
    }

    /**
     * 更新所有用戶的所有資產價格（定時任務用）
     */
    public int updateAllAssetPrices() {
        int pageSize = 100; // 每次只從資料庫取100筆
        int pageNumber = 0;
        int successCount = 0;

        long totalAssets = assetRepository.count();
        log.info("開始執行全站價格更新排程，預計處理總數：{} 筆", totalAssets);

        if (totalAssets == 0) return 0;

        Page<Asset> assetPage;
        do {
            // 每次只撈取第 pageNumber 頁的 100 筆資料
            assetPage = assetRepository.findAll(PageRequest.of(pageNumber, pageSize));

            log.info("正在處理 {}/{} 批次...", pageNumber + 1, assetPage.getTotalPages());

            for (Asset asset : assetPage.getContent()) {
                try {
                    PriceUpdateResponse result = updateAssetPrice(asset.getId());
                    if (result.isSuccess()) {
                        successCount++;
                    }

                }catch (Exception e) {
                    log.error("更新資產價格失敗: {} - {}", asset.getSymbol(), e.getMessage());
                }
            }
            pageNumber++;
        }while (assetPage.hasNext());

        log.info("全站價格更新排程結束: {}/{} 成功", successCount, totalAssets);
        return successCount;
    }

    /**
     * 清除所有價格快取
     */
    @CacheEvict(value = "prices", allEntries = true)
    public void clearAllPriceCache() {
        log.info("所有價格快取已清除");
    }

    /**
     * 清除特定資產價格快取
     */
    @CacheEvict(
            value = "prices",
            key = "(#assetType.name() == 'CRYPTO' ? 'crypto:' : 'stock:') + #symbol.toUpperCase()"
    )
    public void clearPriceCache(String symbol, AssetType assetType) {
        log.info("特定資產快取已清除: {} ({})", symbol, assetType);
    }
}