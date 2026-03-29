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
        long count = knownAssetRepository.count();
        if (count > 0) {
            log.info("known_assets 表已有 {} 筆資料，跳過啟動初始化同步", count);
            return;
        }
        log.info("known_assets 表為空，執行首次同步...");
        syncService.syncAll();
    }

    /**
     * 每天凌晨 2 點執行全量同步。
     * 台股收盤後新增或下市的清單會在隔日更新。
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void dailySync() {
        log.info("=== 定時任務開始：同步資產清單 ===");
        syncService.syncAll();
        log.info("=== 定時任務完成：資產清單同步 ===");
    }
}
