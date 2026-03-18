# Day 16: 歷史資產快照 (Snapshot) 系統實作與除錯全紀錄
## 1. 架構設計：為歷史數據「拍快照」
   為了讓前端能畫出資產走勢圖，我們不能只依賴記錄「當下狀態」的 Asset 資料表，而是必須導入快照機制，定期將數據凍結保存。

### A. 實體層 (AssetSnapshot Entity)
- 靜態工廠方法 (fromAsset)：將「從現有資產複製資料並押上今天日期」的邏輯封裝在 Entity 內部，保持 Service 層乾淨。

- 冗餘欄位設計：刻意保存 symbol、assetName 等文字欄位，確保歷史紀錄的「絕對正確性」（即便未來公司改名，歷史快照也不會被牽連變更），同時減少關聯查詢的負擔。

- 資料庫索引 (@Index)：針對 (asset_id, snapshot_date) 與 (user_id, snapshot_date) 建立索引，避免日後龐大的歷史數據拖垮查詢效能（全表掃描）。

### B. 傳輸物件 (DTOs)
針對不同的前端畫面需求，我們量身打造了三種 DTO：

1. AssetSnapshotResponse：單一資產的單日體檢報告（表格明細）。

2. PortfolioSnapshotResponse：投資組合的單日總結算（Dashboard 儀表板首頁）。

3. AssetHistoryResponse：將時間序列資料打包。特別將圖表用的資料點 (DataPoint) 中的金額型別從 BigDecimal 降級為 Double，大幅減輕前端繪圖套件（如 Recharts）處理資料的負擔。

## 2. 核心邏輯與自動化排程
### A. 業務邏輯 (AssetSnapshotService)
- 防呆機制：在寫入快照前，先檢查 existsByAssetIdAndSnapshotDate，確保同一天同一資產不會重複拍照。

- 群組化運算：在計算歷史走勢時，靈活運用 Java 8 Stream API 的 Collectors.groupingBy(AssetSnapshot::getSnapshotDate)，將雜亂的快照按日期分組後再進行加總。

### B. 定時任務 (@Scheduled)
- 純 Java 背景執行緒：Spring Boot 的 Scheduler 是跑在 JVM 內部的機制。透過 Cron 語法 (0 0 1 * * *) 設定於每日凌晨 1 點執行全站快照。

- 容錯機制 (Try-Catch)：批次任務絕對不能因為單筆資料異常而中斷。務必在迴圈內使用 try-catch 包覆，並將錯誤寫入 Log。

- 隱藏地雷注意：Spring Boot 預設只有「單一執行緒」處理所有定時任務。若未來任務增加，需在 application.yml 中擴充 Thread Pool。

## 3. RESTful API 設計 (AssetSnapshotController)
   完美落實了 HTTP 狀態碼與 Spring 便利工具的運用：

- 409 Conflict (衝突)：當前端要求手動建立快照，但今天已經拍過時，精準回傳 409，而非籠統的 400/500。

- 404 Not Found (找不到)：當查詢的日期沒有任何快照資料時，標準回傳 404。

- @DateTimeFormat：由 Spring 自動將前端傳來的 "YYYY-MM-DD" 字串轉換為 Java 的 LocalDate 物件。

## 4. 實戰除錯紀錄 (Debugging)
### 🐛 Bug 1: Spring Boot 啟動失敗 - 循環依賴 (Circular Dependency)
- 案發現場：PriceAlertService 與 PriceService 在建構子互相注入，導致「雞生蛋，蛋生雞」的死結 (Exit code 1)。

- 解決方案：職責上移 (Responsibility Push-up)

  - 不依賴魔法：放棄使用 @Lazy 延遲載入或直接關閉循環依賴檢查。

  - 重構資料流：將 PriceAlertService 恢復為單純的 CRUD 服務，拔除對 PriceService 的依賴。改由最外層的 PriceAlertController 擔任「組裝廠 (Orchestrator)」，分別向兩個 Service 取得警報資料與最新市價後，在 Controller 內合併 (enrichWithCurrentPrice) 並回傳給前端。

  - (註：未來若資料量擴大，需留意迴圈呼叫外部 API 造成的 N+1 問題，屆時可導入批次查詢或快取機制)

### 🐛 Bug 2: Swagger 查詢特定日期快照回傳 404 Not Found
- 案發現場：呼叫 GET /api/snapshots/portfolio/2026-03-17 時，得到 404 錯誤。

- 原因分析：這不是程式錯誤，而是因為系統剛上線，資料庫中確實沒有昨天 (3/17) 的快照資料。程式邏輯完美地執行了 if (response == null) { return ResponseEntity.notFound().build(); }。

- 驗證方式：先透過 POST /api/snapshots/asset/{id} 手動為資產拍下「今天」的快照，再將查詢 API 的日期改為「今天」，即可順利取得 200 OK 與完整的 JSON 報表。