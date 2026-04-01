# PocketFolio 待辦事項

更新時間：2026-03-31

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
- [x] Autocomplete 改善：改同步市值前 200（移除廢棄幣）、顯示排名標籤、CoinGecko ID 正規化小寫
- [x] Retry + 指數退避（spring-retry @Retryable maxAttempts=3）+ Sanity check 測試
- [x] 修正 Invalid Date（Jackson write-dates-as-timestamps=false）
- [x] 修正價格更新 UI 誤報成功（檢查 result.success）
- [x] 轉帳交易（Double-Entry Lite）：TRANSFER_OUT / TRANSFER_IN + transfer_group_id 配對，刪除串聯，統計排除轉帳

---

## 🔴 高優先級

### 1. 資產購買連結帳戶扣款
**問題：** 買入資產時應同步從指定帳戶扣款，目前兩者完全獨立。
- [ ] 後端：建立資產時可選填 `fromAccountId`
- [ ] 後端：若有 `fromAccountId`，自動建立一筆 `TRANSFER_OUT`（帳戶 → 投資）配對的 `EXPENSE` 交易
- [ ] 前端：資產新增表單加入「從哪個帳戶扣款」選填欄位

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
- [x] KnownAssetSyncService sanity check 測試（TWSE / TPEX / CoinGecko 各 2 個測試案例）
- [ ] AssetServiceTest
- [ ] PriceServiceTest

---

## 🟢 低優先級

### 6. Service 層重構
- [ ] 提取共用用戶驗證邏輯（目前各 service 重複）

### 7. 日誌管理
- [ ] 後端：引入結構化日誌（Logback JSON appender），方便 GCP Cloud Logging 查詢
- [ ] 後端：敏感操作審計 log（登入、刪除、轉帳）
- [ ] GCP Cloud Logging：設定 log-based alert（Error 率異常時通知）

### 8. 用戶設定
- [ ] 更改顯示名稱
- [ ] 更改密碼
- [ ] 忘記密碼（Email 重設連結）
- [ ] 上傳大頭貼（GCS 儲存）
- [ ] 多語言支援（i18n，至少中 / 英）

### 9. 品牌 / 外觀
- [ ] 換網頁 favicon（`public/favicon.ico`）
- [ ] PWA manifest icon

### 10. 監控
- [ ] GCP Billing 預算警報（建議 $20/月上限）
- [ ] Spring Boot Actuator + Cloud Run health check

### 11. 前端效能
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
