# PocketFolio 待辦事項

更新時間：2026-03-30

---

## ✅ 已完成

### Phase 1-6
- [x] 基礎架構（Spring Boot + PostgreSQL）
- [x] JWT 認證與多用戶資料隔離
- [x] 所有 CRUD 功能（交易、類別、帳戶、資產、警報）
- [x] Redis 快取 + 外部 API（CoinGecko / Yahoo Finance）
- [x] WebSocket 即時價格更新與警報通知
- [x] 資產歷史快照頁面
- [x] 統計分析圖表
- [x] 資料庫複合索引優化

### Phase 7 — 部署與 CI/CD
- [x] Docker multi-stage build
- [x] GCP Cloud Run 後端部署
- [x] Firebase Hosting 前端部署
- [x] GitHub Actions CI/CD Pipeline
- [x] Cloud SQL Auth Connector（Unix socket）
- [x] Upstash Redis（TLS）

### 部署後修復
- [x] Token 過期自動導向 `/login`（axios interceptor + Zustand logout）
- [x] WebSocket CORS — 補上 Firebase Hosting 網域到 `setAllowedOrigins`
- [x] Dashboard 串接真實 API（月度統計、帳戶餘額、帳戶數量）
- [x] 統計分析頁面啟用（新增 StatisticsPage、路由、側邊欄）
- [x] 修正 StatisticsPage API 欄位對應（categoryBreakdown → incomeByCategory/expenseByCategory）
- [x] 新用戶自動建立預設類別（餐飲、交通、娛樂、購物、住房、醫療、教育 / 薪資、獎金、投資收益、其他收入）
- [x] 新用戶自動建立預設帳戶（現金、銀行帳戶、信用卡、投資帳戶）
- [x] 資產搜尋 Autocomplete（known_assets 表 + TWSE/TPEX/CoinGecko 同步 + 前端 AutoComplete）

---

## 🔴 高優先級

### 1. 轉帳交易類別
**問題：** 目前交易只有收入/支出兩種類型，無法記錄帳戶間轉帳。
- [ ] 後端 TransactionType enum 加入 `TRANSFER`
- [ ] 後端轉帳邏輯：一筆轉帳 = 來源帳戶支出 + 目標帳戶收入（需選擇來源/目標帳戶）
- [ ] 前端新增交易表單加入轉帳類型 + 目標帳戶選擇
- [ ] 統計分析排除轉帳（避免重複計算）

### 2. Autocomplete 改善（接續 PR #5）
PR #5 待 merge，merge 後繼續：

**a. Bitcoin 搜尋排名問題**
- 問題：搜尋 "bitcoin" / "BTC" 出現大量垃圾幣，真正的比特幣被埋在後面
- 解法：`KnownAsset` 加 `market_cap_rank` 欄位（nullable）
  - 同步時先打 CoinGecko `/coins/markets?per_page=500` 取前 500 大市值幣並記錄排名
  - 再打 `/coins/list` 補全其餘（排名為 null）
  - 搜尋結果 `ORDER BY market_cap_rank NULLS LAST`

**b. Retry + 指數退避**
- 問題：TWSE/TPEX/CoinGecko 任一 API 偶發失敗時無重試
- 解法：加入 `spring-retry` dependency，`syncTwse/syncTpex/syncCrypto` 各加 `@Retryable(maxAttempts=3, backoff=@Backoff(delay=2000, multiplier=2))`
- 需在 `@SpringBootApplication` 加 `@EnableRetry`

**c. Sanity check 測試**
- 測試場景：mock WebClient 回傳 10 筆（低於閾值），驗證 `deleteByAssetType` 未被呼叫
- 使用 `@SpringBootTest` + Mockito

---

## 🟡 中優先級

### 3. 響應式設計優化
- [ ] 手機版側邊欄（抽屜式 Drawer）
- [ ] 表格在小螢幕的顯示方式
- [ ] 圖表響應式調整
- [ ] 統計卡片佈局（`xs` / `sm` / `md` breakpoints）

### 4. Schema 遷移（Flyway）
**時機：** 下次需要改動資料庫 schema 時引入，不用現在急。
**現狀：** `ddl-auto: update` 可用但有風險（只加不刪欄位，複雜 migration 會失敗）。
- [ ] 引入 Flyway dependency
- [ ] 將現有 schema 轉成 `V1__init.sql`
- [ ] `ddl-auto` 改為 `validate`

### 5. 單元測試補完
- [ ] KnownAssetSyncService sanity check 測試（API 回傳異常筆數，舊資料不被清除）
- [ ] AssetServiceTest
- [ ] PriceServiceTest

---

## 🟢 低優先級

### 6. Service 層重構
- [ ] 提取共用用戶驗證邏輯（目前各 service 重複）

### 7. 監控
- [ ] GCP Billing 預算警報（建議 $20/月上限）
- [ ] Spring Boot Actuator + Cloud Run health check

### 8. 前端效能
- [ ] 代碼分割 + 懶加載
- [ ] 大列表虛擬滾動

---

## 📝 筆記

- WebSocket 認證透過 STOMP connectHeaders 傳 JWT，非 HTTP header
- CoinGecko 查詢用內部 `id`（如 `"bitcoin"`），不是 symbol（`"BTC"`）
- Cloud Run 每次部署後 URL 不變，但 revision 會更新
- `ddl-auto: update` 目前在用，未來換 Flyway 前先不動
- GCP Cloud SQL 費用約 $7-10/月（db-f1-micro，asia-east1）
- `known_assets` 欄位長度：symbol 100、display_code 50、name 200（CoinGecko 有超長 symbol 的垃圾幣）
- TWSE suffix `.TW`，TPEX suffix `.TWO`，CRYPTO 存 CoinGecko id（如 `bitcoin`）
