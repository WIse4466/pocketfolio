package com.pocketfolio.backend;

import com.pocketfolio.backend.scheduler.KnownAssetSyncScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BackendApplicationTests {

    // 避免 @EventListener(ApplicationReadyEvent) 在測試啟動時呼叫外部 API
    @MockitoBean
    KnownAssetSyncScheduler knownAssetSyncScheduler;

    @Test
    void contextLoads() {
    }

}
