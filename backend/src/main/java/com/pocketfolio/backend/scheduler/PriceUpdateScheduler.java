package com.pocketfolio.backend.scheduler;

import com.pocketfolio.backend.service.PriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        value = "scheduler.price-update.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PriceUpdateScheduler {

    private final PriceService priceService;

    /**
     * 定時更新所有資產價格
     *
     * 執行時機：每 5 分鐘執行一次
     * Cron 表達式：秒 分 時 日 月 週
     */
    @Scheduled(cron = "${scheduler.price-update.cron:0 */5 * * * *}")
    public void updateAllPrices() {
        log.info("=== 定時任務開始：更新資產價格 ===");
        log.info("執行時間: {}", LocalDateTime.now());

        try {
            int successCount = priceService.updateAllAssetPrices();
            log.info("=== 定時任務完成：成功更新 {} 筆資產 ===", successCount);

        } catch (Exception e) {
            log.error("=== 定時任務失敗：{} ===", e.getMessage(), e);
        }
    }

    /**
     * 定時清除過期快取
     *
     * 執行時機：每天凌晨 3 點
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void clearExpiredCache() {
        log.info("=== 定時任務開始：清除過期快取 ===");

        try {
            priceService.clearAllPriceCache();
            log.info("=== 定時任務完成：快取已清除 ===");

        } catch (Exception e) {
            log.error("=== 定時任務失敗：{} ===", e.getMessage(), e);
        }
    }
}