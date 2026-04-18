# PocketFolio 待辦事項

更新時間：2026-04-19

---

## ✅ 已完成

### Phase 1–6：核心功能
- [x] 基礎架構（Spring Boot + PostgreSQL）
- [x] JWT 認證與多用戶資料隔離
- [x] 所有 CRUD 功能（交易、類別、帳戶、資產、警報）
- [x] Redis 快取 + 外部 API（CoinGecko / Yahoo Finance）
- [x] WebSocket 即時價格更新與警報通知
- [x] 資產歷史快照頁面、統計分析圖表
- [x] 資料庫複合索引優化

### Phase 7：部署與 CI/CD
- [x] Docker multi-stage build
- [x] GCP Cloud Run 後端部署 + Firebase Hosting 前端部署
- [x] GitHub Actions CI/CD Pipeline（部署）
- [x] GitHub Actions CI Pipeline（PR 自動測試：後端 mvnw test + 前端 lint/build）
- [x] Cloud SQL Auth Connector（Unix socket）
- [x] Upstash Redis（TLS）

### 功能迭代
- [x] Token 過期自動導向 `/login`（axios interceptor + Zustand logout）
- [x] WebSocket CORS 修正
- [x] Dashboard 串接真實 API
- [x] 新用戶自動建立預設類別（11 個）與帳戶（4 個）
- [x] 資產搜尋 Autocomplete（known_assets + TWSE/TPEX/CoinGecko 同步）
- [x] `@Retryable` 指數退避 + Sanity check
- [x] 修正 Invalid Date（Jackson write-dates-as-timestamps=false）
- [x] 轉帳（Double-Entry Lite）：TRANSFER_OUT / TRANSFER_IN + transfer_group_id 配對
- [x] 資產購買連結帳戶扣款：建立資產時可選填 `fromAccountId`，自動建立轉帳配對記錄
- [x] AssetServiceTest（8 個單元測試）
- [x] 修正 Cloud Run 部署失敗（HikariCP maximum-pool-size=5，避免 db-f1-micro 連線上限）

---

## 🔴 高優先級

### 1. 轉帳連結資產（新分支：`feature/transfer-link-asset`）
**目標：** 在交易頁建立轉帳時，若目標帳戶是投資帳戶，可選擇連結既有資產（加倉）或建立新資產記錄。
- [ ] 後端：`TransactionRequest` 加入選填 `assetId`（加倉）或新資產欄位
- [ ] 後端：`TransactionService.createTransfer()` 若目標是 INVESTMENT 帳戶且帶有資產資訊，同步更新 / 建立資產
- [ ] 前端：目標帳戶選擇後，若是 INVESTMENT 類型，動態顯示資產選擇器（現有資產 + 「新增資產」選項）
- [ ] 單元測試覆蓋新邏輯

---

## 🟡 中優先級

### 2. 響應式設計優化
- [ ] 手機版側邊欄（Drawer）
- [ ] 表格在小螢幕的顯示
- [ ] 圖表響應式調整

### 3. Schema 遷移（Flyway）
**現狀：** `ddl-auto: update` 可用，有風險。等下次改 schema 時再引入。
- [ ] 引入 Flyway、將現有 schema 轉為 `V1__init.sql`、`ddl-auto: validate`

### 4. 測試補完
- [ ] PriceServiceTest

---

## 🟢 低優先級

### 5. 用戶設定
- [ ] 更改顯示名稱 / 密碼
- [ ] 忘記密碼（Email 重設）
- [ ] 多語言（i18n，中 / 英）

### 6. 監控與維運
- [ ] Spring Boot Actuator + Cloud Run health check
- [ ] GCP Cloud Logging 結構化日誌
- [ ] GCP Billing 預算警報（建議 $20/月上限）

### 7. 前端效能
- [ ] 代碼分割 + 懶加載
- [ ] 大列表虛擬滾動

---

## 📝 技術筆記

- WebSocket 認證透過 STOMP connectHeaders 傳 JWT，非 HTTP header
- CoinGecko 查詢用內部 `id`（如 `"bitcoin"`），不是 symbol（`"BTC"`）
- `known_assets` 欄位長度：symbol 100、name 200（CoinGecko 有超長 symbol 的垃圾幣）
- TWSE suffix `.TW`，TPEX suffix `.TWO`，CRYPTO 存 CoinGecko id
- Cloud SQL db-f1-micro max_connections=25；HikariCP pool-size 設 5，避免滾動部署時超限
- `ddl-auto: update` 目前在用，未來換 Flyway 前先不動
- GCP Cloud SQL 費用約 $7–10/月（db-f1-micro，asia-east1）
