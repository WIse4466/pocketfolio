package com.pocketfolio.backend.scheduler;

import com.pocketfolio.backend.repository.KnownAssetRepository;
import com.pocketfolio.backend.service.KnownAssetSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KnownAssetSyncScheduler {

    private final KnownAssetSyncService syncService;
    private final KnownAssetRepository knownAssetRepository;

    /**
     * 應用程式啟動後執行一次初始化同步。
     * 若 DB 已有資料（重啟時），跳過以節省 API 呼叫。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initOnStartup() {
        // 分別檢查每個類型，確保各類型都有資料（避免部分同步後重啟跳過剩餘類型）
        if (knownAssetRepository.countByAssetType("STOCK_TW") == 0) {
            log.info("STOCK_TW 為空，執行同步...");
            syncService.syncTwse();
        }
        if (knownAssetRepository.countByAssetType("STOCK_TWO") == 0) {
            log.info("STOCK_TWO 為空，執行同步...");
            syncService.syncTpex();
        }
        if (knownAssetRepository.countByAssetType("CRYPTO") == 0) {
            log.info("CRYPTO 為空，執行同步...");
            syncService.syncCrypto();
        }
    }

    /**
     * 每天凌晨 2 點執行全量同步。
     * 直接呼叫 syncService 的各個方法（經過 Spring proxy），確保 @Retryable 和 @Transactional 正常生效。
     * 不可改成 syncService.syncAll() 內部 this:: 呼叫，否則會繞過 AOP proxy（self-invocation 問題）。
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void dailySync() {
        log.info("=== 定時任務開始：同步資產清單 ===");
        syncSafe("TWSE", syncService::syncTwse);
        syncSafe("TPEX", syncService::syncTpex);
        syncSafe("CoinGecko", syncService::syncCrypto);
        log.info("=== 定時任務完成：資產清單同步 ===");
    }

    // retry 耗盡後 log error，不讓例外往上傳中斷排程
    private void syncSafe(String source, java.util.function.IntSupplier syncFn) {
        try {
            int count = syncFn.getAsInt();
            log.info("{} 同步完成：{} 筆", source, count);
        } catch (Exception e) {
            log.error("{} 同步最終失敗（已重試 3 次）：{}", source, e.getMessage());
        }
    }
}
