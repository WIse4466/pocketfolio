# PocketFolio 待改進清單

---

## 🔴 高優先級（Phase 5-6 期間）

### 1. 資料庫索引優化
**位置：** Entity 類別  
**問題：** Transaction.date 常用於查詢但沒有索引  
**解決方案：**
```java
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_date", columnList = "date"),
    @Index(name = "idx_transaction_user_date", columnList = "user_id, date")
})
```
**預計時間：** 10 分鐘

### 2. 修復廣泛存在的 N+1 查詢問題
**問題：** 列表查詢（List/Page）未 fetch 關聯對象，導致轉換 DTO 時產生大量額外查詢。
**待處理清單：**
- [ ] **TransactionRepository**: 在 `findByUserId` 等方法使用 `@EntityGraph` 加載 `category` 與 `account`。
- [ ] **StaticsService**: `getMonthlySummary` 查詢時應 fetch `category`。
- [ ] **PriceAlertRepository**: `findActiveAlertsBySymbol` 應 fetch `user` 避免推播時觸發查詢。
- [ ] **AccountService**: `calculateCurrentBalance` 避免在迴圈中逐一查詢交易統計，應改用單次批量查詢。

---

## 🟡 中優先級（Phase 7）

### 3. 用戶驗證邏輯重複
**位置：** 所有 Service  
**問題：** 每個 Service 都有相同的驗證邏輯 `if (!entity.getUser().getId().equals(currentUserId))`。
**解決方案選項：**
- 選項 A：提取到 BaseService
- 選項 B：使用 Spring AOP 攔截並驗證權限
- 選項 C：使用 @PreAuthorize SpEL 進行所屬權檢查

**預計時間：** 1-2 小時

### 4. 價格更新機制優化 (PriceService)
**問題：** `updateAllAssetPrices` 針對「每個資產」獨立抓取價格，導致 API 頻率限制（Rate Limit）與效能低下。
**解決方案：** 改為「依 Symbol 分組」抓取價格。例如 10 個用戶持有 BTC，只需呼叫 1 次 API，再批次更新 10 個資產。

### 5. 快照任務重構 (AssetSnapshotService)
**問題：** 
- `createAllSnapshots` 使用 `findAll()` 存在記憶體溢出風險，應改用分頁。
- 迴圈中逐一檢查快照是否存在（N+1 問題），應改用批量存在性檢查。

### 6. Entity → DTO 轉換自動化
**位置：** 所有 Service 的 toResponse() 方法  
**問題：** 手動轉換繁瑣且容易出錯  
**解決方案：** 使用 MapStruct
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
```
**預計時間：** 2-3 小時

### 7. 補完單元測試
**位置：** src/test/java  
**缺少測試：**
- [ ] CategoryServiceTest
- [ ] AccountServiceTest
- [ ] AssetServiceTest
- [ ] PriceServiceTest
- [ ] PriceAlertServiceTest
- [ ] AssetSnapshotServiceTest

**預計時間：** 3-5 小時

### 8. 整合測試
**位置：** src/test/java  
**需求：**
- [ ] API 層整合測試（@SpringBootTest + MockMvc）
- [ ] WebSocket 測試
- [ ] Redis 快取測試

**預計時間：** 2-3 小時

---

## 🟢 低優先級（後續優化）

### 6. 日誌改進
**問題：** 部分 log 資訊不足  
**解決方案：**
- 加入請求 ID 追蹤
- 統一日誌格式
- 敏感資訊遮罩

**預計時間：** 1 小時

---

### 7. 異常處理細化
**問題：** 部分異常訊息對用戶不友善  
**解決方案：**
- 細化異常類型
- 國際化錯誤訊息

**預計時間：** 1-2 小時

---

### 8. API 限流
**問題：** 沒有 API 限流保護  
**解決方案：**
- Spring Cloud Gateway + Redis
- 或 Bucket4j

**預計時間：** 2 小時

---

### 9. 監控與告警
**問題：** 沒有應用監控  
**解決方案：**
- Spring Boot Actuator
- Prometheus + Grafana
- 或 ELK Stack

**預計時間：** 3-4 小時

---

### 10. 文檔完善
**待補：**
- [ ] 架構設計文檔
- [ ] 資料庫 ER 圖
- [ ] API 變更日誌
- [ ] 部署文檔

**預計時間：** 2-3 小時

---

## 📝 記錄

### 2026-03-XX - Code Review
- ✅ 完成 Phase 4 開發
- ✅ 解決循環依賴問題
- ✅ 識別待改進項目
- ⬜ 開始 Phase 5 前端開發

---

## 🎯 Phase 7 檢查清單

完成 Phase 5-6 後，Phase 7 應處理：

- [ ] 高優先級（TODO #1）
- [ ] 中優先級（TODO #2-5）
- [ ] 選擇性低優先級項目
- [ ] Docker 生產環境配置
- [ ] CI/CD Pipeline
- [ ] 性能測試
- [ ] 安全掃描